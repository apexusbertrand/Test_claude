import { putPhoto, putEvent } from './db.js';
import { writeThumbnail, sanitizeFolderName } from './storage.js';
import { extractExif } from './exif.js';
import { reverseGeocode } from './geocode.js';
import { detectTags, preloadModel } from './detect.js';
import { clusterEvents } from './events.js';
import { decodeImage, drawThumbnailCanvas, canvasToBlob } from './thumbnail.js';

function uuid() {
  return crypto.randomUUID();
}

function baseName(name) {
  const i = name.lastIndexOf('.');
  return i === -1 ? name : name.slice(0, i);
}

/**
 * Full pipeline for a batch of newly found image entries: read EXIF, cluster
 * into events, generate + store a thumbnail, run local AI tagging, and
 * reverse-geocode GPS into a place tag. Persists everything to IndexedDB.
 * `entries` items: { handle: FileSystemFileHandle, name, relativePath }.
 */
export async function processBatch(entries, { onProgress } = {}) {
  preloadModel();
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

  // Pass 2: thumbnail + AI tagging + geocoding (slower, per photo).
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
    try {
      const source = await decodeImage(d.file);
      const { canvas, width: w, height: h } = drawThumbnailCanvas(source);
      width = w;
      height = h;
      const blob = await canvasToBlob(canvas);
      thumbFileName = `${baseName(d.name)}-${d.id.slice(0, 8)}.jpg`;
      thumbHandle = await writeThumbnail(eventFolder, thumbFileName, blob);

      const aiTags = await detectTags(canvas);
      tags.push(...aiTags);
      if (source.close) source.close();
    } catch (err) {
      console.warn('Échec miniature/IA pour', d.name, err);
    }

    if (d.lat != null && d.lon != null) {
      const geo = await reverseGeocode(d.lat, d.lon);
      if (geo?.city) tags.push({ category: 'lieu', value: geo.city });
      if (geo?.country) tags.push({ category: 'lieu', value: geo.country });
    }

    await putPhoto({
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
      importedAt: new Date().toISOString(),
    });

    if (onProgress) onProgress({ phase: 'tagging', current: i + 1, total });
  }

  return drafts.length;
}
