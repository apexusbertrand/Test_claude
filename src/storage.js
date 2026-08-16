import { saveHandle, loadHandle, setSetting, getSetting } from './db.js';

export const IMAGE_EXTENSIONS = new Set([
  'jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'heic', 'heif', 'avif', 'tif', 'tiff',
]);

export const capabilities = {
  fsAccess: typeof window !== 'undefined' && 'showDirectoryPicker' in window,
};

let rootHandle = null;
let miniaturesHandle = null;
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

/** Try to restore a previously granted folder from IndexedDB. Returns true if usable. */
export async function restoreLibrary() {
  storageMode = await getSetting('storageMode', null);
  if (storageMode === 'fsa') {
    const handle = await loadHandle('root');
    if (!handle) return false;
    const ok = await verifyPermission(handle, 'readwrite');
    if (!ok) return false;
    rootHandle = handle;
    miniaturesHandle = await getOrCreateDir(rootHandle, 'photos', 'miniatures');
    return true;
  }
  if (storageMode === 'opfs') {
    rootHandle = await navigator.storage.getDirectory();
    miniaturesHandle = await getOrCreateDir(rootHandle, 'photos', 'miniatures');
    return true;
  }
  return false;
}

/** User-initiated: open the native folder picker (Chrome/Edge desktop + Android). */
export async function pickLibraryFolder() {
  if (!capabilities.fsAccess) throw new Error('File System Access API non disponible sur ce navigateur.');
  const handle = await window.showDirectoryPicker({ id: 'photo-library', mode: 'readwrite' });
  const ok = await verifyPermission(handle, 'readwrite');
  if (!ok) throw new Error('Permission refusée pour ce dossier.');
  rootHandle = handle;
  miniaturesHandle = await getOrCreateDir(rootHandle, 'photos', 'miniatures');
  storageMode = 'fsa';
  await saveHandle('root', handle);
  await setSetting('storageMode', 'fsa');
  return rootHandle;
}

/**
 * Fallback for browsers without the File System Access API (e.g. iOS Safari, Firefox):
 * import files selected via <input type="file" webkitdirectory> into a private
 * app-managed storage area (OPFS). Photos are copied once; thumbnails and tags
 * then work exactly like the "real folder" mode.
 */
export async function importFilesFallback(fileList, onProgress) {
  rootHandle = await navigator.storage.getDirectory();
  const originauxHandle = await getOrCreateDir(rootHandle, 'photos', 'originaux');
  miniaturesHandle = await getOrCreateDir(rootHandle, 'photos', 'miniatures');
  storageMode = 'opfs';
  await setSetting('storageMode', 'opfs');

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
  return rootHandle;
}

export function getMiniaturesHandle() {
  return miniaturesHandle;
}

/** Recursively walk a directory handle, yielding image file entries. Skips our generated photos/miniatures tree. */
export async function* walkImages(dirHandle, relPath = '', depth = 0) {
  if (depth > 12) return;
  for await (const [name, handle] of dirHandle.entries()) {
    const childRel = relPath ? `${relPath}/${name}` : name;
    if (childRel === 'photos/miniatures') continue; // never re-ingest our own thumbnails
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
  if (!miniaturesHandle) throw new Error('Bibliothèque non initialisée.');
  return getOrCreateDir(miniaturesHandle, sanitizeFolderName(eventFolderName));
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

/** Recursively walk the miniatures tree, yielding {handle, name, dirHandle} for every sidecar .json file. */
export async function* walkSidecars(dirHandle = miniaturesHandle, depth = 0) {
  if (!dirHandle || depth > 8) return;
  for await (const [name, handle] of dirHandle.entries()) {
    if (handle.kind === 'directory') {
      yield* walkSidecars(handle, depth + 1);
    } else if (handle.kind === 'file' && name.endsWith('.json')) {
      yield { handle, name, dirHandle };
    }
  }
}

/** Load every sidecar under photos/miniatures, keyed by the original photo's relativePath. */
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
  return name.replace(/[\\/:*?"<>|]/g, '-').trim() || 'sans-date';
}
