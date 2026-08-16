import '@tensorflow/tfjs';
import * as cocoSsd from '@tensorflow-models/coco-ssd';

let modelPromise = null;

function loadModel() {
  if (!modelPromise) modelPromise = cocoSsd.load({ base: 'lite_mobilenet_v2' });
  return modelPromise;
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

/** Run object detection on an already-decoded image element/canvas. Never throws. */
export async function detectTags(imageSource, { minScore = 0.55 } = {}) {
  try {
    const model = await loadModel();
    const predictions = await model.detect(imageSource, 20, minScore);
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
    return [];
  }
}

export function preloadModel() {
  loadModel().catch(() => {});
}
