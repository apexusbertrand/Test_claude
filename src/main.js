import './sw-register.js';
import { getAllPhotos, clearAll } from './db.js';
import {
  capabilities,
  restoreLibrarySilently,
  reconnectSavedHandles,
  hasSavedFsaHandles,
  pickSourceFolder,
  pickMiniaturesRoot,
  isLibraryReady,
  hasSource,
  hasMiniaturesRoot,
  isUsingCustomMiniaturesLocation,
  importFilesFallback,
  getRootHandle,
  walkImages,
  loadAllSidecars,
} from './storage.js';
import {
  processBatch,
  rehydrateFromSidecar,
  repairThumbnail,
  propagateAndPersist,
  persistPhoto,
  loadBrowseOnlyPhotos,
} from './tagging.js';
import { sharePhoto } from './share.js';
import { onAiStatus, getAiStatusMessages, reportAiStatus } from './ai-status.js';

const els = {
  reconnectBtn: document.getElementById('btn-reconnect'),
  pickFolderBtn: document.getElementById('btn-pick-folder'),
  pickMiniaturesRootBtn: document.getElementById('btn-pick-miniatures-root'),
  importLabel: document.getElementById('btn-import-fallback-label'),
  importInput: document.getElementById('input-import-fallback'),
  scanBtn: document.getElementById('btn-scan'),
  progress: document.getElementById('progress'),
  progressFill: document.getElementById('progress-fill'),
  progressLabel: document.getElementById('progress-label'),
  statusLog: document.getElementById('status-log'),
  statusLogItems: document.getElementById('status-log-items'),
  statusLogClose: document.getElementById('status-log-close'),
  searchInput: document.getElementById('search-input'),
  categoryFilters: document.getElementById('category-filters'),
  gallery: document.getElementById('gallery'),
  emptyState: document.getElementById('empty-state'),
  modal: document.getElementById('modal'),
  modalBackdrop: document.getElementById('modal-backdrop'),
  modalClose: document.getElementById('modal-close'),
  modalImg: document.getElementById('modal-img'),
  modalName: document.getElementById('modal-name'),
  modalMeta: document.getElementById('modal-meta'),
  modalTags: document.getElementById('modal-tags'),
  tagAddForm: document.getElementById('tag-add-form'),
  tagCategory: document.getElementById('tag-category'),
  tagValue: document.getElementById('tag-value'),
  openOriginalBtn: document.getElementById('btn-open-original'),
  shareBtn: document.getElementById('btn-share'),
};

const CATEGORIES = [
  { key: 'lieu', label: 'Lieu' },
  { key: 'personne', label: 'Personne' },
  { key: 'animal', label: 'Animal' },
  { key: 'evenement', label: 'Événement' },
  { key: 'autre', label: 'Autre' },
];

const state = {
  photos: [],
  thumbUrls: new Map(), // photo.id -> object URL
  activeCategories: new Set(),
  searchText: '',
  currentPhotoId: null,
};

function setProgress(visible, fraction = 0, label = '') {
  els.progress.hidden = !visible;
  els.progressFill.style.width = `${Math.round(fraction * 100)}%`;
  els.progressLabel.textContent = label;
}

/**
 * On-screen diagnostics. A phone user has no devtools console, so anything
 * that would otherwise only be a console.warn — an AI model that failed to
 * load, a photo that couldn't be decoded — has to be visible here instead.
 */
function appendStatusMessage({ message, at }) {
  const item = document.createElement('div');
  item.className = 'status-log-item';
  item.textContent = `${at.toLocaleTimeString('fr-FR')} — ${message}`;
  els.statusLogItems.appendChild(item);
  els.statusLog.hidden = false;
}

async function loadPhotosFromDb() {
  state.photos = await getAllPhotos();
  await hydrateThumbUrls(state.photos);
  buildCategoryChips();
  render();
}

/**
 * Show whatever is already tagged in the miniatures location — fast, read-only,
 * no folder walk, no AI. Picking/reconnecting a folder must never itself kick
 * off an analysis; only the explicit "Analyser le dossier" button does that.
 */
async function showExistingMiniatures() {
  if (!hasMiniaturesRoot()) return;
  await loadBrowseOnlyPhotos();
  await loadPhotosFromDb();
}

async function hydrateThumbUrls(photos) {
  for (const p of photos) {
    if (state.thumbUrls.has(p.id) || !p.thumbHandle) continue;
    try {
      const file = await p.thumbHandle.getFile();
      state.thumbUrls.set(p.id, URL.createObjectURL(file));
    } catch {
      // thumbnail missing/unreadable; card will show a placeholder
    }
  }
}

function buildCategoryChips() {
  els.categoryFilters.innerHTML = '';
  for (const cat of CATEGORIES) {
    const btn = document.createElement('button');
    btn.className = 'chip' + (state.activeCategories.has(cat.key) ? ' active' : '');
    btn.textContent = cat.label;
    btn.addEventListener('click', () => {
      if (state.activeCategories.has(cat.key)) state.activeCategories.delete(cat.key);
      else state.activeCategories.add(cat.key);
      buildCategoryChips();
      render();
    });
    els.categoryFilters.appendChild(btn);
  }
}

function matchesFilters(photo) {
  const text = state.searchText.trim().toLowerCase();
  const tags = photo.tags || [];
  if (state.activeCategories.size > 0) {
    const cats = new Set(tags.map((t) => t.category));
    const anyMatch = [...state.activeCategories].some((c) => cats.has(c));
    if (!anyMatch) return false;
  }
  if (!text) return true;
  return tags.some((t) => t.value.toLowerCase().includes(text)) || photo.name.toLowerCase().includes(text);
}

function render() {
  const visible = state.photos.filter(matchesFilters);
  els.gallery.hidden = state.photos.length === 0;
  els.emptyState.hidden = state.photos.length !== 0;
  els.gallery.innerHTML = '';
  for (const photo of visible) {
    els.gallery.appendChild(renderCard(photo));
  }
}

function renderCard(photo) {
  const card = document.createElement('div');
  card.className = 'card';
  const thumbUrl = state.thumbUrls.get(photo.id);
  if (thumbUrl) {
    const img = document.createElement('img');
    img.src = thumbUrl;
    img.alt = photo.name;
    img.loading = 'lazy';
    card.appendChild(img);
  } else {
    const placeholder = document.createElement('div');
    placeholder.className = 'card-placeholder';
    placeholder.innerHTML = `<span>⚠ Miniature indisponible</span><span class="card-placeholder-name">${escapeHtml(photo.name)}</span>`;
    card.appendChild(placeholder);
  }

  const tagsWrap = document.createElement('div');
  tagsWrap.className = 'card-tags';
  for (const t of (photo.tags || []).slice(0, 4)) {
    const chip = document.createElement('span');
    chip.className = 'tag-chip';
    chip.textContent = t.value;
    tagsWrap.appendChild(chip);
  }
  card.appendChild(tagsWrap);

  card.addEventListener('click', () => openModal(photo.id));
  return card;
}

async function openModal(photoId) {
  state.currentPhotoId = photoId;
  const photo = state.photos.find((p) => p.id === photoId);
  if (!photo) return;
  els.modalImg.src = state.thumbUrls.get(photo.id) || '';
  els.modalName.textContent = photo.name;
  const date = photo.takenAt ? new Date(photo.takenAt).toLocaleString('fr-FR') : 'Date inconnue';
  els.modalMeta.textContent = `${date} · ${photo.eventLabel || ''}`;
  renderTagList(photo);
  els.modal.hidden = false;
}

function renderTagList(photo) {
  els.modalTags.innerHTML = '';
  (photo.tags || []).forEach((t, idx) => {
    const pill = document.createElement('span');
    pill.className = 'tag-pill';
    pill.innerHTML = `<span class="cat">${t.category}</span> ${escapeHtml(t.value)}`;
    const removeBtn = document.createElement('button');
    removeBtn.textContent = '✕';
    removeBtn.title = 'Retirer ce tag';
    removeBtn.addEventListener('click', () => removeTag(photo.id, idx));
    pill.appendChild(removeBtn);
    els.modalTags.appendChild(pill);
  });
}

function escapeHtml(s) {
  return s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

async function removeTag(photoId, idx) {
  const photo = state.photos.find((p) => p.id === photoId);
  photo.tags.splice(idx, 1);
  await persistPhoto(photo);
  renderTagList(photo);
  render();
}

async function addTag(photoId, category, value) {
  const trimmed = value.trim();
  if (!trimmed) return;
  const photo = state.photos.find((p) => p.id === photoId);
  const exists = photo.tags.some((t) => t.category === category && t.value.toLowerCase() === trimmed.toLowerCase());
  if (!exists) photo.tags.push({ category, value: trimmed });
  await persistPhoto(photo);
  renderTagList(photo);
  render();
}

function closeModal() {
  els.modal.hidden = true;
  state.currentPhotoId = null;
}

async function openOriginal(photo) {
  if (!photo.fileHandle) {
    alert("L'original n'est pas disponible sur cet appareil.");
    return;
  }
  try {
    const file = await photo.fileHandle.getFile();
    const url = URL.createObjectURL(file);
    window.open(url, '_blank');
    setTimeout(() => URL.revokeObjectURL(url), 30000);
  } catch {
    alert("Impossible d'ouvrir l'original : le fichier a peut-être été déplacé ou n'est pas accessible sur cet appareil.");
  }
}

async function scanLibrary() {
  const root = getRootHandle();
  if (!root) return;
  els.scanBtn.disabled = true;
  setProgress(true, 0, 'Recherche des photos…');

  try {
    await runScan(root);
    setProgress(true, 1, 'Terminé.');
    setTimeout(() => setProgress(false), 1200);
  } catch (err) {
    console.error('Échec du scan', err);
    setProgress(false);
    reportAiStatus(`L'analyse a été interrompue : ${err.message || err}`);
    alert(
      `L'analyse a été interrompue : ${err.message || err}\n\n` +
        "Si l'erreur mentionne une permission, essayez le bouton \"Utiliser un autre emplacement pour les miniatures\" — certains dossiers photo protégés par le système (ex. l'appareil photo sur Android) refusent la création de sous-dossiers."
    );
  } finally {
    els.scanBtn.disabled = false;
    await loadPhotosFromDb();
  }
}

async function runScan(root) {
  // The DB is just a fast cache; sidecar JSON files next to the thumbnails are
  // the durable record, so a photo already tagged before gets its tags (and a
  // fresh file handle) restored instead of being re-analyzed from scratch —
  // this also means tags survive a cleared browser profile. Photos already
  // showing in the gallery with a *live* file handle are skipped outright;
  // photos loaded read-only (browsed from the miniatures location before the
  // source folder was reachable) still get their handle re-attached.
  const knownWithHandle = new Set(state.photos.filter((p) => p.fileHandle).map((p) => p.relativePath));
  const sidecars = await loadAllSidecars();
  const newEntries = [];
  const toRehydrate = [];
  for await (const entry of walkImages(root)) {
    if (knownWithHandle.has(entry.relativePath)) continue;
    const sidecar = sidecars.get(entry.relativePath);
    if (sidecar) toRehydrate.push({ entry, sidecar });
    else newEntries.push(entry);
  }

  // A sidecar whose thumbnail never made it to disk (decode/write failure on a
  // previous scan) is retried here instead of staying broken forever.
  const toRepair = [];
  for (let i = 0; i < toRehydrate.length; i += 1) {
    const { entry, sidecar } = toRehydrate[i];
    const photo = await rehydrateFromSidecar(entry, sidecar);
    if (!photo.thumbHandle) toRepair.push({ entry, sidecar });
    setProgress(true, (i + 1) / toRehydrate.length, `Restauration des tags ${i + 1}/${toRehydrate.length}`);
  }

  for (let i = 0; i < toRepair.length; i += 1) {
    try {
      await repairThumbnail(toRepair[i].entry, toRepair[i].sidecar);
    } catch (err) {
      console.warn('Échec de la régénération de miniature pour', toRepair[i].entry.name, err);
    }
    setProgress(true, (i + 1) / toRepair.length, `Régénération de miniature ${i + 1}/${toRepair.length}`);
  }

  if (newEntries.length > 0) {
    await processBatch(newEntries, {
      onProgress: ({ phase, current, total }) => {
        if (phase === 'ai-check') {
          setProgress(true, 0.3, 'Vérification des modules IA…');
          return;
        }
        const phaseShare = phase === 'exif' ? 0.3 : 0.65;
        const phaseStart = phase === 'exif' ? 0 : 0.35;
        const frac = phaseStart + phaseShare * (current / total);
        const label = phase === 'exif' ? `Lecture des métadonnées ${current}/${total}` : `Analyse et tags ${current}/${total}`;
        setProgress(true, frac, label);
      },
    });
  }

  // Propagate every manually-named "personne" tag to matching faces across the
  // whole library — this is what makes naming someone once tag them everywhere.
  setProgress(true, 0.95, 'Reconnaissance des personnes nommées…');
  const allPhotos = await getAllPhotos();
  await propagateAndPersist(allPhotos);
}

function refreshPickerButtons() {
  if (!capabilities.fsAccess) return;
  if (hasSource() && hasMiniaturesRoot()) els.reconnectBtn.hidden = true;
  els.pickFolderBtn.textContent = hasSource() ? '✓ Dossier de photos choisi' : 'Choisir le dossier de photos';
  // Optional, always available once a source is picked: switch away from the
  // default photos/miniatures (inside the source folder) to an independent location.
  els.pickMiniaturesRootBtn.hidden = !hasSource();
  els.pickMiniaturesRootBtn.textContent = isUsingCustomMiniaturesLocation()
    ? "✓ Emplacement des miniatures personnalisé (optionnel)"
    : 'Utiliser un autre emplacement pour les miniatures (optionnel)';
}

async function completePickerStep() {
  refreshPickerButtons();
  if (isLibraryReady()) {
    els.scanBtn.disabled = false;
    await showExistingMiniatures();
  }
}

function wireEvents() {
  els.reconnectBtn.addEventListener('click', async () => {
    els.reconnectBtn.disabled = true;
    try {
      const ok = await reconnectSavedHandles();
      if (ok) {
        els.reconnectBtn.hidden = true;
        refreshPickerButtons();
        els.scanBtn.disabled = false;
        await showExistingMiniatures();
      } else {
        alert("Permission refusée, ou l'un des dossiers est introuvable. Utilisez les boutons ci-dessous pour re-choisir manuellement.");
      }
    } catch (err) {
      alert(err.message || String(err));
    } finally {
      els.reconnectBtn.disabled = false;
    }
  });

  els.pickFolderBtn.addEventListener('click', async () => {
    try {
      await pickSourceFolder();
      await completePickerStep();
    } catch (err) {
      if (err?.name !== 'AbortError') alert(err.message || String(err));
    }
  });

  els.pickMiniaturesRootBtn.addEventListener('click', async () => {
    try {
      await pickMiniaturesRoot();
      await completePickerStep();
    } catch (err) {
      if (err?.name !== 'AbortError') alert(err.message || String(err));
    }
  });

  els.importInput.addEventListener('change', async (e) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;
    setProgress(true, 0, 'Import des photos…');
    await importFilesFallback(files, (i, total) => setProgress(true, i / total, `Import ${i}/${total}`));
    setProgress(false);
    els.scanBtn.disabled = false;
    await showExistingMiniatures();
    e.target.value = '';
  });

  els.scanBtn.addEventListener('click', scanLibrary);

  els.statusLogClose.addEventListener('click', () => {
    els.statusLog.hidden = true;
  });

  els.searchInput.addEventListener('input', (e) => {
    state.searchText = e.target.value;
    render();
  });

  els.modalClose.addEventListener('click', closeModal);
  els.modalBackdrop.addEventListener('click', closeModal);
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !els.modal.hidden) closeModal();
  });

  els.tagAddForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!state.currentPhotoId) return;
    await addTag(state.currentPhotoId, els.tagCategory.value, els.tagValue.value);
    els.tagValue.value = '';
  });

  els.openOriginalBtn.addEventListener('click', () => {
    const photo = state.photos.find((p) => p.id === state.currentPhotoId);
    if (photo) openOriginal(photo);
  });

  els.shareBtn.addEventListener('click', async () => {
    const photo = state.photos.find((p) => p.id === state.currentPhotoId);
    if (!photo) return;
    try {
      await sharePhoto(photo);
    } catch (err) {
      alert(err.message || 'Partage impossible.');
    }
  });
}

async function init() {
  wireEvents();

  // Replay past diagnostics into the panel silently on load; alert only for
  // ones that happen live during this session, so a returning user isn't
  // greeted with a popup for something already known and logged.
  getAiStatusMessages().forEach(appendStatusMessage);
  onAiStatus((entry) => {
    appendStatusMessage(entry);
    alert(entry.message);
  });

  if (!capabilities.fsAccess) {
    els.pickFolderBtn.hidden = true;
    els.importLabel.hidden = false;
  }

  // Show whatever is already cached instantly — no permission needed for this.
  await loadPhotosFromDb();

  // Only ever *queries* permission state (never prompts) — safe with no user
  // gesture. Succeeds automatically when the browser kept the grant; otherwise
  // reconnecting requires an explicit click (see the "Reconnecter" button).
  const result = await restoreLibrarySilently();

  if (result.ready) {
    els.scanBtn.disabled = false;
    refreshPickerButtons();
    await showExistingMiniatures();
    return;
  }

  if (result.miniaturesOnly) {
    await loadBrowseOnlyPhotos();
    await loadPhotosFromDb();
  }

  if (capabilities.fsAccess && (await hasSavedFsaHandles())) {
    els.reconnectBtn.hidden = false;
  }
  refreshPickerButtons();
}

init();

// exposed for debugging / advanced use in the browser console
window.__phototag = { clearAll, state };
