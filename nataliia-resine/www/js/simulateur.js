// Simulateur Nataliia Résine : interface (page « Créez votre projet »).
import { homography, invert3, apply, frontSign, quadOk, defaultQuad, luminance, maskedLighting, mipmaps, render, feather } from './sim-engine.js';

const DATA = JSON.parse(document.getElementById('sim-data').textContent);
// Granulat choisi depuis une fiche produit (« Essayer sur ma photo »)
DATA.preselect = new URLSearchParams(location.search).get('granulat');
const MAX_PHOTOS = 2;
const WORK = 1600;     // taille de calcul (côté le plus long)
const SAMPLE_M = 0.3;  // côté de l'échantillon photographié pour la texture (30 cm, voir guide)
const $ = (s, el = document) => el.querySelector(s);
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

const state = { photos: [], cur: -1, step: 'zone', mode: 'zone' };
const textures = new Map();
const cur = () => state.photos[state.cur];

// ---------------------------------------------------------------------------
// Chargement d'une photo
async function loadPhoto(file) {
  if (!file.type.startsWith('image/')) throw new Error('Ce fichier n’est pas une image.');
  let bmp;
  try { bmp = await createImageBitmap(file, { imageOrientation: 'from-image' }); }
  catch {
    const img = new Image(); img.src = URL.createObjectURL(file); await img.decode(); bmp = img;
  }
  const k = Math.min(1, WORK / Math.max(bmp.width, bmp.height));
  const W = Math.round(bmp.width * k); const H = Math.round(bmp.height * k);
  const c = canvas(W, H); const ctx = c.getContext('2d');
  ctx.imageSmoothingQuality = 'high';
  ctx.drawImage(bmp, 0, 0, W, H);
  if (bmp.close) bmp.close();
  const img = ctx.getImageData(0, 0, W, H);
  return {
    name: file.name, W, H, canvas: c, src: img.data,
    lum: luminance(img.data, W, H),
    zone: [], excl: [], quad: null, quadTouched: false, rectM: 2,
    granulat: (DATA.preselect && DATA.granulats.find((g) => g.id === DATA.preselect)?.id) || DATA.granulats[0]?.id,
    out: null, beforeUrl: c.toDataURL('image/jpeg', 0.85), afterUrl: null,
  };
}
const canvas = (w, h) => { const c = document.createElement('canvas'); c.width = w; c.height = h; return c; };

async function texture(id) {
  if (textures.has(id)) return textures.get(id);
  const g = DATA.granulats.find((x) => x.id === id);
  const img = new Image();
  img.crossOrigin = 'anonymous';
  img.src = g.texture;
  await img.decode();
  const n = 1024;
  const k = Math.min(1, n / Math.max(img.naturalWidth, img.naturalHeight));
  const w = Math.max(16, Math.round(img.naturalWidth * k)); const h = Math.max(16, Math.round(img.naturalHeight * k));
  const c = canvas(w, h); const ctx = c.getContext('2d');
  ctx.drawImage(img, 0, 0, w, h);
  const t = mipmaps(ctx.getImageData(0, 0, w, h).data, w, h);
  textures.set(id, t);
  return t;
}

// ---------------------------------------------------------------------------
// Rendu
function buildMask(p) {
  const c = canvas(p.W, p.H); const ctx = c.getContext('2d');
  const poly = (pts) => { ctx.beginPath(); pts.forEach(([x, y], i) => (i ? ctx.lineTo(x, y) : ctx.moveTo(x, y))); ctx.closePath(); ctx.fill(); };
  ctx.fillStyle = '#fff';
  if (p.zone.length >= 3) poly(p.zone);
  ctx.globalCompositeOperation = 'destination-out';
  p.excl.filter((e) => e.length >= 3).forEach(poly);
  const d = ctx.getImageData(0, 0, p.W, p.H).data;
  const m = new Uint8Array(p.W * p.H);
  for (let i = 0; i < m.length; i++) m[i] = d[i * 4 + 3];
  return feather(m, p.W, p.H, Math.max(1, Math.round(p.W / 800)));
}

let renderTimer;
function scheduleRender() { clearTimeout(renderTimer); renderTimer = setTimeout(doRender, 120); }

async function doRender() {
  const p = cur();
  if (!p) return;
  const status = $('#sim-status');
  if (p.zone.length < 3) { p.afterUrl = null; status.textContent = 'Délimitez d’abord la surface (étape 1).'; showResult(); return; }
  if (!p.quadTouched) p.quad = defaultQuad(p.zone, p.W, p.H);
  if (!quadOk(p.quad)) { status.textContent = 'Les 4 points de perspective se croisent : déplacez-les (étape 2).'; return; }
  if (!p.granulat) { status.textContent = 'Choisissez un granulat.'; return; }
  status.textContent = 'Calcul du rendu…';
  try {
    const tex = await texture(p.granulat);
    const out = new Uint8ClampedArray(p.src.length);
    const mask = buildMask(p);
    const light = maskedLighting(p.lum, mask, p.W, p.H, Math.max(p.W, p.H) / 30);
    const r = render({ src: p.src, W: p.W, H: p.H, mask, light, quad: p.quad, tiles: p.rectM / SAMPLE_M, tex, out });
    if (!r.ok) { status.textContent = r.raison === 'zone' ? 'La zone est vide : ajoutez des points.' : 'Perspective impossible : déplacez les 4 points.'; return; }
    const c = canvas(p.W, p.H);
    c.getContext('2d').putImageData(new ImageData(out, p.W, p.H), 0, 0);
    p.out = c;
    p.afterUrl = c.toDataURL('image/jpeg', 0.85);
    const g = DATA.granulats.find((x) => x.id === p.granulat);
    status.textContent = `Rendu avec ${g.nom}.`;
    showResult();
    if (state.step === 'granulat') draw();
  } catch (e) {
    console.error(e);
    status.textContent = 'Impossible de charger la texture de ce granulat. Réessayez.';
  }
}

// ---------------------------------------------------------------------------
// Éditeur (canvas d'affichage)
const view = () => $('#sim-canvas');
let scale = 1;

function draw() {
  const p = cur(); const c = view();
  if (!p) return;
  let cssW = c.parentElement.clientWidth;
  // Plein écran : la photo tient dans la hauteur disponible, outils compris
  if (state.full) cssW = Math.min(cssW, Math.max(200, (window.innerHeight - 64) * p.W / p.H));
  scale = cssW / p.W;
  const dpr = window.devicePixelRatio || 1;
  c.style.width = cssW + 'px'; c.style.height = Math.round(p.H * scale) + 'px';
  c.width = Math.round(cssW * dpr); c.height = Math.round(p.H * scale * dpr);
  const ctx = c.getContext('2d');
  ctx.setTransform(dpr * scale, 0, 0, dpr * scale, 0, 0);
  ctx.drawImage(state.step === 'granulat' && p.out ? p.out : p.canvas, 0, 0);
  const lw = 2 / scale; const r = 9 / scale;
  const polyPath = (pts) => { ctx.beginPath(); pts.forEach(([x, y], i) => (i ? ctx.lineTo(x, y) : ctx.moveTo(x, y))); if (pts.length > 2) ctx.closePath(); };
  const dots = (pts, fill) => pts.forEach(([x, y]) => { ctx.beginPath(); ctx.arc(x, y, r, 0, 7); ctx.fillStyle = fill; ctx.fill(); ctx.lineWidth = lw; ctx.strokeStyle = '#fff'; ctx.stroke(); });

  if (state.step === 'zone') {
    if (p.zone.length) {
      polyPath(p.zone); ctx.fillStyle = 'rgba(210,178,123,.28)'; if (p.zone.length > 2) ctx.fill();
      ctx.lineWidth = lw * 1.5; ctx.strokeStyle = '#d2b27b'; ctx.stroke();
      dots(p.zone, '#b58a50');
    }
    p.excl.forEach((e) => {
      if (!e.length) return;
      polyPath(e); ctx.fillStyle = 'rgba(163,49,42,.3)'; if (e.length > 2) ctx.fill();
      ctx.lineWidth = lw * 1.5; ctx.strokeStyle = '#e06a5f'; ctx.stroke();
      dots(e, '#a3312a');
    });
  }
  if (state.step === 'persp') {
    if (p.zone.length > 2) { polyPath(p.zone); ctx.lineWidth = lw; ctx.strokeStyle = 'rgba(255,255,255,.6)'; ctx.setLineDash([6 / scale, 6 / scale]); ctx.stroke(); ctx.setLineDash([]); }
    if (!p.quadTouched) p.quad = defaultQuad(p.zone, p.W, p.H);
    const Hm = quadOk(p.quad) ? homography(p.quad) : null;
    const Hi = Hm && invert3(Hm);
    if (Hi) {
      const sd = frontSign(Hi);
      // Quadrillage au sol : une case = un dixième du rectangle, prolongé autour
      ctx.lineWidth = lw * 0.6; ctx.strokeStyle = 'rgba(255,255,255,.45)';
      for (let k = -10; k <= 20; k++) {
        const t = k / 10;
        for (const horiz of [true, false]) {
          ctx.beginPath(); let started = false;
          for (let s = -1; s <= 2.001; s += 0.05) {
            const [x, y, d] = apply(Hi, horiz ? s : t, horiz ? t : s);
            if (d * sd <= 0 || !isFinite(x)) { started = false; continue; }
            if (started) ctx.lineTo(x, y); else { ctx.moveTo(x, y); started = true; }
          }
          ctx.stroke();
        }
      }
    }
    polyPath(p.quad); ctx.lineWidth = lw * 2; ctx.strokeStyle = '#f1c879'; ctx.stroke();
    dots(p.quad, '#f1c879');
  }
}

function toImage(e) {
  const r = view().getBoundingClientRect();
  return [(e.clientX - r.left) / scale, (e.clientY - r.top) / scale];
}

function hit(pts, x, y) {
  const R = 24 / scale;
  let best = -1; let bd = R * R;
  pts.forEach(([px, py], i) => { const d = (px - x) ** 2 + (py - y) ** 2; if (d < bd) { bd = d; best = i; } });
  return best;
}

let drag = null;
function onDown(e) {
  const p = cur(); if (!p || state.step === 'granulat') return;
  const [x, y] = toImage(e);
  if (state.step === 'persp') {
    const i = hit(p.quad, x, y);
    if (i >= 0) { drag = { list: p.quad, i }; p.quadTouched = true; view().setPointerCapture(e.pointerId); }
    return;
  }
  const lists = [p.zone, ...p.excl];
  for (const list of lists) {
    const i = hit(list, x, y);
    if (i >= 0) { drag = { list, i }; view().setPointerCapture(e.pointerId); return; }
  }
  drag = { add: true, x0: e.clientX, y0: e.clientY };
}
function onMove(e) {
  if (!drag || drag.add) return;
  const p = cur();
  const [x, y] = toImage(e);
  drag.list[drag.i] = [Math.max(0, Math.min(p.W, x)), Math.max(0, Math.min(p.H, y))];
  draw();
}
function onUp(e) {
  const p = cur();
  if (drag?.add && Math.hypot(e.clientX - drag.x0, e.clientY - drag.y0) < 8) {
    const [x, y] = toImage(e);
    if (state.mode === 'excl') {
      if (!p.excl.length) p.excl.push([]);
      p.excl[p.excl.length - 1].push([x, y]);
    } else p.zone.push([x, y]);
    draw();
    scheduleRender();
  } else if (drag && !drag.add) scheduleRender();
  drag = null;
}

// ---------------------------------------------------------------------------
// Interface
function setStep(step) {
  state.step = step;
  document.querySelectorAll('[data-step]').forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.step === step)));
  document.querySelectorAll('[data-panel]').forEach((el) => { el.hidden = el.dataset.panel !== step; });
  if (step === 'persp') { const p = cur(); if (p && !p.quadTouched) p.quad = defaultQuad(p.zone, p.W, p.H); }
  draw();
  if (step === 'granulat') doRender();
}

function setMode(mode) {
  state.mode = mode;
  const p = cur();
  if (mode === 'excl' && p && (!p.excl.length || p.excl[p.excl.length - 1].length >= 3)) p.excl.push([]);
  document.querySelectorAll('[data-mode]').forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.mode === mode)));
  $('#sim-hint').textContent = mode === 'excl'
    ? 'Touchez l’image autour de l’objet à ne pas recouvrir (pot, meuble, bordure…).'
    : 'Touchez l’image pour placer des points tout autour de la surface à recouvrir. Faites glisser un point pour le corriger.';
}

function renderTabs() {
  $('#sim-tabs').innerHTML = state.photos.map((p, i) => `<button type="button" class="filter" data-tab="${i}" aria-pressed="${i === state.cur}">Photo ${i + 1}</button>`).join('')
    + (state.photos.length && state.photos.length < MAX_PHOTOS ? `<label class="filter" style="cursor:pointer">＋ Une 2ᵉ photo<input type="file" accept="image/*" class="visually-hidden" id="sim-add2"></label>` : '')
    + (state.photos.length ? `<button type="button" class="filter" id="sim-remove">Retirer cette photo</button>` : '');
  document.querySelectorAll('[data-tab]').forEach((b) => b.addEventListener('click', () => select(+b.dataset.tab)));
  $('#sim-add2')?.addEventListener('change', (e) => addFiles(e.target.files));
  $('#sim-remove')?.addEventListener('click', () => {
    state.photos.splice(state.cur, 1);
    state.cur = Math.min(state.cur, state.photos.length - 1);
    refresh();
  });
}

function renderSwatches() {
  const p = cur();
  $('#sim-swatches').innerHTML = DATA.granulats.map((g) => `<button class="swatch" type="button" data-g="${esc(g.id)}" aria-pressed="${p?.granulat === g.id}"><img src="${esc(g.thumb || g.texture)}" alt="" crossorigin="anonymous">${esc(g.nom)}</button>`).join('');
  document.querySelectorAll('[data-g]').forEach((b) => b.addEventListener('click', () => {
    cur().granulat = b.dataset.g;
    renderSwatches();
    doRender();
  }));
}

function showResult() {
  const p = cur();
  const box = $('#sim-result');
  if (!p || !p.afterUrl) { box.hidden = true; return; }
  box.hidden = false;
  $('#cmp-before').src = p.beforeUrl;
  $('#cmp-after').src = p.afterUrl;
  const cmp = $('#sim-compare');
  cmp.style.setProperty('--pos', $('#cmp-range').value + '%');
}

function select(i) {
  state.cur = i;
  refresh();
}

function refresh() {
  const has = state.photos.length > 0;
  $('#sim-drop').hidden = has;
  $('#sim-editor').hidden = !has;
  renderTabs();
  if (!has) { $('#sim-result').hidden = true; return; }
  const p = cur();
  $('#rect-m').value = p.rectM;
  $('#rect-out').textContent = `${p.rectM.toLocaleString('fr-FR')} m`;
  renderSwatches();
  setStep(p.zone.length >= 3 ? state.step : 'zone');
  setMode('zone');
  showResult();
  if (p.zone.length >= 3) doRender();
}

async function addFiles(files) {
  const list = [...files].slice(0, MAX_PHOTOS - state.photos.length);
  if (!list.length) return;
  $('#sim-status').textContent = 'Préparation de la photo…';
  for (const f of list) {
    try { state.photos.push(await loadPhoto(f)); state.cur = state.photos.length - 1; }
    catch (e) { alert(e.message); }
  }
  $('#sim-status').textContent = '';
  state.step = 'zone';
  refresh();
  $('#sim-editor').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function download() {
  const p = cur(); if (!p?.out) return;
  const gap = 12; const band = 46;
  const c = canvas(p.W * 2 + gap, p.H + band); const ctx = c.getContext('2d');
  ctx.fillStyle = '#11100f'; ctx.fillRect(0, 0, c.width, c.height);
  ctx.drawImage(p.canvas, 0, band); ctx.drawImage(p.out, p.W + gap, band);
  ctx.fillStyle = '#d2b27b'; ctx.font = '600 22px Arial'; ctx.textBaseline = 'middle';
  const g = DATA.granulats.find((x) => x.id === p.granulat);
  ctx.fillText('Avant', 14, band / 2);
  ctx.fillText(`Après : ${g?.nom || ''} (aperçu indicatif, Nataliia Résine)`, p.W + gap + 14, band / 2);
  c.toBlob((b) => {
    const a = document.createElement('a');
    a.href = URL.createObjectURL(b);
    a.download = `projet-nataliia-resine-photo-${state.cur + 1}.jpg`;
    a.click();
  }, 'image/jpeg', 0.88);
}

// ---------------------------------------------------------------------------
// Envoi de la demande
const blobOf = (c, q = 0.85) => new Promise((ok) => c.toBlob(ok, 'image/jpeg', q));
function small(c, max) {
  const k = Math.min(1, max / Math.max(c.width, c.height));
  const s = canvas(Math.round(c.width * k), Math.round(c.height * k));
  s.getContext('2d').drawImage(c, 0, 0, s.width, s.height);
  return s;
}

async function send(e) {
  e.preventDefault();
  const f = e.target;
  const err = $('#form-errors');
  const v = (n) => (f.elements[n]?.value || '').trim();
  const errors = [];
  const ready = state.photos.filter((p) => p.out);
  if (!ready.length) errors.push('Ajoutez au moins une photo et réalisez sa simulation.');
  if (!v('nom')) errors.push('Indiquez votre nom.');
  if (!/^[+\d][\d .-]{8,}$/.test(v('telephone'))) errors.push('Indiquez un numéro de téléphone valide.');
  if (!/^\S+@\S+\.\S+$/.test(v('email'))) errors.push('Indiquez une adresse e-mail valide.');
  if (!v('ville')) errors.push('Indiquez votre ville.');
  if (!f.elements.consentement.checked) errors.push('Cochez la case d’accord pour que nous puissions traiter votre demande.');
  if (DATA.turnstile && !f.elements['cf-turnstile-response']?.value) errors.push('Patientez pendant la vérification anti-robot, puis réessayez.');
  err.innerHTML = errors.map(esc).join('<br>');
  err.hidden = !errors.length;
  if (errors.length) { err.scrollIntoView({ behavior: 'smooth', block: 'center' }); return; }

  const btn = $('button[type=submit]', f);
  btn.disabled = true; btn.textContent = 'Envoi en cours…';
  try {
    const fd = new FormData();
    for (const k of ['nom', 'telephone', 'email', 'ville', 'type_surface', 'surface_m2', 'delai', 'message', 'site_web']) fd.append(k, v(k));
    fd.append('consentement', 'oui');
    if (DATA.turnstile) fd.append('turnstile', f.elements['cf-turnstile-response'].value);
    for (let i = 0; i < ready.length; i++) {
      const p = ready[i]; const g = DATA.granulats.find((x) => x.id === p.granulat);
      fd.append(`photo${i + 1}_original`, await blobOf(p.canvas), `photo${i + 1}-original.jpg`);
      fd.append(`photo${i + 1}_rendu`, await blobOf(p.out), `photo${i + 1}-rendu.jpg`);
      fd.append(`photo${i + 1}_granulat`, g?.slug || p.granulat);
      fd.append(`photo${i + 1}_granulat_nom`, g?.nom || '');
    }
    if (DATA.endpoint) {
      const r = await fetch(DATA.endpoint, { method: 'POST', body: fd, headers: { apikey: DATA.anonKey, Authorization: `Bearer ${DATA.anonKey}` } });
      const body = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(body.erreur || 'Envoi impossible. Réessayez ou appelez-nous.');
    } else {
      await saveDemo(fd, ready);
    }
    f.hidden = true;
    $('#sim-done').hidden = false;
    $('#sim-done').scrollIntoView({ behavior: 'smooth', block: 'center' });
  } catch (ex) {
    err.textContent = ex.message; err.hidden = false;
    btn.disabled = false; btn.textContent = 'Envoyer ma demande';
  }
}

// Mode démonstration (pas de base branchée) : la demande est déposée dans l'administration de démonstration
async function saveDemo(fd, ready) {
  const toData = (c) => small(c, 900).toDataURL('image/jpeg', 0.75);
  const id = crypto.randomUUID ? crypto.randomUUID() : String(Date.now());
  const now = new Date().toISOString();
  const photos = ready.map((p, i) => {
    const g = DATA.granulats.find((x) => x.id === p.granulat);
    return { original: `demo/${id}/photo${i + 1}-original.jpg`, rendu: `demo/${id}/photo${i + 1}-rendu.jpg`, produit: g?.slug, produit_nom: g?.nom, data: [toData(p.canvas), toData(p.out)] };
  });
  let inbox = [];
  try { inbox = JSON.parse(localStorage.getItem('nr-demo-inbox')) || []; } catch { /* vide */ }
  inbox.push({
    id, at: now,
    prospect: { nom: fd.get('nom'), email: fd.get('email'), telephone: fd.get('telephone'), ville: fd.get('ville') },
    demande: { type_surface: fd.get('type_surface'), surface_m2: fd.get('surface_m2') || null, delai: fd.get('delai'), message: fd.get('message') },
    photos,
  });
  try { localStorage.setItem('nr-demo-inbox', JSON.stringify(inbox)); }
  catch { throw new Error('Mode démonstration : mémoire du navigateur pleine. Videz l’administration de démonstration.'); }
}

// ---------------------------------------------------------------------------
function init() {
  if (!DATA.granulats.length) { $('#sim-root').innerHTML = '<p class="notice">Le simulateur sera disponible dès que les granulats seront en ligne. En attendant, contactez-nous.</p>'; return; }
  const input = $('#sim-file');
  input.addEventListener('change', (e) => addFiles(e.target.files));
  const zone = $('#sim-drop');
  ['dragover', 'dragenter'].forEach((t) => zone.addEventListener(t, (e) => { e.preventDefault(); zone.classList.add('is-over'); }));
  ['dragleave', 'drop'].forEach((t) => zone.addEventListener(t, () => zone.classList.remove('is-over')));
  zone.addEventListener('drop', (e) => { e.preventDefault(); addFiles(e.dataTransfer.files); });

  const c = view();
  c.addEventListener('pointerdown', onDown);
  c.addEventListener('pointermove', onMove);
  c.addEventListener('pointerup', onUp);
  c.addEventListener('pointercancel', () => { drag = null; });

  document.querySelectorAll('[data-step]').forEach((b) => b.addEventListener('click', () => setStep(b.dataset.step)));
  document.querySelectorAll('[data-mode]').forEach((b) => b.addEventListener('click', () => setMode(b.dataset.mode)));
  $('#sim-undo').addEventListener('click', () => {
    const p = cur();
    const ex = p.excl[p.excl.length - 1];
    if (state.mode === 'excl' && ex?.length) ex.pop(); else p.zone.pop();
    draw(); scheduleRender();
  });
  $('#sim-clear').addEventListener('click', () => {
    const p = cur(); p.zone = []; p.excl = []; p.quadTouched = false; p.out = null; p.afterUrl = null;
    setMode('zone'); draw(); showResult();
  });
  $('#sim-next1').addEventListener('click', () => {
    if (cur().zone.length < 3) { $('#sim-hint').textContent = 'Placez au moins 3 points autour de la surface.'; return; }
    setStep('persp');
  });
  $('#sim-next2').addEventListener('click', () => setStep('granulat'));
  $('#sim-reset-persp').addEventListener('click', () => { const p = cur(); p.quadTouched = false; draw(); scheduleRender(); });
  $('#rect-m').addEventListener('input', (e) => {
    const p = cur(); p.rectM = +e.target.value;
    $('#rect-out').textContent = `${p.rectM.toLocaleString('fr-FR')} m`;
    scheduleRender();
  });
  $('#cmp-range').addEventListener('input', (e) => $('#sim-compare').style.setProperty('--pos', e.target.value + '%'));
  $('#sim-download').addEventListener('click', download);
  const full = (on) => {
    state.full = on;
    $('#sim-editor').classList.toggle('sim-full', on);
    document.body.classList.toggle('sim-no-scroll', on);
    $('#sim-full').setAttribute('aria-pressed', String(on));
    $('#sim-full').textContent = on ? '✕ Fermer le plein écran' : '⤢ Plein écran';
    draw();
  };
  $('#sim-full').addEventListener('click', () => full(!state.full));
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && state.full) full(false); });
  $('#sim-form').addEventListener('submit', send);
  window.addEventListener('resize', () => draw());
  refresh();
}

init();
