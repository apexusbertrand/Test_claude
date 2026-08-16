/** Share a photo's original (if reachable) or fall back to its thumbnail, via the Web Share API. */
export async function sharePhoto(photo, { preferOriginal = true } = {}) {
  let file = null;
  if (preferOriginal && photo.fileHandle) {
    try {
      const f = await photo.fileHandle.getFile();
      file = new File([f], photo.name, { type: f.type || 'image/jpeg' });
    } catch {
      file = null;
    }
  }
  if (!file && photo.thumbHandle) {
    const f = await photo.thumbHandle.getFile();
    file = new File([f], photo.thumbFileName || photo.name, { type: 'image/jpeg' });
  }
  if (!file) throw new Error('Aucune image disponible à partager.');

  if (navigator.canShare && navigator.canShare({ files: [file] })) {
    await navigator.share({ files: [file], title: photo.name });
    return 'shared';
  }
  // Fallback: trigger a download.
  const url = URL.createObjectURL(file);
  const a = document.createElement('a');
  a.href = url;
  a.download = file.name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10000);
  return 'downloaded';
}
