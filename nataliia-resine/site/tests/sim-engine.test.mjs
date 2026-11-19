// Tests du moteur du simulateur : node --test tests/
import test from 'node:test';
import assert from 'node:assert/strict';
import { homography, apply, invert3, quadOk, defaultQuad, lighting, luminance, maskedLighting, mipmaps, render, feather } from '../static/js/sim-engine.js';

const close = (a, b, eps = 1e-6) => assert.ok(Math.abs(a - b) < eps, `${a} ≠ ${b}`);

test('homographie : les 4 coins vont sur le carré unité', () => {
  const q = [[120, 300], [480, 310], [620, 590], [40, 580]];
  const H = homography(q);
  [[0, 0], [1, 0], [1, 1], [0, 1]].forEach(([u, v], i) => {
    const [x, y] = apply(H, ...q[i]);
    close(x, u); close(y, v);
  });
  const Hi = invert3(H);
  const [x, y] = apply(Hi, 0.5, 0.5);
  const [u, v] = apply(H, x, y);
  close(u, 0.5); close(v, 0.5);
});

test('quadrilatère : convexe accepté, croisé ou aplati refusé', () => {
  assert.equal(quadOk([[0, 0], [10, 0], [10, 10], [0, 10]]), true);
  assert.equal(quadOk([[0, 0], [10, 10], [10, 0], [0, 10]]), false);
  assert.equal(quadOk([[0, 0], [5, 0], [10, 0], [0, 10]]), false);
  assert.equal(quadOk(defaultQuad([[0, 0], [100, 0], [100, 80], [0, 80]], 200, 200)), true);
});

function scene(W, H, color, maskFn) {
  const src = new Uint8ClampedArray(W * H * 4);
  const mask = new Uint8Array(W * H);
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    const i = y * W + x;
    src.set([...color, 255], i * 4);
    mask[i] = maskFn(x, y) ? 255 : 0;
  }
  return { src, mask };
}
const flatTex = (rgb, n = 64) => { const d = new Uint8ClampedArray(n * n * 4); for (let i = 0; i < n * n; i++) d.set([...rgb, 255], i * 4); return mipmaps(d, n, n); };

test('rendu : zone recouverte, reste de la photo intact', () => {
  const W = 120; const H = 90;
  const { src, mask } = scene(W, H, [128, 128, 128], (x, y) => y > 45);
  const out = new Uint8ClampedArray(src.length);
  const light = lighting(src, W, H, 4);
  const r = render({ src, W, H, mask, light, quad: [[30, 50], [90, 50], [110, 85], [10, 85]], tiles: 4, tex: flatTex([200, 40, 10]), out });
  assert.equal(r.ok, true);
  const px = (x, y) => [...out.slice((y * W + x) * 4, (y * W + x) * 4 + 3)];
  assert.deepEqual(px(60, 10), [128, 128, 128]);  // hors zone
  assert.deepEqual(px(60, 70), [200, 40, 10]);    // zone, lumière uniforme : couleur exacte du granulat
});

test('rendu : les ombres de la photo restent visibles', () => {
  const W = 100; const H = 100;
  const { src, mask } = scene(W, H, [180, 180, 180], () => true);
  for (let y = 0; y < H; y++) for (let x = 0; x < 50; x++) src.set([60, 60, 60, 255], (y * W + x) * 4); // moitié gauche à l'ombre
  const out = new Uint8ClampedArray(src.length);
  render({ src, W, H, mask, light: lighting(src, W, H, 2), quad: [[10, 10], [90, 10], [90, 90], [10, 90]], tiles: 2, tex: flatTex([150, 150, 150]), out });
  const left = out[(50 * W + 20) * 4]; const right = out[(50 * W + 80) * 4];
  assert.ok(left < right * 0.7, `ombre perdue : ${left} vs ${right}`);
});

test('rendu : bords adoucis = mélange progressif', () => {
  const W = 60; const H = 60;
  const { src, mask } = scene(W, H, [0, 0, 0], (x) => x >= 30);
  feather(mask, W, H, 3);
  assert.ok(mask[30 * W + 29] > 0 && mask[30 * W + 29] < 255);
  const out = new Uint8ClampedArray(src.length);
  render({ src, W, H, mask, light: new Float32Array(W * H).fill(0.5), quad: [[0, 0], [60, 0], [60, 60], [0, 60]], tiles: 1, tex: flatTex([255, 255, 255]), out });
  const v = out[(30 * W + 29) * 4];
  assert.ok(v > 0 && v < 255, `pas de transition : ${v}`);
});

test('rendu : perspective impossible ou zone vide signalées', () => {
  const W = 20; const H = 20;
  const { src, mask } = scene(W, H, [1, 2, 3], () => false);
  const out = new Uint8ClampedArray(src.length);
  assert.equal(render({ src, W, H, mask, light: new Float32Array(W * H), quad: [[0, 0], [10, 0], [10, 10], [0, 10]], tiles: 1, tex: flatTex([0, 0, 0]), out }).raison, 'zone');
  assert.equal(render({ src, W, H, mask, light: new Float32Array(W * H), quad: [[0, 0], [0, 0], [0, 0], [0, 0]], tiles: 1, tex: flatTex([0, 0, 0]), out }).raison, 'perspective');
});

test('mipmaps : moyenne 2x2 et niveaux jusqu’à 8 px', () => {
  const d = new Uint8ClampedArray(16 * 16 * 4);
  for (let i = 0; i < 256; i++) d.set(i % 2 ? [255, 255, 255, 255] : [0, 0, 0, 255], i * 4);
  const m = mipmaps(d, 16, 16);
  assert.equal(m.length, 2);
  assert.equal(m[1].w, 8);
  assert.ok(Math.abs(m[1].data[0] - 127) <= 1);
});

test('performance : photo de 1600 x 1200 rendue en moins de 1,5 s', () => {
  const W = 1600; const H = 1200;
  const { src, mask } = scene(W, H, [120, 110, 100], (x, y) => y > 500);
  const tex = mipmaps(new Uint8ClampedArray(1024 * 1024 * 4).fill(200), 1024, 1024);
  const out = new Uint8ClampedArray(src.length);
  const t0 = performance.now();
  const light = lighting(src, W, H, 10);
  render({ src, W, H, mask, light, quad: [[500, 600], [1100, 600], [1400, 1100], [200, 1100]], tiles: 6, tex, out });
  const ms = performance.now() - t0;
  console.log(`    rendu 1600x1200 : ${ms.toFixed(0)} ms`);
  assert.ok(ms < 1500);
});

test('rendu : perspective dont le dénominateur est négatif (cas réel de la terrasse)', () => {
  const W = 400; const H = 225;
  const zone = [[0, 130], [220, 118], [400, 140], [400, 225], [0, 225]];
  const quad = defaultQuad(zone, W, H);
  const Hm = homography(quad);
  const cx = quad.reduce((a, p) => a + p[0], 0) / 4; const cy = quad.reduce((a, p) => a + p[1], 0) / 4;
  assert.ok(Hm[6] * cx + Hm[7] * cy + 1 < 0, 'le cas testé doit avoir un dénominateur négatif');
  const { src, mask } = scene(W, H, [100, 100, 100], (x, y) => y > 150);
  const out = new Uint8ClampedArray(src.length);
  render({ src, W, H, mask, light: new Float32Array(W * H).fill(0.5), quad, tiles: 3, tex: flatTex([220, 30, 30]), out });
  assert.deepEqual([...out.slice((200 * W + 200) * 4, (200 * W + 200) * 4 + 3)], [220, 30, 30]);
});

test('éclairage : le ciel hors zone ne déborde pas sur le sol', () => {
  const W = 80; const H = 80;
  const src = new Uint8ClampedArray(W * H * 4); const mask = new Uint8Array(W * H);
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    const i = y * W + x; const sol = y >= 40;
    src.set(sol ? [100, 100, 100, 255] : [255, 255, 255, 255], i * 4); mask[i] = sol ? 255 : 0;
  }
  const L = maskedLighting(luminance(src, W, H), mask, W, H, 10);
  const top = L[41 * W + 40]; const bottom = L[75 * W + 40];
  assert.ok(Math.abs(top - bottom) < 0.01, `halo du ciel : ${top} vs ${bottom}`);
});
