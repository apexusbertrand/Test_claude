import { putPhoto, putEvent } from './db.js';
import {
  getEventDir,
  writeFileInDir,
  writeJsonInDir,
  sanitizeFolderName,
  getMiniatureTreeDir,
  walkSidecars,
} from './storage.js';
import { extractExif } from './exif.js';
import { reverseGeocode } from './geocode.js';
import { detectTags, ensureReady as ensureObjectDetectionReady } from './detect.js';
import { detectFaces, ensureReady as ensureFaceDetectionReady, propagateNames } from './faces.js';
import { clusterEvents } from './events.js';
import { decodeImage, drawResizedCanvas, canvasToBlob } from './thumbnail.js';
import { reportAiStatus, withTimeout } from './ai-status.js';

const THUMB_SIZE = 400;
const FACE_DETECT_SIZE = 768; // faces are often small in the frame; detect at a higher resolution than the thumbnail
// Outer safety net around one photo's whole decode+write+AI step. The AI
// stages have their own, shorter budgets (see detect.js / faces.js); this one
// only has to catch a decode that hangs — createImageBitmap and the <img>
// fallback can both stall forever on certain files, neither resolving nor
// rejecting, which no try/catch can catch.
const GENERATE_TIMEOUT_MS = 45000;

function uuid() {
  return crypto.randomUUID();
}

function baseName(name) {
  const i = name.lastIndexOf('.');
  return i === -1 ? name : name.slice(0, i);
}

/** Strip browser-only handles/File objects before writing to a portable JSON sidecar. */
function toSidecarData(photo) {
  const { fileHandle, thumbHandle, eventDirHandle, ...rest } = photo;
  return rest;
}

/**
 * Save a photo's DB row and its JSON sidecar. Writes the sidecar into the
 * photo's own event folder (photo.eventDirHandle) when we have it cached —
 * this matters once photos from several different source folders can be
 * loaded into the gallery at once (see loadBrowseOnlyPhotos), since the
 * globally "current" source folder is no longer necessarily the right one.
 */
export async function persistPhoto(photo) {
  await putPhoto(photo);
  const dirHandle = photo.eventDirHandle || (await getEventDir(photo.eventFolder));
  await writeJsonInDir(dirHandle, photo.thumbFileName.replace(/\.jpg$/i, '.json'), toSidecarData(photo));
}

/**
 * Decode a photo file and produce its thumbnail (written to disk) + local AI
 * tags + face descriptors.
 *
 * Ordering matters: the thumbnail is decoded, written and *committed* before
 * any AI runs, and the AI stage is wrapped so it can never take the thumbnail
 * down with it. The AI models (object detection weights come from an external
 * CDN; face models go through WebGL) are the part most likely to stall or die
 * on a phone — when that happens the photo still gets its thumbnail and its
 * date/place tags, just without the AI-derived ones.
 */
async function generateThumbnailAndTags(file, eventDirHandle, thumbFileName) {
  const source = await decodeImage(file);
  try {
    const { canvas: thumbCanvas, width, height } = drawResizedCanvas(source, THUMB_SIZE);
    const blob = await canvasToBlob(thumbCanvas);
    const thumbHandle = await writeFileInDir(eventDirHandle, thumbFileName, blob);

    let aiTags = [];
    let faces = [];
    try {
      aiTags = await detectTags(thumbCanvas);
      const { canvas: faceCanvas } = drawResizedCanvas(source, FACE_DETECT_SIZE);
      faces = await detectFaces(faceCanvas);
    } catch (err) {
      console.warn('Tags IA indisponibles pour', thumbFileName, err);
    }

    return { thumbHandle, width, height, aiTags, faces };
  } finally {
    if (source.close) source.close();
  }
}

/**
 * Full pipeline for a batch of newly found image entries: read EXIF, cluster
 * into events, generate + store a thumbnail, run local AI tagging (objects/
 * animals + face descriptors), and reverse-geocode GPS into a place tag.
 * The original file is only ever read via entry.handle.getFile() — never
 * written to. Persists a DB row *and* a JSON sidecar next to the thumbnail
 * for every photo. `entries` items: { handle: FileSystemFileHandle, name, relativePath }.
 */
export async function processBatch(entries, { onProgress } = {}) {
  // Kick both AI model loads off now, in parallel, and let them run behind the
  // EXIF pass below — but the photo loop must not start until we know for
  // sure whether each is usable, otherwise every single photo would pay its
  // own model-load timeout (up to 25s × 2) instead of paying it once, here.
  const aiReadyPromise = Promise.all([ensureObjectDetectionReady(), ensureFaceDetectionReady()]);
  const total = entries.length;
  const drafts = [];

  // Pass 1: read files + EXIF (fast, needed before we can cluster events).
  for (let i = 0; i < entries.length; i += 1) {
    const entry = entries[i];
    const file = await entry.handle.getFile();
    const exif = await extractExif(file);
    drafts.push({
      id: uuid(),
      name: entry.name,
      relativePath: entry.relativePath,
      fileHandle: entry.handle,
      file,
      takenAt: exif.takenAt || (file.lastModified ? new Date(file.lastModified).toISOString() : null),
      lat: exif.lat ?? null,
      lon: exif.lon ?? null,
      camera: exif.camera || null,
    });
    if (onProgress) onProgress({ phase: 'exif', current: i + 1, total });
  }

  const { events, assignments } = clusterEvents(drafts);
  for (const e of events) {
    await putEvent({ id: e.id, label: e.label, start: e.start, end: e.lastTime, photoCount: e.photoIds.length });
  }

  // Resolved by now in the common case (it's been running since the top of
  // this function, in parallel with the EXIF pass above); each photo below
  // will see isObjectDetectionDisabled()/isFaceDetectionDisabled() already
  // settled, so a model that failed to load costs nothing per photo. Any
  // failure here already reported its own clear message via disable().
  if (onProgress) onProgress({ phase: 'ai-check', current: 0, total: 1 });
  await aiReadyPromise;

  // Pass 2: thumbnail + AI tagging + face descriptors + geocoding (slower, per photo).
  // Each photo is fully isolated in its own try/catch: one photo's failure
  // (a folder the browser refuses to create, a corrupt file, …) must never
  // abort the rest of the batch — it shows up as a repairable placeholder
  // instead of silently stopping the whole scan partway through.
  const saved = [];
  for (let i = 0; i < drafts.length; i += 1) {
    const d = drafts[i];
    try {
      const eventId = assignments.get(d.id);
      const event = events.find((e) => e.id === eventId);
      const eventFolder = sanitizeFolderName(event ? event.label : 'Sans date');

      const tags = [{ category: 'evenement', value: event ? event.label : 'Sans date' }];

      let width = null;
      let height = null;
      let faces = [];
      let eventDirHandle = null;
      let thumbHandle = null;
      const thumbFileName = `${baseName(d.name)}-${d.id.slice(0, 8)}.jpg`;
      try {
        eventDirHandle = await getEventDir(eventFolder);
        const result = await withTimeout(
          generateThumbnailAndTags(d.file, eventDirHandle, thumbFileName),
          GENERATE_TIMEOUT_MS,
          d.name
        );
        thumbHandle = result.thumbHandle;
        width = result.width;
        height = result.height;
        faces = result.faces;
        tags.push(...result.aiTags);
      } catch (err) {
        console.warn('Échec miniature/IA pour', d.name, '— sera retenté au prochain scan.', err);
        reportAiStatus(`Miniature impossible pour "${d.name}" (${err.message || err}) — sera retentée au prochain scan.`);
      }

      if (d.lat != null && d.lon != null) {
        const geo = await reverseGeocode(d.lat, d.lon);
        if (geo?.city) tags.push({ category: 'lieu', value: geo.city });
        if (geo?.country) tags.push({ category: 'lieu', value: geo.country });
      }

      const photo = {
        id: d.id,
        name: d.name,
        relativePath: d.relativePath,
        fileHandle: d.fileHandle,
        takenAt: d.takenAt,
        lat: d.lat,
        lon: d.lon,
        camera: d.camera,
        eventId,
        eventLabel: event ? event.label : 'Sans date',
        eventFolder,
        eventDirHandle,
        thumbFileName,
        thumbHandle,
        width,
        height,
        tags,
        faces,
        importedAt: new Date().toISOString(),
      };
      if (eventDirHandle) {
        await persistPhoto(photo);
      } else {
        // Couldn't even create/open the event folder for this photo — cache it
        // in the DB anyway (shows up as a repairable placeholder) but skip the
        // sidecar write, since that would fail identically.
        await putPhoto(photo);
      }
      saved.push(photo);
    } catch (err) {
      console.error('Échec du traitement pour', d.name, err);
      reportAiStatus(`Échec du traitement de "${d.name}" : ${err.message || err}`);
    }

    if (onProgress) onProgress({ phase: 'tagging', current: i + 1, total });
  }

  return saved;
}

/**
 * Re-attach a live file handle to a photo we've already tagged before (matched by
 * relativePath against a sidecar found on disk), without re-running EXIF/AI/geocoding.
 * This is what makes tags survive a cleared IndexedDB: the sidecar is the source of
 * truth, the DB is just a fast cache rebuilt from it.
 */
export async function rehydrateFromSidecar(entry, sidecar) {
  const eventDir = await getEventDir(sidecar.eventFolder);
  let thumbHandle = null;
  try {
    thumbHandle = await withTimeout(eventDir.getFileHandle(sidecar.thumbFileName), 8000, sidecar.thumbFileName);
  } catch (err) {
    thumbHandle = null; // thumbnail file was moved/deleted, or unreadable — a repair is retried below
    reportAiStatus(`Miniature introuvable pour "${sidecar.name}" (${err.message || err}) — nouvelle tentative au prochain scan.`);
  }
  const photo = { ...sidecar, fileHandle: entry.handle, thumbHandle, eventDirHandle: eventDir };
  await putPhoto(photo);
  return photo;
}

/**
 * Retry thumbnail + AI tag generation for a photo whose miniature failed last
 * time (missing on disk) — without touching its existing tags (manual ones
 * are kept as-is; new AI tags are merged in, never duplicated). This is what
 * turns a permanently-broken thumbnail into something the next "Analyser"
 * click actually fixes, instead of it staying broken forever.
 */
export async function repairThumbnail(entry, sidecar) {
  const eventDirHandle = await getEventDir(sidecar.eventFolder);
  const thumbFileName = sidecar.thumbFileName || `${baseName(sidecar.name)}-${sidecar.id.slice(0, 8)}.jpg`;
  const file = await entry.handle.getFile();
  const { thumbHandle, width, height, aiTags, faces } = await withTimeout(
    generateThumbnailAndTags(file, eventDirHandle, thumbFileName),
    GENERATE_TIMEOUT_MS,
    sidecar.name
  );

  const existingKeys = new Set((sidecar.tags || []).map((t) => `${t.category}:${t.value}`));
  const tags = [...(sidecar.tags || [])];
  for (const t of aiTags) {
    const key = `${t.category}:${t.value}`;
    if (!existingKeys.has(key)) {
      tags.push(t);
      existingKeys.add(key);
    }
  }

  const photo = { ...sidecar, fileHandle: entry.handle, eventDirHandle, thumbHandle, thumbFileName, width, height, faces, tags };
  await persistPhoto(photo);
  return photo;
}

/**
 * Browse every photo already tagged in the miniatures location, across every
 * source folder ever scanned into it — no source folder access required.
 * Lets the gallery show all existing miniatures/tags on page load and support
 * tag editing + sharing the miniature immediately; opening/sharing the
 * original still needs the matching source folder to be (re-)granted.
 */
export async function loadBrowseOnlyPhotos() {
  const miniatureTreeDir = await getMiniatureTreeDir();
  const photos = [];
  for await (const { handle, dirHandle } of walkSidecars(miniatureTreeDir)) {
    try {
      const file = await handle.getFile();
      const sidecar = JSON.parse(await file.text());
      let thumbHandle = null;
      try {
        thumbHandle = await withTimeout(dirHandle.getFileHandle(sidecar.thumbFileName), 8000, sidecar.thumbFileName);
      } catch (err) {
        thumbHandle = null;
        reportAiStatus(`Miniature introuvable pour "${sidecar.name}" (${err.message || err}).`);
      }
      const photo = { ...sidecar, fileHandle: null, thumbHandle, eventDirHandle: dirHandle };
      await putPhoto(photo);
      photos.push(photo);
    } catch {
      // skip unreadable/corrupt sidecar
    }
  }
  return photos;
}

/**
 * Propagate manually-entered names to every other photo whose detected face
 * matches closely enough. Persists (DB + sidecar) any photo that gained a tag.
 */
export async function propagateAndPersist(allPhotos) {
  const changed = propagateNames(allPhotos);
  for (const photo of changed) {
    await persistPhoto(photo);
  }
  return changed;
}
