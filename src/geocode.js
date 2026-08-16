const cache = new Map();
let queue = Promise.resolve();
const MIN_INTERVAL_MS = 1100; // be polite to the free Nominatim endpoint (max ~1 req/s)
let lastCallAt = 0;

function key(lat, lon) {
  // Round to ~1km so nearby photos share a cache entry / a single request.
  return `${lat.toFixed(2)},${lon.toFixed(2)}`;
}

async function throttledFetch(url) {
  queue = queue.then(async () => {
    const wait = Math.max(0, MIN_INTERVAL_MS - (Date.now() - lastCallAt));
    if (wait > 0) await new Promise((r) => setTimeout(r, wait));
    lastCallAt = Date.now();
    return fetch(url, { headers: { Accept: 'application/json' } });
  });
  return queue;
}

/** Reverse-geocode GPS coordinates into {city, country} tags. Returns null offline or on failure. */
export async function reverseGeocode(lat, lon) {
  if (lat == null || lon == null) return null;
  const k = key(lat, lon);
  if (cache.has(k)) return cache.get(k);
  if (!navigator.onLine) return null;
  try {
    const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lon}&zoom=10&accept-language=fr`;
    const res = await throttledFetch(url);
    if (!res.ok) return null;
    const data = await res.json();
    const addr = data.address || {};
    const city = addr.city || addr.town || addr.village || addr.municipality || addr.county || null;
    const country = addr.country || null;
    const result = { city, country };
    cache.set(k, result);
    return result;
  } catch {
    return null;
  }
}
