import * as faceapi from '@vladmandic/face-api';

const MODELS_URL = './models/faceapi';
const MATCH_THRESHOLD = 0.5; // lower = stricter match (euclidean distance on 128-d descriptor)
const COUNT_TAG_RE = /^\d+\s*personnes?$/i;

let modelsPromise = null;

function loadModels() {
  if (!modelsPromise) {
    modelsPromise = Promise.all([
      faceapi.nets.tinyFaceDetector.loadFromUri(MODELS_URL),
      faceapi.nets.faceLandmark68TinyNet.loadFromUri(MODELS_URL),
      faceapi.nets.faceRecognitionNet.loadFromUri(MODELS_URL),
    ]);
  }
  return modelsPromise;
}

export function preloadFaceModels() {
  loadModels().catch(() => {});
}

/** Detect faces in an image/canvas and return their 128-d descriptors + bounding boxes. Never throws. */
export async function detectFaces(imageSource) {
  try {
    await loadModels();
    const results = await faceapi
      .detectAllFaces(imageSource, new faceapi.TinyFaceDetectorOptions({ inputSize: 416, scoreThreshold: 0.5 }))
      .withFaceLandmarks(true)
      .withFaceDescriptors();
    return results.map((r) => ({
      descriptor: Array.from(r.descriptor),
      box: { x: r.detection.box.x, y: r.detection.box.y, width: r.detection.box.width, height: r.detection.box.height },
    }));
  } catch (err) {
    console.warn('Détection de visages indisponible:', err);
    return [];
  }
}

/** A "personne" tag is a name (not the auto-generated "N personnes" count tag). */
export function isNameTag(tag) {
  return tag.category === 'personne' && !COUNT_TAG_RE.test(tag.value.trim());
}

/**
 * Build a face matcher from every named "personne" tag across the library.
 * Only photos with exactly one detected face are used as exemplars for a name,
 * since a multi-face photo doesn't tell us which face the name refers to.
 */
export function buildFaceMatcher(photos) {
  const byName = new Map();
  for (const photo of photos) {
    const faces = photo.faces || [];
    if (faces.length !== 1) continue;
    const nameTags = (photo.tags || []).filter(isNameTag);
    for (const tag of nameTags) {
      const name = tag.value.trim();
      if (!byName.has(name)) byName.set(name, []);
      byName.get(name).push(new Float32Array(faces[0].descriptor));
    }
  }
  if (byName.size === 0) return null;
  const labeled = Array.from(byName.entries()).map(
    ([name, descriptors]) => new faceapi.LabeledFaceDescriptors(name, descriptors)
  );
  return new faceapi.FaceMatcher(labeled, MATCH_THRESHOLD);
}

/**
 * Propagate known names to every photo's detected faces that match closely enough.
 * Returns the list of photos whose tags were changed (caller is responsible for persisting them).
 */
export function propagateNames(photos) {
  const matcher = buildFaceMatcher(photos);
  if (!matcher) return [];

  const changed = [];
  for (const photo of photos) {
    const faces = photo.faces || [];
    if (faces.length === 0) continue;
    const existingNames = new Set((photo.tags || []).filter(isNameTag).map((t) => t.value.trim()));
    let addedAny = false;
    for (const face of faces) {
      const best = matcher.findBestMatch(new Float32Array(face.descriptor));
      if (best.label === 'unknown') continue;
      if (existingNames.has(best.label)) continue;
      photo.tags.push({ category: 'personne', value: best.label });
      existingNames.add(best.label);
      addedAny = true;
    }
    if (addedAny) changed.push(photo);
  }
  return changed;
}
