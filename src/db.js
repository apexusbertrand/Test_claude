const DB_NAME = 'phototag-db';
const DB_VERSION = 1;

let dbPromise = null;

function openDb() {
  if (dbPromise) return dbPromise;
  dbPromise = new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains('photos')) {
        const store = db.createObjectStore('photos', { keyPath: 'id' });
        store.createIndex('takenAt', 'takenAt');
        store.createIndex('eventId', 'eventId');
      }
      if (!db.objectStoreNames.contains('events')) {
        db.createObjectStore('events', { keyPath: 'id' });
      }
      if (!db.objectStoreNames.contains('handles')) {
        db.createObjectStore('handles', { keyPath: 'key' });
      }
      if (!db.objectStoreNames.contains('settings')) {
        db.createObjectStore('settings', { keyPath: 'key' });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
  return dbPromise;
}

async function tx(storeName, mode, fn) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const t = db.transaction(storeName, mode);
    const store = t.objectStore(storeName);
    const result = fn(store);
    t.oncomplete = () => resolve(result);
    t.onerror = () => reject(t.error);
    t.onabort = () => reject(t.error);
  });
}

export async function putPhoto(photo) {
  return tx('photos', 'readwrite', (store) => store.put(photo));
}

export async function getPhoto(id) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const req = db.transaction('photos', 'readonly').objectStore('photos').get(id);
    req.onsuccess = () => resolve(req.result || null);
    req.onerror = () => reject(req.error);
  });
}

export async function getAllPhotos() {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const req = db.transaction('photos', 'readonly').objectStore('photos').getAll();
    req.onsuccess = () => resolve(req.result || []);
    req.onerror = () => reject(req.error);
  });
}

export async function deletePhoto(id) {
  return tx('photos', 'readwrite', (store) => store.delete(id));
}

export async function putEvent(event) {
  return tx('events', 'readwrite', (store) => store.put(event));
}

export async function getAllEvents() {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const req = db.transaction('events', 'readonly').objectStore('events').getAll();
    req.onsuccess = () => resolve(req.result || []);
    req.onerror = () => reject(req.error);
  });
}

export async function saveHandle(key, handle) {
  return tx('handles', 'readwrite', (store) => store.put({ key, handle }));
}

export async function loadHandle(key) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const req = db.transaction('handles', 'readonly').objectStore('handles').get(key);
    req.onsuccess = () => resolve(req.result ? req.result.handle : null);
    req.onerror = () => reject(req.error);
  });
}

export async function setSetting(key, value) {
  return tx('settings', 'readwrite', (store) => store.put({ key, value }));
}

export async function getSetting(key, fallback = null) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const req = db.transaction('settings', 'readonly').objectStore('settings').get(key);
    req.onsuccess = () => resolve(req.result ? req.result.value : fallback);
    req.onerror = () => reject(req.error);
  });
}

export async function clearAll() {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const t = db.transaction(['photos', 'events', 'handles'], 'readwrite');
    t.objectStore('photos').clear();
    t.objectStore('events').clear();
    t.objectStore('handles').clear();
    t.oncomplete = () => resolve();
    t.onerror = () => reject(t.error);
  });
}
