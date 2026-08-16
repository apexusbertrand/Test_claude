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

/** Safe to call on page load, with no user gesture: only ever *queries* the current permission state. */
async function hasPermission(handle, mode) {
  return (await handle.queryPermission({ mode })) === 'granted';
}

/**
 * Requests permission — the browser requires this to run inside a user gesture
 * (a click), otherwise it throws instead of prompting. Never call this from
 * code that runs automatically on page load; only from a click handler.
 */
async function requestPermission(handle, mode) {
  return (await handle.requestPermission({ mode })) === 'granted';
}

async function computeMiniaturesBase() {
  miniaturesBaseHandle = await getOrCreateDir(miniaturesRootHandle, 'photos', 'miniature', sanitizeFolderName(sourceFolderName));
}

async function loadSavedHandles() {
  return {
    source: await loadHandle('sourceRoot'),
    miniaturesRoot: await loadHandle('miniaturesRoot'),
  };
}

/**
 * Silent restore attempt, safe to run on every page load with no user gesture:
 * only succeeds for whatever the browser already durably granted permission
 * for (queryPermission only — never prompts). Source and miniatures root are
 * restored independently of one another, since the browser may have kept the
 * permission for one but not the other.
 */
export async function restoreLibrarySilently() {
  storageMode = await getSetting('storageMode', null);
  if (storageMode === 'opfs') {
    const opfsRoot = await navigator.storage.getDirectory();
    sourceRootHandle = opfsRoot;
    miniaturesRootHandle = opfsRoot;
    sourceFolderName = 'import';
    await computeMiniaturesBase();
    return { ready: true, hasSavedHandles: true };
  }
  if (storageMode !== 'fsa') return { ready: false, hasSavedHandles: false };

  const { source, miniaturesRoot } = await loadSavedHandles();
  if (!source && !miniaturesRoot) return { ready: false, hasSavedHandles: false };

  if (miniaturesRoot && (await hasPermission(miniaturesRoot, 'readwrite'))) {
    miniaturesRootHandle = miniaturesRoot;
  }
  if (source && (await hasPermission(source, 'read'))) {
    sourceRootHandle = source;
    sourceFolderName = source.name;
  }
  if (sourceRootHandle && miniaturesRootHandle) {
    await computeMiniaturesBase();
    return { ready: true, hasSavedHandles: true };
  }
  return { ready: false, hasSavedHandles: true, miniaturesOnly: !!miniaturesRootHandle };
}

/** Whether a previous session saved folder handles we could try to reconnect to. */
export async function hasSavedFsaHandles() {
  if ((await getSetting('storageMode', null)) !== 'fsa') return false;
  const { source, miniaturesRoot } = await loadSavedHandles();
  return !!source && !!miniaturesRoot;
}

/**
 * Re-request permission on the already-saved handles — no folder picker dialog,
 * just the browser's native "allow access again?" prompt. Must be called from a
 * click handler (user gesture) or the browser rejects it outright. On success,
 * this reconnects to the *same* folders as before, so tags/thumbnails already
 * on disk are recognized (rehydrated) instead of being re-analyzed from scratch.
 */
export async function reconnectSavedHandles() {
  const { source, miniaturesRoot } = await loadSavedHandles();
  if (!source || !miniaturesRoot) return false;
  const sourceGranted = await requestPermission(source, 'read');
  const miniGranted = await requestPermission(miniaturesRoot, 'readwrite');
  if (sourceGranted) {
    sourceRootHandle = source;
    sourceFolderName = source.name;
  }
  if (miniGranted) {
    miniaturesRootHandle = miniaturesRoot;
  }
  if (!sourceGranted || !miniGranted) return false;
  storageMode = 'fsa';
  await computeMiniaturesBase();
  return true;
}

/** The photos/miniature directory at the root of the miniatures location — one subfolder per source folder ever scanned. */
export async function getMiniatureTreeDir() {
  if (!miniaturesRootHandle) throw new Error('Emplacement des miniatures non défini.');
  return getOrCreateDir(miniaturesRootHandle, 'photos', 'miniature');
}

/**
 * Step 1: user picks the folder containing the original photos. Requested
 * read-only — the app never writes into it, only reads files from it.
 */
export async function pickSourceFolder() {
  if (!capabilities.fsAccess) throw new Error('File System Access API non disponible sur ce navigateur.');
  const handle = await window.showDirectoryPicker({ id: 'photo-source', mode: 'read' });
  if (!(await requestPermission(handle, 'read'))) throw new Error('Permission refusée pour ce dossier.');
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
  if (!(await requestPermission(handle, 'readwrite'))) throw new Error('Permission refusée pour ce dossier.');
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

export function getMiniaturesRootHandle() {
  return miniaturesRootHandle;
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

export async function writeFileInDir(dirHandle, fileName, blob) {
  const fh = await dirHandle.getFileHandle(fileName, { create: true });
  const writable = await fh.createWritable();
  await writable.write(blob);
  await writable.close();
  return fh;
}

export async function writeJsonInDir(dirHandle, fileName, data) {
  return writeFileInDir(dirHandle, fileName, JSON.stringify(data));
}

export async function writeThumbnail(eventFolderName, fileName, blob) {
  const eventDir = await getEventDir(eventFolderName);
  return writeFileInDir(eventDir, fileName, blob);
}

/**
 * Sidecar JSON written next to each thumbnail: tags, the path to the original,
 * and every other piece of metadata — never the original photo itself, which
 * is only ever read, never written. This is the durable, portable record of
 * a photo's tags: it survives a cleared browser profile, unlike IndexedDB.
 */
export async function writeSidecar(eventFolderName, fileName, data) {
  const eventDir = await getEventDir(eventFolderName);
  return writeJsonInDir(eventDir, fileName, data);
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
