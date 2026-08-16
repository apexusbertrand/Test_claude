import { putPhoto, putEvent } from './db.js';
import { writeThumbnail, writeSidecar, getEventDir, sanitizeFolderName } from './storage.js';
import { extractExif } from './exif.js';
import { reverseGeocode } from './geocode.js';
import { detectTags, preloadModel } from './detect.js';
import { detectFaces, preloadFaceModels, propagateNames } from './faces.js';
import { clusterEvents } from './events.js';
import { decodeImage, drawResizedCanvas, canvasToBlob } from './thumbnail.js';

const THUMB_SIZE = 400;
const FACE_DETECT_SIZE = 768; // faces are often small in the frame; detect at a higher resolution than the thumbnail

function uuid() {
  return crypto.randomUUID();
}

function baseName(name) {
  const i = name.lastIndexOf('.');
  return i === -1 ? name : name.slice(0, i);
}

/** Strip browser-only handles/File objects before writing to a portable JSON sidecar. */
function toSidecarData(photo) {
  const { fileHandle, thumbHandle, ...rest } = photo;
  return rest;
}

export async function persistPhoto(photo) {
  await putPhoto(photo);
  await writeSidecar(photo.eventFolder, photo.thumbFileName.replace(/\.jpg$/i, '.json'), toSidecarData(photo));
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
  preloadModel();
  preloadFaceModels();
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

  // Pass 2: thumbnail + AI tagging + face descriptors + geocoding (slower, per photo).
  const saved = [];
  for (let i = 0; i < drafts.length; i += 1) {
    const d = drafts[i];
    const eventId = assignments.get(d.id);
    const event = events.find((e) => e.id === eventId);
    const eventFolder = sanitizeFolderName(event ? event.label : 'Sans date');

    const tags = [{ category: 'evenement', value: event ? event.label : 'Sans date' }];

    let width = null;
    let height = null;
    let thumbHandle = null;
    let thumbFileName = null;
    let faces = [];
    try {
      const source = await decodeImage(d.file);
      const { canvas: thumbCanvas, width: w, height: h } = drawResizedCanvas(source, THUMB_SIZE);
      width = w;
      height = h;
      const blob = await canvasToBlob(thumbCanvas);
      thumbFileName = `${baseName(d.name)}-${d.id.slice(0, 8)}.jpg`;
      thumbHandle = await writeThumbnail(eventFolder, thumbFileName, blob);

      const aiTags = await detectTags(thumbCanvas);
      tags.push(...aiTags);

      const { canvas: faceCanvas } = drawResizedCanvas(source, FACE_DETECT_SIZE);
      faces = await detectFaces(faceCanvas);
      if (source.close) source.close();
    } catch (err) {
      console.warn('Échec miniature/IA pour', d.name, err);
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
      thumbFileName,
      thumbHandle,
      width,
      height,
      tags,
      faces,
      importedAt: new Date().toISOString(),
    };
    await persistPhoto(photo);
    saved.push(photo);

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
    thumbHandle = await eventDir.getFileHandle(sidecar.thumbFileName);
  } catch {
    thumbHandle = null; // thumbnail file was moved/deleted; tags are still recovered
  }
  const photo = { ...sidecar, fileHandle: entry.handle, thumbHandle };
  await putPhoto(photo);
  return photo;
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
