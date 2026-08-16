import { parse } from 'exifr';

/** Extract the fields we care about from a photo File. Never throws: returns {} on failure. */
export async function extractExif(file) {
  try {
    const data = await parse(file, { gps: true, tiff: true, exif: true, ifd0: true });
    if (!data) return {};
    const takenAt = data.DateTimeOriginal || data.CreateDate || data.ModifyDate || null;
    return {
      takenAt: takenAt ? new Date(takenAt).toISOString() : null,
      lat: typeof data.latitude === 'number' ? data.latitude : null,
      lon: typeof data.longitude === 'number' ? data.longitude : null,
      camera: [data.Make, data.Model].filter(Boolean).join(' ').trim() || null,
    };
  } catch {
    return {};
  }
}
