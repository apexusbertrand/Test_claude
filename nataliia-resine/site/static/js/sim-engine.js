// Moteur du simulateur Nataliia Résine. Code pur (tableaux de pixels), sans DOM : testable sous Node.
//
// Principe : le visiteur repère 4 points formant un rectangle posé au sol (vu en perspective).
// On calcule l'homographie qui envoie l'image sur ce rectangle, ce qui donne, pour chaque pixel
// de la zone, sa position « au sol ». La texture du granulat est répétée au sol à sa taille réelle,
// puis éclairée avec la lumière de la photo d'origine (ombres conservées).

// Homographie : 4 points image (x, y) -> carré unité (0,0) (1,0) (1,1) (0,1)
export function homography(src, dst = [[0, 0], [1, 0], [1, 1], [0, 1]]) {
  const A = [];
  const b = [];
  for (let i = 0; i < 4; i++) {
    const [x, y] = src[i];
    const [u, v] = dst[i];
    A.push([x, y, 1, 0, 0, 0, -u * x, -u * y]); b.push(u);
    A.push([0, 0, 0, x, y, 1, -v * x, -v * y]); b.push(v);
  }
  const h = solve(A, b);
  if (!h) return null;
  return [...h, 1];
}

// Élimination de Gauss avec pivot partiel
function solve(A, b) {
  const n = b.length;
  const M = A.map((row, i) => [...row, b[i]]);
  for (let c = 0; c < n; c++) {
    let p = c;
    for (let r = c + 1; r < n; r++) if (Math.abs(M[r][c]) > Math.abs(M[p][c])) p = r;
    if (Math.abs(M[p][c]) < 1e-12) return null;
    [M[c], M[p]] = [M[p], M[c]];
    for (let r = 0; r < n; r++) {
      if (r === c) continue;
      const f = M[r][c] / M[c][c];
      for (let k = c; k <= n; k++) M[r][k] -= f * M[c][k];
    }
  }
  return M.map((row, i) => row[n] / row[i]);
}

export function apply(H, x, y) {
  const d = H[6] * x + H[7] * y + H[8];
  return [(H[0] * x + H[1] * y + H[2]) / d, (H[3] * x + H[4] * y + H[5]) / d, d];
}

// Signe « devant » d'une homographie inverse (carré unité -> image), pour tracer le quadrillage
export function frontSign(Hi) {
  return Math.sign(apply(Hi, 0.5, 0.5)[2]) || 1;
}

export function invert3(m) {
  const [a, b, c, d, e, f, g, h, i] = m;
  const A = e * i - f * h; const B = -(d * i - f * g); const C = d * h - e * g;
  const det = a * A + b * B + c * C;
  if (Math.abs(det) < 1e-15) return null;
  return [A / det, -(b * i - c * h) / det, (b * f - c * e) / det,
    B / det, (a * i - c * g) / det, -(a * f - c * d) / det,
    C / det, -(a * h - b * g) / det, (a * e - b * d) / det];
}

// Quadrilatère convexe et non dégénéré (sinon la perspective n'a pas de sens)
export function quadOk(q) {
  let sign = 0;
  for (let i = 0; i < 4; i++) {
    const [ax, ay] = q[i]; const [bx, by] = q[(i + 1) % 4]; const [cx, cy] = q[(i + 2) % 4];
    const cross = (bx - ax) * (cy - by) - (by - ay) * (cx - bx);
    if (Math.abs(cross) < 1e-6) return false;
    if (sign && Math.sign(cross) !== sign) return false;
    sign = Math.sign(cross);
  }
  return true;
}

// Perspective par défaut à partir de l'emprise de la zone : trapèze plus étroit en haut
export function defaultQuad(points, W, H) {
  if (!points || points.length < 3) {
    return [[W * 0.3, H * 0.55], [W * 0.7, H * 0.55], [W * 0.85, H * 0.9], [W * 0.15, H * 0.9]];
  }
  const xs = points.map((p) => p[0]); const ys = points.map((p) => p[1]);
  const x0 = Math.min(...xs); const x1 = Math.max(...xs); const y0 = Math.min(...ys); const y1 = Math.max(...ys);
  const w = x1 - x0; const h = y1 - y0;
  const top = y0 + h * 0.35; const bot = y0 + h * 0.85;
  return [[x0 + w * 0.3, top], [x0 + w * 0.7, top], [x0 + w * 0.82, bot], [x0 + w * 0.18, bot]];
}

// Luminance lissée de la photo : sert d'« éclairage » appliqué à la texture
export function lighting(rgba, W, H, radius) {
  const L = new Float32Array(W * H);
  for (let i = 0, j = 0; i < L.length; i++, j += 4) {
    L[i] = (0.2126 * rgba[j] + 0.7152 * rgba[j + 1] + 0.0722 * rgba[j + 2]) / 255;
  }
  const r = Math.max(1, Math.round(radius));
  boxBlur(L, W, H, r); boxBlur(L, W, H, r);
  return L;
}

// Luminance brute (0..1)
export function luminance(rgba, W, H) {
  const L = new Float32Array(W * H);
  for (let i = 0, j = 0; i < L.length; i++, j += 4) L[i] = (0.2126 * rgba[j] + 0.7152 * rgba[j + 1] + 0.0722 * rgba[j + 2]) / 255;
  return L;
}

// Éclairage du sol seul : moyenne locale de la luminance pondérée par le masque.
// Le ciel ou les murs autour de la zone ne débordent pas sur le sol, et un grand rayon
// garde les ombres portées sans recopier les motifs de l'ancien revêtement.
export function maskedLighting(lum, mask, W, H, radius) {
  const a = new Float32Array(W * H); const m = new Float32Array(W * H);
  for (let i = 0; i < a.length; i++) { const w = mask[i] / 255; a[i] = lum[i] * w; m[i] = w; }
  const r = Math.max(1, Math.round(radius));
  boxBlur(a, W, H, r); boxBlur(a, W, H, r); boxBlur(m, W, H, r); boxBlur(m, W, H, r);
  for (let i = 0; i < a.length; i++) a[i] = m[i] > 1e-4 ? a[i] / m[i] : 0;
  return a;
}

// Flou boîte séparable, en place
export function boxBlur(a, W, H, r) {
  const tmp = new Float32Array(Math.max(W, H));
  for (let y = 0; y < H; y++) {
    const o = y * W; let s = 0; let n = 0;
    for (let x = -r; x <= r; x++) if (x >= 0 && x < W) { s += a[o + x]; n++; }
    for (let x = 0; x < W; x++) {
      tmp[x] = s / n;
      const out = x - r; const inn = x + r + 1;
      if (out >= 0) { s -= a[o + out]; n--; }
      if (inn < W) { s += a[o + inn]; n++; }
    }
    for (let x = 0; x < W; x++) a[o + x] = tmp[x];
  }
  for (let x = 0; x < W; x++) {
    let s = 0; let n = 0;
    for (let y = -r; y <= r; y++) if (y >= 0 && y < H) { s += a[y * W + x]; n++; }
    for (let y = 0; y < H; y++) {
      tmp[y] = s / n;
      const out = y - r; const inn = y + r + 1;
      if (out >= 0) { s -= a[out * W + x]; n--; }
      if (inn < H) { s += a[inn * W + x]; n++; }
    }
    for (let y = 0; y < H; y++) a[y * W + x] = tmp[y];
  }
}

// Pyramide de la texture (moyennes 2x2) : évite le moiré au loin
export function mipmaps(rgba, w, h) {
  const levels = [{ data: rgba, w, h }];
  while (w > 8 && h > 8) {
    const nw = w >> 1; const nh = h >> 1;
    const prev = levels[levels.length - 1].data;
    const d = new Uint8ClampedArray(nw * nh * 4);
    for (let y = 0; y < nh; y++) {
      for (let x = 0; x < nw; x++) {
        const o = (y * nw + x) * 4;
        const a = ((2 * y) * w + 2 * x) * 4; const b = a + 4; const c = a + w * 4; const e = c + 4;
        for (let k = 0; k < 4; k++) d[o + k] = (prev[a + k] + prev[b + k] + prev[c + k] + prev[e + k]) >> 2;
      }
    }
    levels.push({ data: d, w: nw, h: nh });
    w = nw; h = nh;
  }
  return levels;
}

// Rendu.
//  src     : pixels de la photo (RGBA), W x H
//  mask    : Uint8Array W x H, 0..255 (surface à recouvrir, bords adoucis)
//  light   : Float32Array W x H (lighting())
//  quad    : 4 points image du rectangle repéré au sol
//  tiles   : nombre de répétitions de la texture le long d'un côté du rectangle
//            (= côté du rectangle en m / côté de l'échantillon photographié en m)
//  tex     : mipmaps(...)
//  out     : RGBA de sortie (même taille que src)
const SHADE = 0.6;
const TINT = 0.5;
export function render({ src, W, H, mask, light, quad, tiles, tex, out }) {
  out.set(src);
  const Hm = homography(quad);
  if (!Hm) return { ok: false, raison: 'perspective' };
  // Éclairage moyen de la zone, pour normaliser
  let sum = 0; let cnt = 0; let sr = 0; let sg = 0; let sb = 0;
  for (let i = 0; i < mask.length; i++) {
    if (mask[i] > 127) { sum += light[i]; cnt++; sr += src[i * 4]; sg += src[i * 4 + 1]; sb += src[i * 4 + 2]; }
  }
  if (!cnt) return { ok: false, raison: 'zone' };
  const mean = sum / cnt;
  // Teinte de la lumière (soleil couchant, ombre bleutée…) estimée sur le sol, appliquée à mi-force
  const gray = (sr + sg + sb) / 3 || 1;
  const tr = 1 + TINT * (sr / gray - 1); const tg = 1 + TINT * (sg / gray - 1); const tb = 1 + TINT * (sb / gray - 1);
  const base = tex[0];
  const levels = tex.length;
  const [h0, h1, h2, h3, h4, h5, h6, h7] = Hm;
  // Le dénominateur projectif n'a pas de signe imposé : on prend celui du centre du rectangle
  // comme « devant », le signe opposé correspond à l'au-delà de l'horizon.
  const cx = (quad[0][0] + quad[1][0] + quad[2][0] + quad[3][0]) / 4;
  const cy = (quad[0][1] + quad[1][1] + quad[2][1] + quad[3][1]) / 4;
  const sgn = Math.sign(h6 * cx + h7 * cy + 1) || 1;
  for (let y = 0; y < H; y++) {
    for (let x = 0; x < W; x++) {
      const i = y * W + x;
      const m = mask[i];
      if (!m) continue;
      const den = h6 * x + h7 * y + 1;
      if (den * sgn <= 1e-9) continue; // au-delà de l'horizon
      const nu = h0 * x + h1 * y + h2; const nv = h3 * x + h4 * y + h5;
      const u = (nu / den) * tiles; const v = (nv / den) * tiles;
      // Taille d'un pixel écran dans la texture -> niveau de détail
      const d2 = den * den;
      const dux = ((h0 * den - nu * h6) / d2) * tiles; const dvx = ((h3 * den - nv * h6) / d2) * tiles;
      const duy = ((h1 * den - nu * h7) / d2) * tiles; const dvy = ((h4 * den - nv * h7) / d2) * tiles;
      const fp = Math.max(Math.abs(dux), Math.abs(dvx), Math.abs(duy), Math.abs(dvy)) * base.w;
      // Deux niveaux voisins mélangés (filtrage trilinéaire simplifié) : pas de bandes au loin
      let lf = fp > 1 ? Math.log2(fp) : 0;
      if (lf > levels - 1) lf = levels - 1;
      const l0 = lf | 0; const l1 = l0 + 1 < levels ? l0 + 1 : l0; const fr = lf - l0;
      // Répétition en miroir : pas de raccord visible même si la photo d'échantillon ne se répète pas
      let tu = u % 2; if (tu < 0) tu += 2; if (tu > 1) tu = 2 - tu;
      let tv = v % 2; if (tv < 0) tv += 2; if (tv > 1) tv = 2 - tv;
      const t = tex[l0]; const t1 = tex[l1];
      const o = (Math.min(t.h - 1, (tv * t.h) | 0) * t.w + Math.min(t.w - 1, (tu * t.w) | 0)) * 4;
      const o1 = (Math.min(t1.h - 1, (tv * t1.h) | 0) * t1.w + Math.min(t1.w - 1, (tu * t1.w) | 0)) * 4;
      const cr = t.data[o] + (t1.data[o1] - t.data[o]) * fr;
      const cg = t.data[o + 1] + (t1.data[o1 + 1] - t.data[o + 1]) * fr;
      const cb = t.data[o + 2] + (t1.data[o1 + 2] - t.data[o + 2]) * fr;
      // Effet de la lumière atténué : on garde les grandes ombres, pas les nuances de l'ancien sol
      let s = 1 + SHADE * (light[i] / mean - 1);
      s = s < 0.45 ? 0.45 : s > 1.35 ? 1.35 : s;
      const a = m / 255; const j = i * 4;
      out[j] = src[j] + (cr * s * tr - src[j]) * a;
      out[j + 1] = src[j + 1] + (cg * s * tg - src[j + 1]) * a;
      out[j + 2] = src[j + 2] + (cb * s * tb - src[j + 2]) * a;
    }
  }
  return { ok: true };
}

// Adoucit les bords du masque (Uint8Array) sur `r` pixels
export function feather(mask, W, H, r) {
  const f = Float32Array.from(mask);
  boxBlur(f, W, H, r);
  for (let i = 0; i < mask.length; i++) mask[i] = f[i];
  return mask;
}
