// Préparation des photos avant envoi : rotation selon l'appareil, redimensionnement, compression.
// Grande version 2560 px (plein écran sur ordinateur) + petite version 800 px (vignettes, mobile).

async function decode(file) {
  if ('createImageBitmap' in window) {
    try { return await createImageBitmap(file, { imageOrientation: 'from-image' }); } catch { /* repli ci-dessous */ }
  }
  const url = URL.createObjectURL(file);
  try {
    const img = new Image();
    img.src = url;
    await img.decode();
    return img;
  } finally {
    URL.revokeObjectURL(url);
  }
}

function encode(source, maxW, quality) {
  const w0 = source.width;
  const h0 = source.height;
  const scale = Math.min(1, maxW / w0);
  const w = Math.round(w0 * scale);
  const h = Math.round(h0 * scale);
  const canvas = document.createElement('canvas');
  canvas.width = w;
  canvas.height = h;
  const ctx = canvas.getContext('2d');
  ctx.imageSmoothingQuality = 'high';
  ctx.drawImage(source, 0, 0, w, h);
  const toBlob = (type, q) => new Promise((ok) => canvas.toBlob(ok, type, q));
  return (async () => {
    // WebP si le navigateur sait l'encoder, sinon JPEG (anciens Safari)
    let blob = await toBlob('image/webp', quality);
    if (!blob || blob.type !== 'image/webp') blob = await toBlob('image/jpeg', quality + 0.03);
    return { blob, w, h, ext: blob.type === 'image/webp' ? 'webp' : 'jpg' };
  })();
}

export async function prepareImage(file, { maxW = 2560, smallW = 800, quality = 0.82 } = {}) {
  if (!file.type.startsWith('image/')) throw new Error(`« ${file.name} » n’est pas une image.`);
  let src;
  try { src = await decode(file); } catch { throw new Error(`Impossible de lire « ${file.name} ». Essayez une photo JPEG ou PNG.`); }
  const big = await encode(src, maxW, quality);
  const small = await encode(src, smallW, quality);
  if (src.close) src.close();
  return { big, small, width: big.w, height: big.h, tooSmall: big.w < 1920 };
}
