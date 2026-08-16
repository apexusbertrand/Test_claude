const MAX_SIZE = 400;
const QUALITY = 0.82;

/** Decode an image File into an ImageBitmap (fast path) with a <img> fallback for unsupported formats. */
export async function decodeImage(file) {
  if ('createImageBitmap' in window) {
    try {
      return await createImageBitmap(file);
    } catch {
      // fall through to <img> fallback (e.g. some HEIC files)
    }
  }
  const url = URL.createObjectURL(file);
  try {
    const img = new Image();
    img.decoding = 'async';
    const loaded = new Promise((resolve, reject) => {
      img.onload = () => resolve(img);
      img.onerror = reject;
    });
    img.src = url;
    return await loaded;
  } finally {
    URL.revokeObjectURL(url);
  }
}

function sourceDimensions(source) {
  return {
    width: source.width || source.naturalWidth,
    height: source.height || source.naturalHeight,
  };
}

/** Draw a decoded image onto a canvas sized to fit MAX_SIZE, keeping aspect ratio. */
export function drawThumbnailCanvas(source) {
  const { width, height } = sourceDimensions(source);
  const scale = Math.min(1, MAX_SIZE / Math.max(width, height));
  const w = Math.max(1, Math.round(width * scale));
  const h = Math.max(1, Math.round(height * scale));
  const canvas = document.createElement('canvas');
  canvas.width = w;
  canvas.height = h;
  const ctx = canvas.getContext('2d');
  ctx.drawImage(source, 0, 0, w, h);
  return { canvas, width, height };
}

export function canvasToBlob(canvas) {
  return new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', QUALITY));
}
