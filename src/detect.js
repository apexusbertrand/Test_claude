import '@tensorflow/tfjs';
import * as cocoSsd from '@tensorflow-models/coco-ssd';
import { withTimeout, reportAiStatus } from './ai-status.js';

// The COCO-SSD weights are fetched from an external CDN
// (storage.googleapis.com/tfjs-models/…). On mobile that download can stall
// indefinitely without ever failing, so it is bounded here; if it doesn't
// arrive in time the whole object-detection feature is switched off for the
// session rather than re-stalling on every single photo.
const MODEL_LOAD_TIMEOUT_MS = 25000;
const DETECT_TIMEOUT_MS = 15000;

let modelPromise = null;
let disabled = false;

function loadModel() {
  if (!modelPromise) {
    modelPromise = withTimeout(
      cocoSsd.load({ base: 'lite_mobilenet_v2' }),
      MODEL_LOAD_TIMEOUT_MS,
      'téléchargement du modèle de détection d\'objets'
    );
  }
  return modelPromise;
}

export function isObjectDetectionDisabled() {
  return disabled;
}

function disable(reason) {
  if (disabled) return;
  disabled = true;
  reportAiStatus(`Détection d'objets/animaux désactivée : ${reason}. Les miniatures et les autres tags continuent normalement.`);
}

// Map COCO-SSD class names to French tag {category, value} entries.
const ANIMALS = new Set(['bird', 'cat', 'dog', 'horse', 'sheep', 'cow', 'elephant', 'bear', 'zebra', 'giraffe']);
const FR_LABELS = {
  person: 'Personne',
  bird: 'Oiseau',
  cat: 'Chat',
  dog: 'Chien',
  horse: 'Cheval',
  sheep: 'Mouton',
  cow: 'Vache',
  elephant: 'Éléphant',
  bear: 'Ours',
  zebra: 'Zèbre',
  giraffe: 'Girafe',
};

/** Run object detection on an already-decoded image element/canvas. Never throws, never hangs. */
export async function detectTags(imageSource, { minScore = 0.55 } = {}) {
  if (disabled) return [];
  try {
    const model = await loadModel();
    const predictions = await withTimeout(model.detect(imageSource, 20, minScore), DETECT_TIMEOUT_MS, 'détection d\'objets');
    const tags = new Map();
    let personCount = 0;
    for (const p of predictions) {
      if (p.score < minScore) continue;
      if (p.class === 'person') {
        personCount += 1;
        continue;
      }
      if (ANIMALS.has(p.class)) {
        const value = FR_LABELS[p.class] || p.class;
        tags.set(`animal:${value}`, { category: 'animal', value });
      }
    }
    if (personCount === 1) tags.set('personne:1', { category: 'personne', value: '1 personne' });
    else if (personCount > 1) tags.set('personne:n', { category: 'personne', value: `${personCount} personnes` });
    return Array.from(tags.values());
  } catch (err) {
    console.warn('Détection IA indisponible:', err);
    disable(err.message || String(err));
    return [];
  }
}

export function preloadModel() {
  loadModel().catch((err) => disable(err.message || String(err)));
}

/**
 * Await model readiness once, up front, instead of finding out lazily on the
 * first photo. Resolves to true/false — never throws — so callers can run
 * this for every AI feature in parallel and report one clear outcome before
 * touching a single photo.
 */
export async function ensureReady() {
  if (disabled) return false;
  try {
    await loadModel();
    return true;
  } catch (err) {
    disable(err.message || String(err));
    return false;
  }
}
