import { saveHandle, loadHandle, setSetting, getSetting } from './db.js';

export const IMAGE_EXTENSIONS = new Set([
  'jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'heic', 'heif', 'avif', 'tif', 'tiff',
]);

export const capabilities = {
  fsAccess: typeof window !== 'undefined' && 'showDirectoryPicker' in window,
};

let sourceRootHandle = null; // where the original photos live — read-only, never written to
let miniaturesRootHandle = null; // a separate location (ideally the device/storage root) for all generated data
let miniaturesBaseHandle = null; // <miniaturesRoot>/photos/miniature/<sourceFolderName> — where this library's event folders live
let sourceFolderName = '';
let storageMode = null; // 'fsa' | 'opfs'

function extOf(name) {
  const i = name.lastIndexOf('.');
  return i === -1 ? '' : name.slice(i + 1).toLowerCase();
}

export function isImageFile(name) {
  return IMAGE_EXTENSIONS.has(extOf(name));
}

async function getOrCreateDir(dirHandle, ...parts) {
  let cur = dirHandle;
  for (const part of parts) {
    cur = await cur.getDirectoryHandle(part, { create: true });
  }
  return cur;
}

async function verifyPermission(handle, mode = 'readwrite') {
  const opts = { mode };
  if ((await handle.queryPermission(opts)) === 'granted') return true;
  if ((await handle.requestPermission(opts)) === 'granted') return true;
  return false;
}

async function computeMiniaturesBase() {
  miniaturesBaseHandle = await getOrCreateDir(miniaturesRootHandle, 'photos', 'miniature', sanitizeFolderName(sourceFolderName));
}

/** Try to restore a previously granted library from IndexedDB. Returns true if usable. */
export async function restoreLibrary() {
  storageMode = await getSetting('storageMode', null);
  if (storageMode === 'fsa') {
    const source = await loadHandle('sourceRoot');
    const miniaturesRoot = await loadHandle('miniaturesRoot');
    if (!source || !miniaturesRoot) return false;
    if (!(await verifyPermission(source, 'read'))) return false;
    if (!(await verifyPermission(miniaturesRoot, 'readwrite'))) return false;
    sourceRootHandle = source;
    miniaturesRootHandle = miniaturesRoot;
    sourceFolderName = source.name;
    await computeMiniaturesBase();
    return true;
  }
  if (storageMode === 'opfs') {
    const opfsRoot = await navigator.storage.getDirectory();
    sourceRootHandle = opfsRoot;
    miniaturesRootHandle = opfsRoot;
    sourceFolderName = 'import';
    await computeMiniaturesBase();
    return true;
  }
  return false;
}

/**
 * Step 1: user picks the folder containing the original photos. Requested
 * read-only — the app never writes into it, only reads files from it.
 */
export async function pickSourceFolder() {
  if (!capabilities.fsAccess) throw new Error('File System Access API non disponible sur ce navigateur.');
  const handle = await window.showDirectoryPicker({ id: 'photo-source', mode: 'read' });
  if (!(await verifyPermission(handle, 'read'))) throw new Error('Permission refusée pour ce dossier.');
  sourceRootHandle = handle;
  sourceFolderName = handle.name;
  storageMode = 'fsa';
  await saveHandle('sourceRoot', handle);
  await setSetting('storageMode', 'fsa');
  return handle;
}

/**
 * Step 2: user picks where generated data (miniatures + tag sidecars) should
 * live — independent from the source folder. Pick the device/storage root
 * (or any folder outside the source) so it never mixes with the originals;
 * the app then creates photos/miniature/<nom-du-dossier-source>/<événement>/ inside it.
 */
export async function pickMiniaturesRoot() {
  if (!capabilities.fsAccess) throw new Error('File System Access API non disponible sur ce navigateur.');
  const handle = await window.showDirectoryPicker({ id: 'photo-miniatures-root', mode: 'readwrite' });
  if (!(await verifyPermission(handle, 'readwrite'))) throw new Error('Permission refusée pour ce dossier.');
  miniaturesRootHandle = handle;
  await saveHandle('miniaturesRoot', handle);
  await computeMiniaturesBase();
  return handle;
}

export function hasSource() {
  return !!sourceRootHandle;
}

export function hasMiniaturesRoot() {
  return !!miniaturesRootHandle;
}

export function isLibraryReady() {
  return !!sourceRootHandle && !!miniaturesBaseHandle;
}

/**
 * Fallback for browsers without the File System Access API (e.g. iOS Safari, Firefox):
 * import files selected via <input type="file" webkitdirectory> into a private
 * app-managed storage area (OPFS). Photos are copied once, under photos/originaux/;
 * miniatures live under the separate photos/miniature/ branch of the same OPFS root —
 * there is no real "device root" to pick from in this mode.
 */
export async function importFilesFallback(fileList, onProgress) {
  const opfsRoot = await navigator.storage.getDirectory();
  sourceRootHandle = opfsRoot;
  miniaturesRootHandle = opfsRoot;
  sourceFolderName = 'import';
  storageMode = 'opfs';
  await setSetting('storageMode', 'opfs');
  await computeMiniaturesBase();

  const originauxHandle = await getOrCreateDir(opfsRoot, 'photos', 'originaux');
  const files = Array.from(fileList).filter((f) => isImageFile(f.name));
  const imported = [];
  let i = 0;
  for (const file of files) {
    i += 1;
    const safeName = `${Date.now()}_${i}_${file.name.replace(/[^\w.\-]/g, '_')}`;
    const fh = await originauxHandle.getFileHandle(safeName, { create: true });
    const writable = await fh.createWritable();
    await writable.write(file);
    await writable.close();
    imported.push({ handle: fh, name: safeName, originalName: file.name, relativePath: file.webkitRelativePath || file.name });
    if (onProgress) onProgress(i, files.length);
  }
  return imported;
}

export function getStorageMode() {
  return storageMode;
}

export function getRootHandle() {
  return sourceRootHandle;
}

export function getSourceFolderName() {
  return sourceFolderName;
}

/** Recursively walk a directory handle, yielding image file entries. Skips our generated photos/miniature tree (OPFS mode). */
export async function* walkImages(dirHandle, relPath = '', depth = 0) {
  if (depth > 12) return;
  for await (const [name, handle] of dirHandle.entries()) {
    const childRel = relPath ? `${relPath}/${name}` : name;
    if (childRel === 'photos/miniature') continue; // never re-ingest our own thumbnails (only relevant in OPFS mode)
    if (handle.kind === 'directory') {
      yield* walkImages(handle, childRel, depth + 1);
    } else if (handle.kind === 'file' && isImageFile(name)) {
      yield { handle, name, relativePath: childRel };
    }
  }
}

export async function readFile(fileHandle) {
  return fileHandle.getFile();
}

export async function getEventDir(eventFolderName) {
  if (!miniaturesBaseHandle) throw new Error("Emplacement des miniatures non défini.");
  return getOrCreateDir(miniaturesBaseHandle, sanitizeFolderName(eventFolderName));
}

export async function writeThumbnail(eventFolderName, fileName, blob) {
  const eventDir = await getEventDir(eventFolderName);
  const fh = await eventDir.getFileHandle(fileName, { create: true });
  const writable = await fh.createWritable();
  await writable.write(blob);
  await writable.close();
  return fh;
}

/**
 * Sidecar JSON written next to each thumbnail: tags, the path to the original,
 * and every other piece of metadata — never the original photo itself, which
 * is only ever read, never written. This is the durable, portable record of
 * a photo's tags: it survives a cleared browser profile, unlike IndexedDB.
 */
export async function writeSidecar(eventFolderName, fileName, data) {
  const eventDir = await getEventDir(eventFolderName);
  const fh = await eventDir.getFileHandle(fileName, { create: true });
  const writable = await fh.createWritable();
  await writable.write(JSON.stringify(data));
  await writable.close();
  return fh;
}

/** Recursively walk this library's miniature tree, yielding {handle, name} for every sidecar .json file. */
export async function* walkSidecars(dirHandle = miniaturesBaseHandle, depth = 0) {
  if (!dirHandle || depth > 8) return;
  for await (const [name, handle] of dirHandle.entries()) {
    if (handle.kind === 'directory') {
      yield* walkSidecars(handle, depth + 1);
    } else if (handle.kind === 'file' && name.endsWith('.json')) {
      yield { handle, name, dirHandle };
    }
  }
}

/** Load every sidecar under this library's miniature tree, keyed by the original photo's relativePath. */
export async function loadAllSidecars() {
  const byPath = new Map();
  for await (const { handle } of walkSidecars()) {
    try {
      const file = await handle.getFile();
      const data = JSON.parse(await file.text());
      if (data.relativePath) byPath.set(data.relativePath, data);
    } catch {
      // skip unreadable/corrupt sidecar
    }
  }
  return byPath;
}

export function sanitizeFolderName(name) {
  return name.replace(/[\\/:*?"<>|]/g, '-').trim() || 'sans-nom';
}
