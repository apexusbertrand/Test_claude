// Administration Nataliia Résine : interface.
import { createApi, slugify, newMediaPath } from './api.js';
import { prepareImage } from './images.js';

const app = document.getElementById('app');
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const $ = (sel, el = document) => el.querySelector(sel);
const $$ = (sel, el = document) => [...el.querySelectorAll(sel)];
const fmtDate = (d) => (d ? new Date(d).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }) : '');
const fmtDateTime = (d) => (d ? new Date(d).toLocaleString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '');

const CATS = { granulats: 'Granulats de marbre', profils: 'Profils de finition', autre: 'Autres produits' };
const STATUTS = { nouveau: 'Nouveau', contacte: 'Contacté', devis: 'Devis envoyé', signe: 'Signé', sans_suite: 'Sans suite' };
const TABLES = { realisations: 'Réalisation', produits: 'Produit', prestations: 'Prestation', avis: 'Avis', reglages: 'Réglages' };

let api;
let user = null;
let dirty = false;
let leaving = null; // fichiers envoyés pendant l'édition, à supprimer si on quitte sans enregistrer

// ---------------------------------------------------------------------------
// Utilitaires d'interface
let toastTimer;
function toast(msg, err = false) {
  const t = $('#toast');
  t.textContent = msg;
  t.className = 'show' + (err ? ' err' : '');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { t.className = ''; }, err ? 6000 : 3500);
}
const fail = (e) => { console.error(e); toast(e?.message || 'Une erreur est survenue.', true); };

function busy(btn, on, label) {
  if (!btn) return;
  if (on) { btn.dataset.label = btn.innerHTML; btn.disabled = true; btn.textContent = label || 'Patientez…'; }
  else { btn.disabled = false; if (btn.dataset.label) btn.innerHTML = btn.dataset.label; }
}

function go(hash) { location.hash = hash; }

function confirmLeave() {
  if (!dirty) return true;
  return confirm('Des modifications ne sont pas enregistrées. Quitter quand même ?');
}

window.addEventListener('beforeunload', (e) => { if (dirty) { e.preventDefault(); e.returnValue = ''; } });

// ---------------------------------------------------------------------------
// Mise en page
async function shell(active, html, { savebar = '' } = {}) {
  let nouvelles = 0;
  try { nouvelles = (await api.list('prospects')).filter((p) => p.statut === 'nouveau').length; } catch { /* rien */ }
  const tabs = [
    ['#/', 'Accueil', 'accueil'],
    ['#/realisations', 'Réalisations', 'realisations'],
    ['#/produits', 'Produits', 'produits'],
    ['#/demandes', 'Demandes' + (nouvelles ? ` <span class="count">${nouvelles}</span>` : ''), 'demandes'],
    ['#/avis', 'Avis', 'avis'],
    ['#/prestations', 'Prestations', 'prestations'],
    ['#/reglages', 'Réglages', 'reglages'],
    ['#/historique', 'Historique', 'historique'],
    ['#/compte', 'Mon compte', 'compte'],
  ];
  app.innerHTML = `${api.mode === 'demo' ? '<div class="demo-bar">Mode démonstration : les modifications restent dans ce navigateur et ne sont pas publiées. <button type="button" id="demo-reset">Remettre à zéro</button></div>' : ''}
<header class="top">
  <div class="top-row">
    <a class="top-brand" href="#/">Nataliia <b>Résine</b><small>Administration</small></a>
    <div class="top-actions"><a class="btn btn-ghost" href="../index.html" target="_blank" rel="noopener">Voir le site ↗</a></div>
  </div>
  <nav class="nav" aria-label="Rubriques">${tabs.map(([h, l, k]) => `<a href="${h}"${k === active ? ' aria-current="page"' : ''}>${l}</a>`).join('')}</nav>
</header>
<main id="main">${html}</main>
${savebar ? `<div class="savebar"><div class="savebar-in">${savebar}</div></div>` : ''}`;
  $('#demo-reset')?.addEventListener('click', () => {
    if (confirm('Effacer toutes les modifications de démonstration ?')) { api.reset(); location.reload(); }
  });
  window.scrollTo(0, 0);
}

function trackDirty(form) {
  dirty = false;
  form.addEventListener('input', (e) => { dirty = true; e.target.closest?.('.field-error')?.classList.remove('field-error'); });
  form.addEventListener('change', () => { dirty = true; });
}

// ---------------------------------------------------------------------------
// Gestion des photos dans un formulaire
class MediaList {
  constructor(el, { items = [], max = 1, min = 0, folder, altBase = () => '', label, hint = '', maxW = 2560, warnSmall = false }) {
    Object.assign(this, { el, max, min, folder, altBase, label, hint, maxW, warnSmall });
    this.items = items.map((x) => ({ ...x }));
    this.initial = new Set(items.flatMap((x) => [x.path, x.small]).filter(Boolean));
    this.uploaded = [];
    this.pending = 0;
    this.render();
  }
  render() {
    if (!this.el.isConnected) return;
    const can = this.items.length + this.pending < this.max;
    this.el.innerHTML = `<div class="photos">
      ${this.items.map((m, i) => `<div class="photo">
        <img src="${esc(api.url(m))}" alt="">
        ${this.warnSmall && m.w && m.w < 1920 ? '<span class="warn" title="Moins de 1920 px de large : un peu juste en plein écran">Petite</span>' : ''}
        <div class="tools">
          ${this.max > 1 && i > 0 ? `<button type="button" data-act="left" data-i="${i}" aria-label="Déplacer avant">←</button>` : ''}
          ${this.max > 1 && i < this.items.length - 1 ? `<button type="button" data-act="right" data-i="${i}" aria-label="Déplacer après">→</button>` : ''}
          <button type="button" data-act="del" data-i="${i}" aria-label="Retirer la photo">✕</button>
        </div>
        <input type="text" data-alt="${i}" value="${esc(m.alt || '')}" placeholder="Description de la photo" aria-label="Description de la photo ${i + 1}">
      </div>`).join('')}
      ${Array.from({ length: this.pending }, () => '<div class="busy">Préparation de la photo…</div>').join('')}
      ${can ? `<label class="add-photo"><span>＋ ${this.items.length ? 'Ajouter' : esc(this.label || 'Ajouter une photo')}</span><span class="hint">${this.max > 1 ? `${this.items.length}/${this.max}` : 'Appareil photo ou galerie'}</span><input type="file" accept="image/*" ${this.max - this.items.length > 1 ? 'multiple' : ''}></label>` : ''}
    </div>${this.hint ? `<p class="hint" style="margin-top:8px">${this.hint}</p>` : ''}`;
    $$('[data-act]', this.el).forEach((b) => b.addEventListener('click', () => this.act(b.dataset.act, +b.dataset.i)));
    $$('[data-alt]', this.el).forEach((inp) => inp.addEventListener('input', () => { this.items[+inp.dataset.alt].alt = inp.value; }));
    $('input[type=file]', this.el)?.addEventListener('change', (e) => this.add([...e.target.files]));
  }
  act(a, i) {
    if (a === 'del') this.items.splice(i, 1);
    if (a === 'left') [this.items[i - 1], this.items[i]] = [this.items[i], this.items[i - 1]];
    if (a === 'right') [this.items[i + 1], this.items[i]] = [this.items[i], this.items[i + 1]];
    dirty = true;
    this.render();
  }
  async add(files) {
    files = files.slice(0, this.max - this.items.length - this.pending);
    for (const file of files) {
      this.pending++;
      this.render();
      try {
        const img = await prepareImage(file, { maxW: this.maxW });
        const path = newMediaPath(this.folder, img.big.ext);
        const small = path.replace(/\.(\w+)$/, '-800.$1');
        await api.upload(path, img.big.blob);
        await api.upload(small, img.small.blob);
        // Page quittée pendant l'envoi : le fichier ne servira pas
        if (!this.el.isConnected) { api.removeFiles([path, small]).catch(() => {}); return; }
        this.uploaded.push(path, small);
        const n = this.items.length + 1;
        this.items.push({ path, small, w: img.width, h: img.height, alt: this.altBase(n) });
        if (img.tooSmall && this.warnSmall) toast('Photo de moins de 1920 px de large : elle sera un peu floue en plein écran.');
        dirty = true;
      } catch (e) { fail(e); }
      this.pending--;
      this.render();
    }
  }
  isBusy() { return this.pending > 0; }
  value() {
    return this.items.map((m, i) => ({ ...m, alt: (m.alt || '').trim() || this.altBase(i + 1) }));
  }
  // Fichiers à supprimer après enregistrement : ceux d'origine retirés
  removedPaths() {
    const kept = new Set(this.items.flatMap((x) => [x.path, x.small]));
    return [...this.initial].filter((p) => !kept.has(p) && !p.startsWith('static:'));
  }
  // Fichiers à supprimer si on abandonne : ceux envoyés pendant cette édition
  abandonedPaths() { return this.uploaded; }
}

// Un PDF (fiche technique)
class PdfField {
  constructor(el, value) { this.el = el; this.value = value || null; this.initial = value?.path || null; this.uploaded = null; this.render(); }
  render() {
    this.el.innerHTML = this.value
      ? `<div class="file-line"><span>📄 ${esc(this.value.nom || 'Document')}</span><button type="button" class="btn btn-sm btn-danger">Retirer</button></div>`
      : `<label class="btn btn-ghost btn-sm" style="position:relative">Ajouter un PDF<input type="file" accept="application/pdf" style="position:absolute;opacity:0;width:1px;height:1px"></label>`;
    $('button', this.el)?.addEventListener('click', () => { this.value = null; dirty = true; this.render(); });
    $('input', this.el)?.addEventListener('change', async (e) => {
      const f = e.target.files[0];
      if (!f) return;
      if (f.type !== 'application/pdf') return toast('Ce fichier n’est pas un PDF.', true);
      if (f.size > 15 * 1024 * 1024) return toast('PDF trop lourd (15 Mo maximum).', true);
      this.el.innerHTML = '<p class="hint">Envoi du PDF…</p>';
      try {
        const path = newMediaPath('documents', 'pdf');
        await api.upload(path, f);
        this.uploaded = path;
        this.value = { path, nom: f.name.replace(/\.pdf$/i, '') };
        dirty = true;
      } catch (err) { fail(err); }
      this.render();
    });
  }
  removedPaths() { return this.initial && this.initial !== this.value?.path ? [this.initial] : []; }
  abandonedPaths() { return this.uploaded ? [this.uploaded] : []; }
}

// Prépare le nettoyage des fichiers selon que l'on enregistre ou non
function fileWatch(fields) {
  leaving = () => api.removeFiles(fields.flatMap((f) => f.abandonedPaths())).catch(() => {});
  return {
    busy: () => fields.some((f) => f.isBusy?.()),
    saved: () => { leaving = null; return api.removeFiles(fields.flatMap((f) => f.removedPaths())).catch(() => {}); },
  };
}

function uniqueSlug(base, rows, id) {
  const taken = new Set(rows.filter((r) => r.id !== id).map((r) => r.slug));
  let s = slugify(base); let n = 2;
  while (taken.has(s)) s = `${slugify(base)}-${n++}`;
  return s;
}

function showErrors(form, errors) {
  $$('.field-error', form).forEach((f) => f.classList.remove('field-error'));
  $('#errors', form)?.remove();
  if (!errors.length) return false;
  errors.forEach(([name]) => name && form.querySelector(`[name="${name}"]`)?.closest('.field')?.classList.add('field-error'));
  form.insertAdjacentHTML('afterbegin', `<div class="error-box" id="errors">${errors.map(([, m]) => esc(m)).join('<br>')}</div>`);
  window.scrollTo({ top: 0, behavior: 'smooth' });
  return true;
}

// Liste réordonnable
function listPage({ active, title, table, rows, newHref, newLabel, emptyText, rowHtml, editHref }) {
  return shell(active, `<div class="head"><h1>${title}</h1><a class="btn" href="${newHref}">＋ ${newLabel}</a></div>
  ${rows.length ? `<div class="list" id="list">${rows.map((r, i) => `<div class="row" data-id="${r.id}">
      ${rowHtml(r)}
      <div class="row-move"><button type="button" data-up="${i}" ${i === 0 ? 'disabled' : ''} aria-label="Monter">▲</button><button type="button" data-down="${i}" ${i === rows.length - 1 ? 'disabled' : ''} aria-label="Descendre">▼</button></div>
    </div>`).join('')}</div>
    <p class="hint" style="margin-top:12px">L’ordre de cette liste est celui du site. Utilisez ▲ ▼ pour le changer.</p>`
    : `<div class="empty"><p>${emptyText}</p><a class="btn" href="${newHref}">＋ ${newLabel}</a></div>`}`).then(() => {
    $$('[data-up],[data-down]').forEach((b) => b.addEventListener('click', async () => {
      const i = +(b.dataset.up ?? b.dataset.down);
      const j = b.dataset.up !== undefined ? i - 1 : i + 1;
      const ids = rows.map((r) => r.id);
      [ids[i], ids[j]] = [ids[j], ids[i]];
      try { await api.reorder(table, ids); toast('Ordre enregistré.'); route(); } catch (e) { fail(e); }
    }));
    $$('.row-main').forEach((a) => { if (!a.getAttribute('href')) a.setAttribute('href', editHref(a.closest('.row').dataset.id)); });
  });
}

const thumb = (m) => `<div class="row-thumb">${m ? `<img src="${esc(api.url(m))}" alt="" loading="lazy">` : '◇'}</div>`;
const draft = (r) => (r.publie === false ? ' <span class="badge badge-draft">Brouillon</span>' : '');

// ---------------------------------------------------------------------------
// Aperçu (rendu avec la feuille de style du site)
function preview(title, bodyHtml) {
  const doc = `<!doctype html><html lang="fr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><link rel="stylesheet" href="../css/site.css"></head>
<body><main>${bodyHtml}</main><script>document.querySelectorAll('.compare').forEach(function(b){var i=b.querySelector('input');function s(){b.style.setProperty('--pos',i.value+'%')}i.addEventListener('input',s);s()})<\/script></body></html>`;
  const m = document.createElement('div');
  m.className = 'modal';
  m.innerHTML = `<div class="modal-bar"><span>Aperçu : ${esc(title)}</span><button type="button" class="btn btn-sm btn-ghost">Fermer</button></div><iframe title="Aperçu"></iframe>`;
  document.body.appendChild(m);
  $('iframe', m).srcdoc = doc;
  const close = () => m.remove();
  $('button', m).addEventListener('click', close);
  document.addEventListener('keydown', function k(e) { if (e.key === 'Escape') { close(); document.removeEventListener('keydown', k); } });
}
const pimg = (m, alt) => (m ? `<img src="${esc(api.fullUrl(m))}" alt="${esc(m.alt || alt)}">` : '');

// ---------------------------------------------------------------------------
// Écrans
const screens = {};

screens.accueil = async () => {
  const [rea, prods, prospects, pend] = await Promise.all([api.list('realisations'), api.list('produits'), api.list('prospects'), api.pending()]);
  const nouvelles = prospects.filter((p) => p.statut === 'nouveau').length;
  await shell('accueil', `<h1>Bonjour</h1>
  <div class="publish${pend.changes ? '' : ' ok'}" id="publish">
    <div><p><strong>${pend.changes ? 'Des modifications attendent d’être publiées.' : 'Le site est à jour.'}</strong></p>
    <p class="muted small" id="pub-status">${pend.lastPublication ? 'Dernière publication : ' + fmtDateTime(pend.lastPublication) : 'Aucune publication pour l’instant.'}</p></div>
    <button class="btn ${pend.changes ? '' : 'btn-ghost'}" id="pub-btn" type="button">Publier le site</button>
  </div>
  <div class="stack">
    <div class="tiles">
      <a class="tile" href="#/realisations"><strong>${rea.length}</strong><span>réalisation${rea.length > 1 ? 's' : ''}${rea.some((r) => !r.publie) ? ` dont ${rea.filter((r) => !r.publie).length} en brouillon` : ''}</span></a>
      <a class="tile" href="#/produits"><strong>${prods.length}</strong><span>produit${prods.length > 1 ? 's' : ''}${prods.some((p) => p.exemple) ? ` dont ${prods.filter((p) => p.exemple).length} d’exemple` : ''}</span></a>
      <a class="tile" href="#/demandes"><strong>${nouvelles}</strong><span>nouvelle${nouvelles > 1 ? 's' : ''} demande${nouvelles > 1 ? 's' : ''}</span></a>
    </div>
    <div class="panel">
      <h2>Ajouter rapidement</h2>
      <div style="display:flex;gap:10px;flex-wrap:wrap">
        <a class="btn" href="#/realisations/nouveau">＋ Une réalisation</a>
        <a class="btn btn-ghost" href="#/produits/nouveau">＋ Un produit</a>
        <a class="btn btn-ghost" href="#/avis/nouveau">＋ Un avis client</a>
      </div>
    </div>
    ${prods.some((p) => p.exemple) ? `<div class="warn-box">Les produits marqués « Exemple » sont là pour la démonstration. Supprimez-les quand vos vrais granulats sont saisis.</div>` : ''}
    <p class="small muted">Besoin d’aide ? <a href="aide.html" target="_blank" rel="noopener">Guide d’utilisation</a></p>
  </div>`);
  $('#pub-btn').addEventListener('click', publish);
};

async function publish() {
  const btn = $('#pub-btn');
  busy(btn, true, 'Publication…');
  try {
    const r = await api.publish();
    if (r?.demo) {
      toast('Mode démonstration : rien n’est publié en vrai.');
      route();
      return;
    }
    $('#publish').className = 'publish';
    $('#publish strong').textContent = 'Publication en cours…';
    $('#pub-status').textContent = 'Le site sera à jour dans 1 à 3 minutes. Vous pouvez continuer à travailler.';
    let tries = 0;
    const poll = async () => {
      if (!$('#pub-status')) return;
      const s = await api.publishStatus();
      tries++;
      if (s?.statut === 'completed') {
        const ok = s.conclusion === 'success';
        $('#publish').className = 'publish' + (ok ? ' ok' : '');
        $('#publish strong').textContent = ok ? 'Le site est à jour.' : 'La publication a échoué.';
        $('#pub-status').textContent = ok ? 'Publié à l’instant.' : 'Réessayez. Si le problème continue, contactez Apexus.';
        busy(btn, false);
        return;
      }
      if (tries < 24) setTimeout(poll, 10000); else busy(btn, false);
    };
    setTimeout(poll, 15000);
  } catch (e) { busy(btn, false); fail(e); }
}

// --- Réalisations -----------------------------------------------------------
screens.realisations = async () => {
  const [rows, prest] = await Promise.all([api.list('realisations'), api.list('prestations')]);
  const label = Object.fromEntries(prest.map((p) => [p.slug, p.titre]));
  await listPage({
    active: 'realisations', title: 'Réalisations', table: 'realisations', rows,
    newHref: '#/realisations/nouveau', newLabel: 'Nouvelle réalisation',
    emptyText: 'Aucune réalisation pour l’instant. Ajoutez votre premier chantier avec ses photos avant et après.',
    editHref: (id) => `#/realisations/${id}`,
    rowHtml: (r) => `${thumb(r.apres?.[0])}<a class="row-main"><strong>${esc(r.titre)}${draft(r)}</strong><span>${esc(label[r.type] || r.type)}${r.ville ? ' · ' + esc(r.ville) : ''}${r.date_chantier ? ' · ' + fmtDate(r.date_chantier) : ''}</span></a>`,
  });
};

screens.realisation = async (id) => {
  const [rows, prest, prods] = await Promise.all([api.list('realisations'), api.list('prestations'), api.list('produits')]);
  const r = id === 'nouveau' ? { titre: '', type: '', ville: '', texte: '', apres: [], avant: [], produits: [], publie: true, date_chantier: null } : rows.find((x) => x.id === id);
  if (!r) return notFound();
  await shell('realisations', `<p class="crumb"><a href="#/realisations">Réalisations</a></p>
  <h1>${r.id ? 'Modifier la réalisation' : 'Nouvelle réalisation'}</h1>
  <form class="editor" id="f" novalidate>
    <fieldset><legend>Le chantier</legend>
      <div class="field"><label for="titre">Titre <span class="req">*</span></label><input type="text" id="titre" name="titre" value="${esc(r.titre)}" placeholder="Ex. Terrasse en granulat clair" maxlength="90"></div>
      <div class="grid2">
        <div class="field"><label for="type">Type de prestation <span class="req">*</span></label><select id="type" name="type"><option value="">Choisir</option>${prest.map((p) => `<option value="${esc(p.slug)}"${p.slug === r.type ? ' selected' : ''}>${esc(p.titre)}</option>`).join('')}</select></div>
        <div class="field"><label for="ville">Ville</label><input type="text" id="ville" name="ville" value="${esc(r.ville || '')}" placeholder="Facultatif"></div>
        <div class="field"><label for="date">Date du chantier</label><input type="date" id="date" name="date" value="${esc(r.date_chantier || '')}"></div>
      </div>
      <div class="field"><label for="texte">Description</label><textarea id="texte" name="texte" placeholder="Quelques lignes sur le chantier (facultatif)">${esc(r.texte)}</textarea></div>
    </fieldset>
    <fieldset><legend>Photos après <span class="req">*</span></legend><div class="field" data-name="apres"><div id="m-apres"></div></div></fieldset>
    <fieldset><legend>Photos avant</legend><div id="m-avant"></div></fieldset>
    <fieldset><legend>Produits utilisés</legend>
      ${prods.length ? `<div class="checks">${prods.map((p) => `<label class="check"><input type="checkbox" name="produits" value="${p.id}"${(r.produits || []).includes(p.id) ? ' checked' : ''}><span>${esc(p.nom)}${p.exemple ? ' <span class="badge badge-ex">Exemple</span>' : ''}</span></label>`).join('')}</div>` : '<p class="hint">Aucun produit saisi. <a href="#/produits/nouveau">Ajouter un produit</a></p>'}
    </fieldset>
    <fieldset><legend>Publication</legend>
      <label class="check"><input type="checkbox" name="publie" ${r.publie ? 'checked' : ''}><span>Visible sur le site<br><span class="hint">Décochez pour garder en brouillon.</span></span></label>
    </fieldset>
  </form>`, {
    savebar: `<div class="left">${r.id ? '<button class="btn btn-danger" type="button" id="del">Supprimer</button>' : ''}</div>
      <div class="right"><button class="btn btn-ghost" type="button" id="prev">Aperçu</button><button class="btn" type="button" id="save">Enregistrer</button></div>`,
  });
  const f = $('#f');
  trackDirty(f);
  const titre = () => $('#titre')?.value.trim() || 'Réalisation';
  const apres = new MediaList($('#m-apres'), { items: r.apres, max: 4, min: 1, folder: 'realisations', warnSmall: true, label: 'Ajouter les photos après', altBase: (n) => `${titre()}, après, photo ${n}`, hint: '1 à 4 photos. Grand format conseillé (le site les affiche en plein écran). La première est la photo principale.' });
  const avant = new MediaList($('#m-avant'), { items: r.avant, max: 2, folder: 'realisations', warnSmall: true, label: 'Ajouter les photos avant', altBase: (n) => `${titre()}, avant, photo ${n}`, hint: '0 à 2 photos. Avec une photo avant, le site affiche un curseur avant/après.' });
  const files = fileWatch([apres, avant]);

  const collect = () => ({
    ...(r.id ? { id: r.id } : {}),
    titre: $('#titre').value.trim(),
    type: $('#type').value,
    ville: $('#ville').value.trim() || null,
    date_chantier: $('#date').value || null,
    texte: $('#texte').value.trim(),
    apres: apres.value(),
    avant: avant.value(),
    produits: $$('input[name=produits]:checked').map((c) => c.value),
    publie: $('input[name=publie]').checked,
  });

  $('#prev').addEventListener('click', () => {
    const v = collect();
    const t = prest.find((p) => p.slug === v.type)?.titre || '';
    preview(v.titre || 'Réalisation', `<section class="page-head"><span class="eyebrow">${esc(t)}${v.ville ? ' · ' + esc(v.ville) : ''}</span><h1>${esc(v.titre || 'Sans titre')}</h1>${v.texte ? `<p class="lead">${esc(v.texte)}</p>` : ''}</section>
      <section class="section" style="padding-top:0">
      ${v.avant[0] && v.apres[0] ? `<div class="compare">${pimg(v.avant[0])}<div class="after">${pimg(v.apres[0])}</div><span class="lbl lbl-before">Avant</span><span class="lbl lbl-after">Après</span><span class="handle"></span><input type="range" min="0" max="100" value="50" aria-label="Comparer"></div>` : ''}
      <div class="gallery" style="margin-top:16px">${v.apres.map((m) => pimg(m)).join('')}${v.avant.slice(1).map((m) => pimg(m)).join('')}</div>
      ${v.produits.length ? `<h2 style="margin-top:40px;font-size:28px">Produits utilisés</h2><ul>${v.produits.map((id) => `<li>${esc(prods.find((p) => p.id === id)?.nom || '')}</li>`).join('')}</ul>` : ''}
      </section>`);
  });

  $('#save').addEventListener('click', async () => {
    if (files.busy()) return toast('Attendez la fin de l’envoi des photos.', true);
    const v = collect();
    const errors = [];
    if (!v.titre) errors.push(['titre', 'Indiquez un titre.']);
    if (!v.type) errors.push(['type', 'Choisissez le type de prestation.']);
    if (!v.apres.length) errors.push([null, 'Ajoutez au moins une photo après.']);
    if (showErrors(f, errors)) return;
    if (!r.id) v.slug = uniqueSlug(v.titre + (v.ville ? ' ' + v.ville : ''), rows);
    const btn = $('#save');
    busy(btn, true, 'Enregistrement…');
    try {
      const saved = await api.save('realisations', v);
      await files.saved();
      dirty = false;
      toast('Réalisation enregistrée. Pensez à publier le site.');
      go(`#/realisations/${saved.id}`);
    } catch (e) { busy(btn, false); fail(e); }
  });

  $('#del')?.addEventListener('click', async () => {
    if (!confirm(`Supprimer « ${r.titre} » ? Elle disparaîtra du site à la prochaine publication.`)) return;
    try {
      await api.remove('realisations', r.id);
      dirty = false; leaving = null;
      toast('Réalisation supprimée.');
      go('#/realisations');
    } catch (e) { fail(e); }
  });
};

// --- Produits ---------------------------------------------------------------
screens.produits = async () => {
  const rows = await api.list('produits');
  await listPage({
    active: 'produits', title: 'Produits', table: 'produits', rows,
    newHref: '#/produits/nouveau', newLabel: 'Nouveau produit',
    emptyText: 'Aucun produit. Ajoutez vos granulats, profils de finition et autres produits.',
    editHref: (id) => `#/produits/${id}`,
    rowHtml: (p) => `${thumb(p.photos?.[0] || p.texture)}<a class="row-main"><strong>${esc(p.nom)}${draft(p)}${p.exemple ? ' <span class="badge badge-ex">Exemple</span>' : ''}</strong><span>${esc(CATS[p.categorie])}${p.categorie === 'granulats' ? (p.texture ? ' · simulateur ✓' : ' · pas de texture') : ''}</span></a>`,
  });
};

screens.produit = async (id) => {
  const rows = await api.list('produits');
  const p = id === 'nouveau' ? { nom: '', categorie: 'granulats', descriptif: '', photos: [], lien: '', pdf: null, texture: null, grain_mm: null, publie: true, exemple: false } : rows.find((x) => x.id === id);
  if (!p) return notFound();
  await shell('produits', `<p class="crumb"><a href="#/produits">Produits</a></p>
  <h1>${p.id ? 'Modifier le produit' : 'Nouveau produit'}</h1>
  ${p.exemple ? '<div class="warn-box" style="margin-bottom:16px">Produit d’exemple fourni avec le site. Supprimez-le quand vos vrais produits sont saisis.</div>' : ''}
  <form class="editor" id="f" novalidate>
    <fieldset><legend>Le produit</legend>
      <div class="field"><label for="nom">Nom <span class="req">*</span></label><input type="text" id="nom" name="nom" value="${esc(p.nom)}" maxlength="90"></div>
      <div class="field"><label for="categorie">Catégorie <span class="req">*</span></label><select id="categorie" name="categorie">${Object.entries(CATS).map(([k, v]) => `<option value="${k}"${k === p.categorie ? ' selected' : ''}>${v}</option>`).join('')}</select></div>
      <div class="field"><label for="descriptif">Descriptif</label><textarea id="descriptif" name="descriptif">${esc(p.descriptif)}</textarea></div>
    </fieldset>
    <fieldset><legend>Photos</legend><div id="m-photos"></div></fieldset>
    <fieldset id="fs-texture"><legend>Simulateur</legend>
      <div id="tex-warn"></div>
      <div class="field"><span class="label">Texture du granulat</span><div id="m-texture"></div></div>
      <div class="field"><label for="grain">Taille du grain (mm)</label><input type="number" id="grain" name="grain" min="0.5" max="50" step="0.5" inputmode="decimal" value="${p.grain_mm ?? ''}" placeholder="Ex. 4"></div>
    </fieldset>
    <fieldset><legend>Documents et lien</legend>
      <div class="field"><label for="lien">Lien (fournisseur, fiche produit)</label><input type="url" id="lien" name="lien" value="${esc(p.lien || '')}" placeholder="https://…" inputmode="url"></div>
      <div class="field"><span class="label">Fiche technique (PDF)</span><div id="pdf"></div></div>
    </fieldset>
    <fieldset><legend>Publication</legend>
      <label class="check"><input type="checkbox" name="publie" ${p.publie ? 'checked' : ''}><span>Visible sur le site</span></label>
    </fieldset>
  </form>`, {
    savebar: `<div class="left">${p.id ? '<button class="btn btn-danger" type="button" id="del">Supprimer</button>' : ''}</div>
      <div class="right"><button class="btn btn-ghost" type="button" id="prev">Aperçu</button><button class="btn" type="button" id="save">Enregistrer</button></div>`,
  });
  const f = $('#f');
  trackDirty(f);
  const nom = () => $('#nom')?.value.trim() || 'Produit';
  const photos = new MediaList($('#m-photos'), { items: p.photos, max: 8, folder: 'produits', label: 'Ajouter des photos', altBase: (n) => `${nom()}, photo ${n}`, hint: 'Facultatif, jusqu’à 8 photos.' });
  const texture = new MediaList($('#m-texture'), { items: p.texture ? [p.texture] : [], max: 1, folder: 'textures', maxW: 1600, label: 'Ajouter la texture', altBase: () => `Texture ${nom()}`, hint: 'Photo de l’échantillon posé à plat, prise bien au-dessus, en lumière du jour sans ombre, sur au moins 30 x 30 cm. Sans texture, le granulat n’est pas proposé dans le simulateur.' });
  const pdf = new PdfField($('#pdf'), p.pdf);
  const files = fileWatch([photos, texture, pdf]);
  const syncCat = () => {
    if (!$('#categorie')) return; // page quittée entre-temps
    const g = $('#categorie').value === 'granulats';
    $('#fs-texture').hidden = !g;
    $('#tex-warn').innerHTML = g && !texture.items.length ? '<div class="warn-box" style="margin-bottom:14px">Sans texture, ce granulat ne sera pas proposé dans le simulateur.</div>' : '';
  };
  $('#categorie').addEventListener('change', syncCat);
  $('#m-texture').addEventListener('click', () => setTimeout(syncCat, 0));
  $('#m-texture').addEventListener('change', () => setTimeout(syncCat, 1500));
  syncCat();

  const collect = () => {
    const g = $('#categorie').value === 'granulats';
    const grain = parseFloat(String($('#grain').value).replace(',', '.'));
    return {
      ...(p.id ? { id: p.id } : {}),
      nom: $('#nom').value.trim(),
      categorie: $('#categorie').value,
      descriptif: $('#descriptif').value.trim(),
      photos: photos.value(),
      texture: g ? texture.value()[0] || null : null,
      grain_mm: g && grain > 0 ? grain : null,
      lien: $('#lien').value.trim() || null,
      pdf: pdf.value,
      publie: $('input[name=publie]').checked,
    };
  };

  $('#prev').addEventListener('click', () => {
    const v = collect();
    preview(v.nom || 'Produit', `<section class="page-head"><span class="eyebrow">${esc(CATS[v.categorie])}</span><h1>${esc(v.nom || 'Sans nom')}</h1></section>
    <section class="section" style="padding-top:0"><div class="product"><div class="product-media">${v.photos.map((m) => pimg(m)).join('') || (v.texture ? pimg(v.texture) : '')}</div>
    <div>${v.descriptif ? `<p class="lead">${esc(v.descriptif)}</p>` : ''}
    ${v.grain_mm ? `<ul class="facts"><li><span>Taille du grain</span><strong>${v.grain_mm} mm environ</strong></li></ul>` : ''}
    <div class="actions">${v.texture ? '<span class="btn">Essayer sur ma photo ↗</span>' : ''}${v.lien ? '<span class="btn btn-ghost">Site du fournisseur ↗</span>' : ''}${v.pdf ? `<span class="btn btn-ghost">${esc(v.pdf.nom)} (PDF)</span>` : ''}</div></div></div></section>`);
  });

  $('#save').addEventListener('click', async () => {
    if (files.busy()) return toast('Attendez la fin de l’envoi des photos.', true);
    const v = collect();
    const errors = [];
    if (!v.nom) errors.push(['nom', 'Indiquez le nom du produit.']);
    if (v.lien && !/^https?:\/\/\S+\.\S+/.test(v.lien)) errors.push(['lien', 'Le lien doit commencer par https://']);
    if (showErrors(f, errors)) return;
    if (!p.id) v.slug = uniqueSlug(v.nom, rows);
    const btn = $('#save');
    busy(btn, true, 'Enregistrement…');
    try {
      const saved = await api.save('produits', v);
      await files.saved();
      dirty = false;
      toast(v.categorie === 'granulats' && !v.texture ? 'Produit enregistré (sans texture : absent du simulateur).' : 'Produit enregistré. Pensez à publier le site.');
      go(`#/produits/${saved.id}`);
    } catch (e) { busy(btn, false); fail(e); }
  });

  $('#del')?.addEventListener('click', async () => {
    const used = (await api.list('realisations')).filter((r) => (r.produits || []).includes(p.id));
    if (!confirm(`Supprimer « ${p.nom} » ?${used.length ? `\nIl est cité dans ${used.length} réalisation(s), il en sera retiré.` : ''}`)) return;
    try {
      for (const r of used) await api.save('realisations', { id: r.id, produits: r.produits.filter((x) => x !== p.id) });
      await api.remove('produits', p.id);
      dirty = false; leaving = null;
      toast('Produit supprimé.');
      go('#/produits');
    } catch (e) { fail(e); }
  });
};

// --- Prestations ------------------------------------------------------------
screens.prestations = async () => {
  const rows = await api.list('prestations');
  await listPage({
    active: 'prestations', title: 'Prestations', table: 'prestations', rows,
    newHref: '#/prestations/nouveau', newLabel: 'Nouvelle prestation',
    emptyText: 'Aucune prestation.',
    editHref: (id) => `#/prestations/${id}`,
    rowHtml: (p) => `${thumb(p.photo)}<a class="row-main"><strong>${esc(p.titre)}${draft(p)}</strong><span>${esc(p.resume)}</span></a>`,
  });
};

screens.prestation = async (id) => {
  const rows = await api.list('prestations');
  const p = id === 'nouveau' ? { titre: '', resume: '', texte: '', photo: null, publie: true } : rows.find((x) => x.id === id);
  if (!p) return notFound();
  await shell('prestations', `<p class="crumb"><a href="#/prestations">Prestations</a></p>
  <h1>${p.id ? 'Modifier la prestation' : 'Nouvelle prestation'}</h1>
  <form class="editor" id="f" novalidate>
    <fieldset><legend>Texte</legend>
      <div class="field"><label for="titre">Titre <span class="req">*</span></label><input type="text" id="titre" name="titre" value="${esc(p.titre)}" maxlength="60"></div>
      <div class="field"><label for="resume">Phrase d’accroche <span class="req">*</span></label><input type="text" id="resume" name="resume" value="${esc(p.resume)}" maxlength="160"><p class="hint">Affichée sur les cartes de l’accueil.</p></div>
      <div class="field"><label for="texte">Texte</label><textarea id="texte" name="texte">${esc(p.texte)}</textarea></div>
    </fieldset>
    <fieldset><legend>Photo</legend><div id="m-photo"></div></fieldset>
    <fieldset><legend>Publication</legend><label class="check"><input type="checkbox" name="publie" ${p.publie ? 'checked' : ''}><span>Visible sur le site</span></label></fieldset>
  </form>`, {
    savebar: `<div class="left">${p.id ? '<button class="btn btn-danger" type="button" id="del">Supprimer</button>' : ''}</div><div class="right"><button class="btn" type="button" id="save">Enregistrer</button></div>`,
  });
  const f = $('#f');
  trackDirty(f);
  const photo = new MediaList($('#m-photo'), { items: p.photo ? [p.photo] : [], max: 1, folder: 'prestations', altBase: () => $('#titre')?.value.trim() || 'Prestation', hint: 'Facultatif.' });
  const files = fileWatch([photo]);
  $('#save').addEventListener('click', async () => {
    if (files.busy()) return toast('Attendez la fin de l’envoi de la photo.', true);
    const v = {
      ...(p.id ? { id: p.id } : {}),
      titre: $('#titre').value.trim(), resume: $('#resume').value.trim(), texte: $('#texte').value.trim(),
      photo: photo.value()[0] || null, publie: $('input[name=publie]').checked,
    };
    const errors = [];
    if (!v.titre) errors.push(['titre', 'Indiquez un titre.']);
    if (!v.resume) errors.push(['resume', 'Indiquez une phrase d’accroche.']);
    if (showErrors(f, errors)) return;
    if (!p.id) { v.slug = uniqueSlug(v.titre, rows); v.ordre = rows.length; }
    const btn = $('#save');
    busy(btn, true, 'Enregistrement…');
    try {
      const saved = await api.save('prestations', v);
      await files.saved();
      dirty = false;
      toast('Prestation enregistrée. Pensez à publier le site.');
      go(`#/prestations/${saved.id}`);
    } catch (e) { busy(btn, false); fail(e); }
  });
  $('#del')?.addEventListener('click', async () => {
    const used = (await api.list('realisations')).filter((r) => r.type === p.slug);
    if (used.length) return toast(`Impossible : ${used.length} réalisation(s) utilisent cette prestation. Changez d’abord leur type, ou décochez « Visible sur le site ».`, true);
    if (!confirm(`Supprimer « ${p.titre} » ?`)) return;
    try { await api.remove('prestations', p.id); dirty = false; leaving = null; toast('Prestation supprimée.'); go('#/prestations'); } catch (e) { fail(e); }
  });
};

// --- Avis -------------------------------------------------------------------
screens.avis = async () => {
  const rows = await api.list('avis');
  await listPage({
    active: 'avis', title: 'Avis clients', table: 'avis', rows,
    newHref: '#/avis/nouveau', newLabel: 'Nouvel avis',
    emptyText: 'Aucun avis. La page « Avis clients » apparaît sur le site dès le premier avis publié. Publiez uniquement des avis dont le client a donné son accord.',
    editHref: (id) => `#/avis/${id}`,
    rowHtml: (a) => `<a class="row-main"><strong>${esc(a.nom)}${a.ville ? ' · ' + esc(a.ville) : ''}${draft(a)}</strong><span>${esc(a.texte.slice(0, 90))}${a.texte.length > 90 ? '…' : ''}</span></a>`,
  });
};

screens.unAvis = async (id) => {
  const rows = await api.list('avis');
  const a = id === 'nouveau' ? { nom: '', ville: '', texte: '', date_avis: null, publie: true } : rows.find((x) => x.id === id);
  if (!a) return notFound();
  await shell('avis', `<p class="crumb"><a href="#/avis">Avis clients</a></p>
  <h1>${a.id ? 'Modifier l’avis' : 'Nouvel avis'}</h1>
  <form class="editor" id="f" novalidate>
    <fieldset><legend>Avis</legend>
      <div class="grid2">
        <div class="field"><label for="nom">Nom affiché <span class="req">*</span></label><input type="text" id="nom" name="nom" value="${esc(a.nom)}" placeholder="Ex. Sophie M. ou S. M."></div>
        <div class="field"><label for="ville">Ville</label><input type="text" id="ville" name="ville" value="${esc(a.ville || '')}"></div>
        <div class="field"><label for="date">Date</label><input type="date" id="date" name="date" value="${esc(a.date_avis || '')}"></div>
      </div>
      <div class="field"><label for="texte">Texte de l’avis <span class="req">*</span></label><textarea id="texte" name="texte">${esc(a.texte)}</textarea></div>
      <p class="hint">Publiez uniquement un avis réel, avec l’accord du client.</p>
    </fieldset>
    <fieldset><legend>Publication</legend><label class="check"><input type="checkbox" name="publie" ${a.publie ? 'checked' : ''}><span>Visible sur le site</span></label></fieldset>
  </form>`, {
    savebar: `<div class="left">${a.id ? '<button class="btn btn-danger" type="button" id="del">Supprimer</button>' : ''}</div><div class="right"><button class="btn" type="button" id="save">Enregistrer</button></div>`,
  });
  const f = $('#f');
  trackDirty(f);
  $('#save').addEventListener('click', async () => {
    const v = { ...(a.id ? { id: a.id } : {}), nom: $('#nom').value.trim(), ville: $('#ville').value.trim() || null, date_avis: $('#date').value || null, texte: $('#texte').value.trim(), publie: $('input[name=publie]').checked };
    const errors = [];
    if (!v.nom) errors.push(['nom', 'Indiquez le nom affiché.']);
    if (!v.texte) errors.push(['texte', 'Saisissez le texte de l’avis.']);
    if (showErrors(f, errors)) return;
    const btn = $('#save');
    busy(btn, true, 'Enregistrement…');
    try { const s = await api.save('avis', v); dirty = false; toast('Avis enregistré. Pensez à publier le site.'); go(`#/avis/${s.id}`); } catch (e) { busy(btn, false); fail(e); }
  });
  $('#del')?.addEventListener('click', async () => {
    if (!confirm('Supprimer cet avis ?')) return;
    try { await api.remove('avis', a.id); dirty = false; toast('Avis supprimé.'); go('#/avis'); } catch (e) { fail(e); }
  });
};

// --- Demandes (dossiers prospects) -------------------------------------------
screens.demandes = async () => {
  const rows = await api.list('prospects');
  const filtre = new URLSearchParams(location.hash.split('?')[1] || '').get('statut') || '';
  const shown = filtre ? rows.filter((p) => p.statut === filtre) : rows;
  await shell('demandes', `<div class="head"><h1>Demandes</h1>
    <select class="status-select" id="filtre" aria-label="Filtrer par statut"><option value="">Tous les statuts (${rows.length})</option>${Object.entries(STATUTS).map(([k, v]) => `<option value="${k}"${k === filtre ? ' selected' : ''}>${v} (${rows.filter((p) => p.statut === k).length})</option>`).join('')}</select></div>
  ${shown.length ? `<div class="list">${shown.map((p) => `<a class="row" href="#/demandes/${p.id}" style="text-decoration:none;color:inherit"><div class="row-main"><strong>${esc(p.nom)} ${p.statut === 'nouveau' ? '<span class="badge badge-new">Nouveau</span>' : `<span class="badge badge-ex">${STATUTS[p.statut]}</span>`}</strong><span>${esc(p.ville || '')}${p.ville ? ' · ' : ''}${fmtDate(p.created_at)} · ${esc(p.telephone)}</span></div><span aria-hidden="true">›</span></a>`).join('')}</div>`
    : `<div class="empty"><p>${rows.length ? 'Aucune demande avec ce statut.' : 'Aucune demande pour l’instant. Les demandes envoyées depuis le simulateur du site arriveront ici, avec les photos et les rendus.'}</p></div>`}
  <p class="hint" style="margin-top:14px">Les dossiers sans suite sont supprimés automatiquement 3 ans après le dernier contact (RGPD).</p>`);
  $('#filtre').addEventListener('change', (e) => go('#/demandes' + (e.target.value ? '?statut=' + e.target.value : '')));
};

screens.demande = async (id) => {
  const p = await api.get('prospects', id);
  if (!p) return notFound();
  const demandes = (await api.list('demandes').catch(() => [])).filter((d) => d.prospect_id === id);
  await shell('demandes', `<p class="crumb"><a href="#/demandes">Demandes</a></p>
  <h1>${esc(p.nom)}</h1>
  <div class="stack">
    <div class="panel"><dl class="kv">
      <dt>Téléphone</dt><dd><a href="tel:${esc(p.telephone)}">${esc(p.telephone)}</a></dd>
      <dt>E-mail</dt><dd><a href="mailto:${esc(p.email)}">${esc(p.email)}</a></dd>
      <dt>Ville</dt><dd>${esc(p.ville || '')}</dd>
      <dt>Première demande</dt><dd>${fmtDateTime(p.created_at)}</dd>
    </dl></div>
    <form class="panel" id="f">
      <div class="field"><label for="statut">Statut</label><select id="statut" class="status-select">${Object.entries(STATUTS).map(([k, v]) => `<option value="${k}"${k === p.statut ? ' selected' : ''}>${v}</option>`).join('')}</select></div>
      <div class="field"><label for="notes">Notes</label><textarea id="notes">${esc(p.notes)}</textarea></div>
      <button class="btn" type="submit">Enregistrer</button>
    </form>
    ${demandes.map((d) => `<div class="panel"><h2>Demande du ${fmtDate(d.created_at)}</h2><dl class="kv">
      <dt>Surface</dt><dd>${esc(d.type_surface || '')}${d.surface_m2 ? ' · ' + esc(d.surface_m2) + ' m²' : ''}</dd>
      <dt>Délai</dt><dd>${esc(d.delai || '')}</dd><dt>Message</dt><dd>${esc(d.message || '')}</dd></dl>
      <div class="photos" style="margin-top:12px" data-photos='${esc(JSON.stringify(d.photos || []))}'></div></div>`).join('')}
    <div style="display:flex;gap:10px;flex-wrap:wrap">
      <button class="btn btn-ghost" id="export" type="button">Exporter (RGPD)</button>
      <button class="btn btn-danger" id="del" type="button">Supprimer le dossier</button>
    </div>
  </div>`);
  for (const box of $$('[data-photos]')) {
    const list = JSON.parse(box.dataset.photos);
    for (const ph of list) {
      for (const k of ['original', 'rendu']) {
        if (!ph[k]) continue;
        const u = await api.privateUrl(ph[k]).catch(() => '');
        const lbl = k === 'rendu' ? `Rendu : ${ph.produit_nom || 'granulat'}` : 'Photo d’origine';
        box.insertAdjacentHTML('beforeend', `<a class="photo" href="${esc(u)}" target="_blank" rel="noopener" download><img src="${esc(u)}" alt="${esc(lbl)}"><span class="hint" style="display:block;padding:6px 8px">${esc(lbl)}</span></a>`);
      }
    }
  }
  $('#f').addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await api.save('prospects', { id: p.id, statut: $('#statut').value, notes: $('#notes').value, dernier_contact: new Date().toISOString() });
      toast('Dossier enregistré.');
    } catch (err) { fail(err); }
  });
  $('#export').addEventListener('click', () => {
    const blob = new Blob([JSON.stringify({ prospect: p, demandes, note: 'Les photos se téléchargent depuis la fiche (toucher une photo).' }, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `dossier-${slugify(p.nom)}.json`;
    a.click();
  });
  $('#del').addEventListener('click', async () => {
    if (!confirm(`Supprimer définitivement le dossier de ${p.nom}, ses demandes et ses photos ?`)) return;
    try {
      // Photos d'abord (stockage privé), puis la fiche et ses demandes
      await api.removePrivateFiles(demandes.flatMap((d) => (d.photos || []).flatMap((ph) => [ph.original, ph.rendu])).filter(Boolean));
      await api.remove('prospects', p.id);
      toast('Dossier et photos supprimés.');
      go('#/demandes');
    } catch (e) { fail(e); }
  });
};

// --- Réglages ---------------------------------------------------------------
screens.reglages = async () => {
  const s = await api.getSettings();
  await shell('reglages', `<h1>Réglages</h1>
  <form class="editor" id="f" novalidate>
    <fieldset><legend>Coordonnées</legend>
      <div class="grid2">
        <div class="field"><label for="tel">Téléphone <span class="req">*</span></label><input type="tel" id="tel" name="tel" value="${esc(s.telephone)}"></div>
        <div class="field"><label for="email">E-mail <span class="req">*</span></label><input type="email" id="email" name="email" value="${esc(s.email)}"></div>
      </div>
      <div class="field"><label for="wa">Lien WhatsApp</label><input type="url" id="wa" name="wa" value="${esc(s.whatsapp)}"><p class="hint">Format : https://wa.me/33612345678</p></div>
    </fieldset>
    <fieldset><legend>Zone d’intervention</legend>
      <div class="field"><label for="zone">Villes (une par ligne) <span class="req">*</span></label><textarea id="zone" name="zone" style="min-height:140px">${esc((s.zone || []).join('\n'))}</textarea></div>
      <div class="field"><label for="zt">Phrase de zone</label><input type="text" id="zt" name="zt" value="${esc(s.zoneTexte)}"><p class="hint">Affichée en pied de page. Ex. « Somain, Douai, Valenciennes et leurs alentours ».</p></div>
    </fieldset>
    <fieldset><legend>Page d’accueil</legend>
      <div class="grid2">
        <div class="field"><label for="a1">Accroche, ligne 1</label><input type="text" id="a1" name="a1" value="${esc(s.accroche?.[0])}"></div>
        <div class="field"><label for="a2">Accroche, ligne 2</label><input type="text" id="a2" name="a2" value="${esc(s.accroche?.[1])}"></div>
      </div>
      <div class="field"><span class="label">Grande image d’accueil</span><div id="m-accueil"></div></div>
    </fieldset>
    <fieldset><legend>Logo</legend><div id="m-logo"></div></fieldset>
  </form>`, { savebar: '<div></div><div class="right"><button class="btn" type="button" id="save">Enregistrer</button></div>' });
  const f = $('#f');
  trackDirty(f);
  const accueil = new MediaList($('#m-accueil'), { items: s.accueil ? [s.accueil] : [], max: 1, folder: 'site', warnSmall: true, altBase: () => 'Revêtement en moquette de pierre réalisé par Nataliia Résine', hint: 'Laissez vide pour garder l’image actuelle. Format paysage, 1920 px de large minimum.' });
  const logo = new MediaList($('#m-logo'), { items: s.logo ? [s.logo] : [], max: 1, folder: 'site', maxW: 1280, altBase: () => 'Logo Nataliia Résine', hint: 'Laissez vide pour garder le logo actuel.' });
  const files = fileWatch([accueil, logo]);
  $('#save').addEventListener('click', async () => {
    if (files.busy()) return toast('Attendez la fin de l’envoi des photos.', true);
    const v = {
      ...s,
      telephone: $('#tel').value.trim(), email: $('#email').value.trim(), whatsapp: $('#wa').value.trim(),
      zone: $('#zone').value.split('\n').map((x) => x.trim()).filter(Boolean), zoneTexte: $('#zt').value.trim(),
      accroche: [$('#a1').value.trim(), $('#a2').value.trim()],
      accueil: accueil.value()[0] || null, logo: logo.value()[0] || null,
    };
    const errors = [];
    if (!/^[\d .+]{10,}$/.test(v.telephone)) errors.push(['tel', 'Numéro de téléphone invalide.']);
    if (!/^\S+@\S+\.\S+$/.test(v.email)) errors.push(['email', 'Adresse e-mail invalide.']);
    if (v.whatsapp && !/^https:\/\/wa\.me\/\d+$/.test(v.whatsapp)) errors.push(['wa', 'Le lien WhatsApp doit être de la forme https://wa.me/33612345678']);
    if (!v.zone.length) errors.push(['zone', 'Indiquez au moins une ville.']);
    if (showErrors(f, errors)) return;
    const btn = $('#save');
    busy(btn, true, 'Enregistrement…');
    try { await api.saveSettings(v); await files.saved(); dirty = false; toast('Réglages enregistrés. Pensez à publier le site.'); route(); } catch (e) { busy(btn, false); fail(e); }
  });
};

// --- Historique -------------------------------------------------------------
const ACTIONS = { INSERT: 'Création', UPDATE: 'Modification', DELETE: 'Suppression' };
screens.historique = async () => {
  const rows = (await api.history(150)).filter((h) => TABLES[h.table_name]);
  await shell('historique', `<h1>Historique</h1>
  <p class="muted">Les 150 dernières modifications. « Revenir à cette version » remet l’élément dans l’état où il était juste avant la modification.</p>
  ${rows.length ? `<div class="hist">${rows.map((h, i) => {
    const d = h.apres || h.avant || {};
    const name = d.titre || d.nom || (h.table_name === 'reglages' ? 'Réglages du site' : '');
    return `<div class="row"><div class="row-main"><strong>${ACTIONS[h.action]} · ${TABLES[h.table_name]} « ${esc(name)} »</strong><span>${fmtDateTime(h.at)} · ${esc(h.auteur || '')}</span></div>
      ${h.action === 'INSERT' ? `<button class="btn btn-sm btn-ghost" data-i="${i}">Annuler la création</button>` : `<button class="btn btn-sm btn-ghost" data-i="${i}">Revenir à cette version</button>`}</div>`;
  }).join('')}</div>` : '<div class="empty"><p>Aucune modification enregistrée.</p></div>'}`);
  $$('[data-i]').forEach((b) => b.addEventListener('click', async () => {
    const h = rows[+b.dataset.i];
    if (!confirm(h.action === 'INSERT' ? 'Supprimer l’élément créé ?' : 'Remettre l’élément dans son état précédent ?')) return;
    try { await api.restore(h); toast('C’est fait. Pensez à publier le site.'); route(); } catch (e) { fail(e); }
  }));
};

// --- Compte -----------------------------------------------------------------
screens.compte = async () => {
  await shell('compte', `<h1>Mon compte</h1>
  <div class="stack">
    <div class="panel"><p>Connectée en tant que <strong>${esc(user?.email)}</strong></p><button class="btn btn-ghost" id="out" type="button">Se déconnecter</button></div>
    <form class="panel" id="pw" novalidate><h2>Changer de mot de passe</h2>
      <div class="field"><label for="p1">Nouveau mot de passe</label><input type="password" id="p1" name="p1" autocomplete="new-password" minlength="10"><p class="hint">10 caractères minimum.</p></div>
      <div class="field"><label for="p2">Confirmer</label><input type="password" id="p2" name="p2" autocomplete="new-password"></div>
      <button class="btn" type="submit">Changer le mot de passe</button>
    </form>
    <div class="panel"><h2>Autres utilisateurs</h2><p class="muted">Pour donner accès à une autre personne, demandez à Apexus de lui envoyer une invitation.</p></div>
    <p class="small"><a href="aide.html" target="_blank" rel="noopener">Guide d’utilisation</a></p>
  </div>`);
  $('#out').addEventListener('click', async () => { await api.logout(); });
  $('#pw').addEventListener('submit', async (e) => {
    e.preventDefault();
    const p1 = $('#p1').value; const p2 = $('#p2').value;
    if (showErrors($('#pw'), [...(p1.length < 10 ? [['p1', 'Le mot de passe doit faire au moins 10 caractères.']] : []), ...(p1 !== p2 ? [['p2', 'Les deux mots de passe sont différents.']] : [])])) return;
    try { await api.updatePassword(p1); toast('Mot de passe changé.'); e.target.reset(); } catch (err) { fail(err); }
  });
};

function notFound() {
  return shell('', '<div class="empty"><p>Élément introuvable. Il a peut-être été supprimé.</p><a class="btn" href="#/">Retour à l’accueil</a></div>');
}

// ---------------------------------------------------------------------------
// Connexion
function loginScreen(msg = '') {
  app.innerHTML = `${api.mode === 'demo' ? '<div class="demo-bar">Mode démonstration : saisissez n’importe quel e-mail et mot de passe.</div>' : ''}
  <div class="login">
    <div class="login-brand"><img src="../img/logo-640.jpg" alt="Nataliia Résine"><p class="muted">Administration du site</p></div>
    <form class="panel" id="login" novalidate>
      ${msg ? `<div class="warn-box" style="margin-bottom:14px">${esc(msg)}</div>` : ''}
      <div class="field"><label for="email">E-mail</label><input type="email" id="email" name="email" autocomplete="username" required value="${api.mode === 'demo' ? 'demo@nataliia-resine.fr' : ''}"></div>
      <div class="field"><label for="pass">Mot de passe</label><input type="password" id="pass" name="pass" autocomplete="current-password" required value="${api.mode === 'demo' ? 'demonstration' : ''}"></div>
      <button class="btn" type="submit" style="width:100%">Se connecter</button>
      <p style="text-align:center;margin:14px 0 0"><button type="button" id="forgot" class="btn btn-sm btn-ghost" style="border:0">Mot de passe oublié ?</button></p>
    </form>
  </div>`;
  $('#login').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = $('button[type=submit]', e.target);
    busy(btn, true, 'Connexion…');
    try { await api.login($('#email').value.trim(), $('#pass').value); await start(); } catch (err) { busy(btn, false); fail(err); }
  });
  $('#forgot').addEventListener('click', async () => {
    const email = $('#email').value.trim();
    if (!/^\S+@\S+\.\S+$/.test(email)) return toast('Saisissez d’abord votre e-mail ci-dessus.', true);
    try { await api.resetPassword(email); toast('Si ce compte existe, un e-mail vient de partir avec un lien pour choisir un nouveau mot de passe.'); } catch (err) { fail(err); }
  });
}

function newPasswordScreen() {
  app.innerHTML = `<div class="login"><div class="login-brand"><img src="../img/logo-640.jpg" alt="Nataliia Résine"></div>
  <form class="panel" id="np" novalidate><h2>Choisissez un nouveau mot de passe</h2>
    <div class="field"><label for="p1">Nouveau mot de passe</label><input type="password" id="p1" name="p1" autocomplete="new-password"><p class="hint">10 caractères minimum.</p></div>
    <button class="btn" type="submit" style="width:100%">Enregistrer</button></form></div>`;
  $('#np').addEventListener('submit', async (e) => {
    e.preventDefault();
    if ($('#p1').value.length < 10) return toast('10 caractères minimum.', true);
    try { await api.updatePassword($('#p1').value); toast('Mot de passe enregistré.'); history.replaceState(null, '', location.pathname); await start(); } catch (err) { fail(err); }
  });
}

// ---------------------------------------------------------------------------
// Routage
const ROUTES = [
  [/^#?\/?$/, () => screens.accueil()],
  [/^#\/realisations$/, () => screens.realisations()],
  [/^#\/realisations\/(.+)$/, (m) => screens.realisation(m[1])],
  [/^#\/produits$/, () => screens.produits()],
  [/^#\/produits\/(.+)$/, (m) => screens.produit(m[1])],
  [/^#\/prestations$/, () => screens.prestations()],
  [/^#\/prestations\/(.+)$/, (m) => screens.prestation(m[1])],
  [/^#\/avis$/, () => screens.avis()],
  [/^#\/avis\/(.+)$/, (m) => screens.unAvis(m[1])],
  [/^#\/demandes(\?.*)?$/, () => screens.demandes()],
  [/^#\/demandes\/([^?]+)$/, (m) => screens.demande(m[1])],
  [/^#\/reglages$/, () => screens.reglages()],
  [/^#\/historique$/, () => screens.historique()],
  [/^#\/compte$/, () => screens.compte()],
];

let currentHash = location.hash;
async function route() {
  if (!user) return;
  const h = location.hash || '#/';
  for (const [re, fn] of ROUTES) {
    const m = h.match(re);
    if (m) {
      try { await fn(m); } catch (e) { fail(e); }
      return;
    }
  }
  notFound();
}

window.addEventListener('hashchange', () => {
  if (location.hash === currentHash) return;
  if (!confirmLeave()) { history.replaceState(null, '', currentHash || '#/'); return; }
  if (leaving) { leaving(); leaving = null; }
  dirty = false;
  currentHash = location.hash;
  route();
});

async function start() {
  user = await api.session();
  if (!user) return loginScreen();
  let ok = false;
  try { ok = await api.isAdmin(); } catch (e) { fail(e); }
  if (!ok) {
    await api.logout();
    user = null;
    return loginScreen('Ce compte n’a pas accès à l’administration.');
  }
  currentHash = location.hash;
  route();
}

(async () => {
  try {
    api = await createApi();
  } catch (e) {
    app.innerHTML = `<p class="boot">Impossible de démarrer l’administration : ${esc(e.message)}</p>`;
    return;
  }
  // Lien reçu par e-mail : invitation (premier mot de passe) ou mot de passe oublié
  const fromEmail = /type=(recovery|invite)/.test(location.hash);
  let shown = false;
  const askPassword = () => { if (!shown) { shown = true; newPasswordScreen(); } };
  api.onAuth((event, s) => {
    if (event === 'PASSWORD_RECOVERY' || (fromEmail && event === 'SIGNED_IN')) { if (s) user = s; askPassword(); return; }
    if (event === 'SIGNED_OUT') { user = null; dirty = false; loginScreen(); }
    if (event === 'SIGNED_IN' && s) user = s;
  });
  if (fromEmail) {
    // La session issue du lien peut déjà être prête avant l'abonnement aux événements
    if (await api.session()) askPassword();
    return;
  }
  start();
})();
