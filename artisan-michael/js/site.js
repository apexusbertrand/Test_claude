/* LOGE&NERJ — script commun.
   Tout ce qui est coordonnées et URL de webhooks est dans CONFIG.
   Un champ vide (téléphone, horaires, SIRET…) n'est simplement pas affiché sur le site. */
const CONFIG = {
  nom: 'LOGE&NERJ',
  prenom: 'Michael',
  sousTitre: 'Rénover, isoler, économiser',   // signature du logo officiel
  metiers: 'Rénovation tous corps d’état et dépannage',
  telephone: '07 49 94 52 79',   // numéro public de la page Facebook (13/11/2026) ; vide = pas de téléphone affiché
  email: 'logenerj@artisanat1.fr',
  adresse: 'Somain (59490), Nord',   // ville d'après la page Facebook ; rue à ajouter quand on l'aura
  zone: 'Somain et alentours',
  horaires: '',          // à renseigner ; vide = non affiché
  siret: '',             // obligatoire (mentions légales) : à renseigner dès que possible
  facebook: 'https://www.facebook.com/p/Logenerj-61591852021732/',
  facebookAlbum: 'https://www.facebook.com/media/set/?set=a.122098578171395067&type=3',   // album photo de la page Facebook   // page Facebook « Loge&nerj » (adresse définitive, relevée le 12/11/2026)
  formWebhook: 'https://apexus.app.n8n.cloud/webhook/contact-logenerj',   // flux n8n « Site LOGE&NERJ — Formulaire de devis »
  chatWebhook: 'https://apexus.app.n8n.cloud/webhook/9692846f-a737-47ce-beaa-cba903f86a47/chat',   // flux n8n « Site LOGE&NERJ — Agent Chat »
  maquette: false
};
window.CONFIG = CONFIG;

const LOGO = `<img src="assets/brand/embleme-160.webp" alt="" class="logo" width="160" height="114">`;  // emblème du logo officiel (assets/brand/)
window.LOGO = LOGO;

const NAV = [
  ['index.html', 'Accueil'],
  ['savoir-faire.html', 'Savoir-faire'],
  ['realisations.html', 'Chantiers'],
  ['qui-suis-je.html', 'Qui suis-je'],
  ['contact.html', 'Contact']
];
const METIERS = { carrelage: 'Carrelage', platrerie: 'Placo & isolation', couverture: 'Toiture', menuiserie: 'Menuiserie ext.', peinture: 'Peinture', depannage: 'Dépannage', chauffage: 'Pompe à chaleur' };
const tel = () => 'tel:' + CONFIG.telephone.replace(/\s/g, '');
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

function layout() {
  const here = (location.pathname.split('/').pop() || 'index.html');
  const current = here === 'chantier.html' ? 'realisations.html' : here;
  const brand = `<a class="brand" href="index.html">${LOGO}<span><span class="brand-name">${esc(CONFIG.nom)}</span><span class="brand-sub">${esc(CONFIG.sousTitre)}</span></span></a>`;

  document.body.insertAdjacentHTML('afterbegin', `
    <div class="tape" aria-hidden="true"><div class="tape-fill"></div><span class="tape-count">000 cm</span></div>
    <header class="hdr">
      ${CONFIG.maquette ? '<div class="mock-flag">Maquette de présentation · téléphone, adresse et chantier n°2 fictifs</div>' : ''}
      <div class="wrap hdr-in">
        ${brand}
        <nav class="nav" id="nav" aria-label="Menu principal">
          ${NAV.map(([h, l]) => `<a href="${h}"${h === current ? ' aria-current="page"' : ''}>${l}</a>`).join('')}
        </nav>
        <a class="btn sun" href="contact.html">Devis gratuit <span class="arr">→</span></a>
        <button class="burger" aria-label="Ouvrir le menu" aria-expanded="false" aria-controls="nav"><span></span></button>
      </div>
    </header>`);

  document.body.insertAdjacentHTML('beforeend', `
    <div class="champs" aria-hidden="true"></div>
    <footer class="ftr">
      <div class="wrap">
        <div class="ftr-grid">
          <div><a href="index.html"><img class="ftr-logo" src="assets/brand/logo-transparent-900.webp" alt="LOGE&amp;NERJ, rénover, isoler, économiser" width="900" height="817" loading="lazy"></a><p>${esc(CONFIG.metiers)}. Artisan indépendant : je pose, j’isole, je répare, et je laisse le chantier propre.</p></div>
          <div><h4>Contact</h4><ul>
            ${CONFIG.telephone ? `<li><a href="${tel()}">${esc(CONFIG.telephone)}</a></li>` : ''}
            <li><a href="mailto:${esc(CONFIG.email)}">${esc(CONFIG.email)}</a></li>
            <li>${esc(CONFIG.adresse)}</li>${CONFIG.horaires ? `<li>${esc(CONFIG.horaires)}</li>` : ''}
            ${CONFIG.facebook ? `<li><a class="fb-link" href="${CONFIG.facebook}" target="_blank" rel="noopener"><svg viewBox="0 0 24 24" aria-hidden="true" class="fb-ico"><path fill="currentColor" d="M22 12a10 10 0 1 0-11.56 9.88v-6.99H7.9V12h2.54V9.8c0-2.5 1.49-3.89 3.78-3.89 1.09 0 2.24.2 2.24.2v2.46h-1.26c-1.24 0-1.63.77-1.63 1.56V12h2.78l-.44 2.89h-2.34v6.99A10 10 0 0 0 22 12Z"/></svg> Suivre mes chantiers sur Facebook</a></li>` : ''}</ul></div>
          <div><h4>Le site</h4><ul>${NAV.map(([h, l]) => `<li><a href="${h}">${l}</a></li>`).join('')}<li><a href="mentions-legales.html">Mentions légales</a></li>${CONFIG.maquette ? '<li><a href="charte.html">Charte graphique</a></li>' : ''}</ul></div>
        </div>
        <div class="ftr-legal">${CONFIG.siret ? `<span>SIRET ${esc(CONFIG.siret)}</span>` : ''}<span>${CONFIG.maquette ? 'Maquette · informations fictives' : '© ' + new Date().getFullYear() + ' ' + esc(CONFIG.nom)}</span><span class="ftr-credit">Site créé par <a href="https://apexus.fr" target="_blank" rel="noopener">Apexus</a></span></div>
      </div>
    </footer>
    <nav class="mbar" aria-label="Contact rapide">
      ${CONFIG.telephone ? `<a href="${tel()}">Appeler</a>` : `<a href="mailto:${esc(CONFIG.email)}">E-mail</a>`}
      <a href="contact.html">Devis gratuit</a>
    </nav>`);

  document.querySelectorAll('[data-tel]').forEach(a => { if (CONFIG.telephone) { a.href = tel(); a.textContent = CONFIG.telephone; } else (a.closest('[data-tel-block]') || a).remove(); });
  document.querySelectorAll('[data-tel-block]').forEach(el => { if (!CONFIG.telephone) el.remove(); });
  document.querySelectorAll('[data-email]').forEach(a => { a.href = 'mailto:' + CONFIG.email; a.textContent = CONFIG.email; });
  document.querySelectorAll('[data-cfg]').forEach(el => { if (CONFIG[el.dataset.cfg]) el.textContent = CONFIG[el.dataset.cfg]; else (el.closest('[data-cfg-block]') || el).remove(); });
  document.querySelectorAll('[data-facebook]').forEach(a => { if (CONFIG.facebook) a.href = CONFIG.facebook; else a.remove(); });

  const burger = document.querySelector('.burger'), nav = document.getElementById('nav');
  burger.addEventListener('click', () => {
    const open = nav.classList.toggle('open');
    burger.setAttribute('aria-expanded', open);
  });
}

function tape() {
  const fill = document.querySelector('.tape-fill'), count = document.querySelector('.tape-count');
  const upd = () => {
    const max = document.documentElement.scrollHeight - innerHeight;
    const p = max > 0 ? scrollY / max : 0;
    fill.style.width = (p * 100).toFixed(2) + '%';
    count.textContent = String(Math.round(scrollY / 10)).padStart(3, '0') + ' cm';
  };
  addEventListener('scroll', upd, { passive: true });
  addEventListener('resize', upd);
  upd();
}

function reveal(root = document) {
  const io = new IntersectionObserver(es => es.forEach(e => {
    if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target); }
  }), { threshold: .15 });
  root.querySelectorAll('.rv, .coupe').forEach(el => io.observe(el));
}

function compare(root = document) {
  root.querySelectorAll('.ba').forEach(ba => {
    const input = ba.querySelector('input'), bubble = ba.querySelector('.level b');
    const set = v => {
      ba.style.setProperty('--pos', v + '%');
      if (bubble) bubble.style.transform = `translateX(${(v - 50) * .3}px)`;
    };
    input.addEventListener('input', () => set(input.value));
    set(input.value);
  });
}

async function getJSON(path) {
  const r = await fetch(path, { cache: 'no-cache' });
  if (!r.ok) throw new Error(path + ' ' + r.status);
  return r.json();
}
window.getJSON = getJSON;

const imgPath = (slug, f) => `assets/realisations/${slug}/${f}`;
const chips = ids => `<div class="chips">${ids.map(m => `<span class="chip ${m}">${METIERS[m] || m}</span>`).join('')}</div>`;

function fiche(r, i) {
  return `<a class="fiche rv" href="chantier.html?c=${encodeURIComponent(r.slug)}" data-metiers="${r.metiers.join(' ')}">
    <div class="fiche-top"><span class="mono">Chantier n°${String(i + 1).padStart(3, '0')}</span>${r.fictif ? '<span class="mono tag">Exemple</span>' : r.date_affichee ? `<span class="mono">${esc(r.date_affichee)}</span>` : ''}</div>
    <div class="fiche-img"><img src="${imgPath(r.slug, r.couverture)}" alt="" loading="lazy"></div>
    <div class="fiche-body">
      <h3>${esc(r.titre)}</h3>
      <dl class="fields"><dt>Lieu</dt><dd>${esc(r.commune)}</dd><dt>Étapes</dt><dd>${r.etapes.length} photos commentées</dd></dl>
      ${chips(r.metiers)}
    </div></a>`;
}

// Réalisation publiée en vidéo sur Facebook : la vidéo ne se charge qu'au clic (pas de cookie Facebook avant)
function ficheVideo(v) {
  return `<article class="fiche video rv" data-metiers="${v.metiers.join(' ')}">
    <div class="fiche-top"><span class="mono">Chantier en vidéo</span><span class="mono tag">Facebook</span></div>
    <div class="fiche-video">
      ${v.integrable === false
        ? `<a class="play" href="${esc(v.url)}" target="_blank" rel="noopener" aria-label="Voir la vidéo sur Facebook : ${esc(v.titre)}">
        <span class="play-ico" aria-hidden="true"></span>
        <span class="play-txt">Voir la vidéo</span>
        <span class="play-note">S’ouvre sur Facebook</span>
      </a>`
        : `<button type="button" class="play" data-embed="${esc(v.embed)}" aria-label="Lire la vidéo : ${esc(v.titre)}">
        <span class="play-ico" aria-hidden="true"></span>
        <span class="play-txt">Lire la vidéo</span>
        <span class="play-note">La vidéo est hébergée par Facebook</span>
      </button>`}
    </div>
    <div class="fiche-body">
      <h3>${esc(v.titre)}</h3>
      <p class="fiche-txt">${esc(v.resume)}</p>
      ${chips(v.metiers)}
      <a class="lnk" href="${esc(v.url)}" target="_blank" rel="noopener">Voir sur Facebook →</a>
    </div></article>`;
}

async function renderFiches(el, { limit, filters } = {}) {
  try {
    const { realisations, videos = [] } = await getJSON('content/realisations.json');
    const cards = [];
    realisations.forEach((r, i) => cards.push(fiche(r, i)));
    videos.forEach(v => cards.push(ficheVideo(v)));
    if (!limit && CONFIG.facebookAlbum) cards.push(`<a class="fiche album rv" href="${CONFIG.facebookAlbum}" target="_blank" rel="noopener" data-metiers="carrelage platrerie couverture">
      <div class="fiche-top"><span class="mono">Album photo</span><span class="mono tag">Facebook</span></div>
      <div class="fiche-album" aria-hidden="true"><img src="assets/realisations/combles-briques/04-isolation-doublage.webp" alt=""><img src="assets/realisations/combles-briques/02-fenetre-de-toit.webp" alt=""><img src="assets/realisations/combles-briques/05-piece-livree.webp" alt=""></div>
      <div class="fiche-body"><h3>Toutes les photos de chantiers</h3><p class="fiche-txt">L’album complet de LOGE&amp;NERJ, mis à jour au fil des chantiers, sur la page Facebook.</p><span class="lnk">Ouvrir l’album sur Facebook →</span></div></a>`);
    el.innerHTML = (limit ? cards.slice(0, limit) : cards).join('');
    el.addEventListener('click', e => {
      const b = e.target.closest('button.play'); if (!b) return;
      b.outerHTML = `<iframe src="${b.dataset.embed}" width="267" height="476" style="border:none;overflow:hidden" scrolling="no" allowfullscreen allow="autoplay; clipboard-write; encrypted-media; picture-in-picture; web-share" title="Vidéo Facebook LOGE&amp;NERJ"></iframe>`;
    });
    reveal(el);
    if (filters) {
      filters.addEventListener('click', e => {
        const b = e.target.closest('button'); if (!b) return;
        filters.querySelectorAll('button').forEach(x => x.setAttribute('aria-pressed', x === b));
        el.querySelectorAll('.fiche').forEach(f => {
          f.style.display = (b.dataset.f === 'all' || f.dataset.metiers.includes(b.dataset.f)) ? '' : 'none';
        });
      });
    }
  } catch (err) {
    el.innerHTML = '<p>Impossible de charger les chantiers. Ouvrez le site via un serveur (voir CLAUDE.md).</p>';
  }
}

async function renderSamples(el) {
  const { metiers } = await getJSON('content/competences.json');
  el.innerHTML = metiers.map(m => `
    <a class="sample rv" href="savoir-faire.html#${m.id}">
      <div class="sample-tex tex-${m.texture}"><span class="mono">Réf. ${m.ref}</span></div>
      <div class="sample-body">
        <span class="status${m.statut === 'Nouveau' ? ' new' : ''}">${esc(m.statut)}</span>
        <h3>${esc(m.nom)}</h3>
        <p>${esc(m.accroche)}</p>
        <span class="go">Voir le détail <span>→</span></span>
      </div></a>`).join('');
  reveal(el);
}

async function renderSavoirFaire(el, nopeEl) {
  const { metiers, ne_fait_pas } = await getJSON('content/competences.json');
  el.innerHTML = metiers.map(m => `
    <section class="metier" id="${m.id}">
      <div class="metier-tex tex-${m.texture}"><span class="big">${m.ref}</span></div>
      <div class="metier-body rv">
        <span class="mono">Réf. ${m.ref} · ${esc(m.statut)}</span>
        <h2>${esc(m.nom)}</h2>
        <p class="accroche">${esc(m.accroche)}</p>
        <p>${esc(m.texte)}</p>
        <ul class="presta">${m.prestations.map(p => `<li>${esc(p)}</li>`).join('')}</ul>
        <div class="chips">${m.materiaux.map(x => `<span class="chip">${esc(x)}</span>`).join('')}</div>
      </div>
    </section>`).join('');
  nopeEl.innerHTML = ne_fait_pas.map(x => `<li>${esc(x)}</li>`).join('');
  if (!ne_fait_pas.length) nopeEl.closest('section').hidden = true;   // liste vide : la section disparaît
  reveal(document);
  if (location.hash) document.querySelector(location.hash)?.scrollIntoView();
}

async function renderChantier(el) {
  const slug = new URLSearchParams(location.search).get('c');
  const { realisations } = await getJSON('content/realisations.json');
  const idx = Math.max(0, realisations.findIndex(r => r.slug === slug));
  const r = realisations[idx];
  const next = realisations[(idx + 1) % realisations.length];
  document.title = `${r.titre} · ${CONFIG.nom}`;

  const ba = r.avant && r.apres ? `
    <div class="ba rv" aria-label="Comparaison avant et après">
      <img src="${imgPath(r.slug, r.avant)}" alt="Avant les travaux">
      <div class="ba-after" style="position:absolute;inset:0"><img src="${imgPath(r.slug, r.apres)}" alt="Après les travaux"></div>
      <span class="ba-tag l">Avant</span><span class="ba-tag r">Après</span>
      <div class="ba-handle"><div class="level"><i><b></b></i></div></div>
      <input type="range" min="0" max="100" value="50" aria-label="Glisser pour comparer avant et après">
    </div>
    <div class="ba-cap mono"><span>← Avant</span><span>Glissez le niveau</span><span>Après →</span></div>` : '';

  el.innerHTML = `
    <div class="wrap">
      <div class="panneau">
        <div class="panneau-l">
          <span class="mono">Chantier n°${String(idx + 1).padStart(3, '0')}${r.date_affichee ? ' · ' + esc(r.date_affichee) : ''}</span>
          <h1>${esc(r.titre)}</h1>
          <p>${esc(r.resume)}</p>
          ${r.fictif ? '<div class="fictif-note"><strong>Chantier d’exemple.</strong> Textes et visuels inventés pour la maquette, ils seront remplacés par un vrai chantier.</div>' : ''}
        </div>
        <div class="panneau-r">
          <dl class="fields">
            <dt>Nature</dt><dd>${esc(r.titre)}</dd>
            <dt>Lieu</dt><dd>${esc(r.commune)}</dd>
            ${r.duree ? `<dt>Durée</dt><dd>${esc(r.duree)}</dd>` : ''}
            <dt>Métiers</dt><dd>${chips(r.metiers)}</dd>
            <dt>Entreprise</dt><dd>${esc(CONFIG.nom)}</dd>
          </dl>
        </div>
      </div>
      ${ba}
    </div>
    <section class="sec"><div class="wrap">
      <div class="sec-head"><span class="sec-num">Journal</span><h2 class="sec-title">Le chantier, étape par étape</h2></div>
      <div class="etapes">
        ${r.etapes.map((e, i) => `
          <article class="etape rv">
            <span class="etape-n">${String(i + 1).padStart(2, '0')}</span>
            <figure data-img="${imgPath(r.slug, e.photo)}"><img src="${imgPath(r.slug, e.photo)}" alt="${esc(e.alt)}" loading="lazy"></figure>
            <div class="etape-txt">${e.metier ? `<span class="chip ${e.metier}">${METIERS[e.metier]}</span>` : '<span class="mono">Avant travaux</span>'}
              <h3>${esc(e.titre)}</h3><p>${esc(e.texte)}</p></div>
          </article>`).join('')}
      </div>
      <div class="next-ch">
        <a class="btn ghost" href="realisations.html">← Tous les chantiers</a>
        <a class="btn" href="chantier.html?c=${encodeURIComponent(next.slug)}">Chantier suivant <span class="arr">→</span></a>
      </div>
    </div></section>`;

  el.querySelectorAll('figure img').forEach(img => {
    const set = () => img.parentElement.classList.add(img.naturalHeight > img.naturalWidth ? 'portrait' : 'landscape');
    img.complete ? set() : img.addEventListener('load', set);
  });
  compare(el);
  reveal(el);
}

const PJ_MAX = 3, PJ_MAX_OCTETS = 9 * 1024 * 1024;
const PJ_TYPES = /^(image\/(jpeg|png|webp|heic|heif)|application\/pdf)$/;
const taille = o => o > 1048576 ? (o / 1048576).toFixed(1).replace('.', ',') + ' Mo' : Math.max(1, Math.round(o / 1024)) + ' Ko';

function contactForm(form) {
  const status = form.querySelector('.status-msg'), btn = form.querySelector('button[type=submit]');
  const started = Date.now();
  const pjInput = form.querySelector('#f-pj'), pjList = form.querySelector('.pj-list');
  let fichiers = [];
  const afficherPj = msg => {
    pjList.innerHTML = fichiers.map((f, i) => `<li><span>${esc(f.name)} · ${taille(f.size)}</span><button type="button" data-i="${i}" aria-label="Retirer ${esc(f.name)}">×</button></li>`).join('')
      + (msg ? `<li class="pj-err">${esc(msg)}</li>` : '');
  };
  if (pjInput) {
    pjInput.addEventListener('change', () => {
      let msg = '';
      [...pjInput.files].forEach(f => {
        if (!PJ_TYPES.test(f.type) && !/\.(jpe?g|png|webp|heic|heif|pdf)$/i.test(f.name)) { msg = `« ${f.name} » n’est pas une photo ni un PDF.`; return; }
        if (fichiers.length >= PJ_MAX) { msg = `${PJ_MAX} fichiers maximum.`; return; }
        if (fichiers.reduce((t, x) => t + x.size, 0) + f.size > PJ_MAX_OCTETS) { msg = `« ${f.name} » dépasse la limite de 9 Mo au total.`; return; }
        fichiers.push(f);
      });
      pjInput.value = '';
      afficherPj(msg);
    });
    pjList.addEventListener('click', e => {
      const b = e.target.closest('button[data-i]'); if (!b) return;
      fichiers.splice(+b.dataset.i, 1); afficherPj();
    });
  }
  form.addEventListener('submit', async e => {
    e.preventDefault();
    if (!form.checkValidity()) { form.reportValidity(); return; }
    if (form.website.value || Date.now() - started < 3000) { status.className = 'status-msg ok'; status.textContent = 'Merci, c’est envoyé.'; return; }
    const data = {
      source: 'formulaire-site', page: location.pathname,
      prenom: form.prenom.value.trim(), nom: form.nom.value.trim(),
      telephone: form.telephone.value.trim(), email: form.email.value.trim(),
      commune: form.commune.value.trim(),
      travaux: [...form.querySelectorAll('input[name=travaux]:checked')].map(x => x.value),
      surface: form.surface.value.trim(), delai: form.delai.value,
      description: form.description.value.trim(), consentement: form.consent.checked,
      date: new Date().toISOString()
    };
    btn.disabled = true; status.className = 'status-msg'; status.textContent = fichiers.length ? 'Envoi en cours (avec vos fichiers, cela peut prendre quelques secondes)…' : 'Envoi en cours…';
    try {
      if (!CONFIG.formWebhook) {
        await new Promise(r => setTimeout(r, 700));
        console.info('[maquette] demande non envoyée', data);
        status.className = 'status-msg ok';
        status.textContent = `Maquette : rien n’a été envoyé. En ligne, ${CONFIG.prenom} recevra cette demande par mail.`;
      } else {
        // envoi en multipart : champs texte + pièces jointes (pj0, pj1, pj2), transmises au mail puis non conservées
        const fd = new FormData();
        Object.entries(data).forEach(([k, v]) => fd.append(k, Array.isArray(v) ? v.join(',') : String(v)));
        fichiers.forEach((f, i) => fd.append('pj' + i, f, f.name));
        const res = await fetch(CONFIG.formWebhook, { method: 'POST', body: fd });
        if (!res.ok) throw new Error(res.status);
        const json = await res.json().catch(() => ({}));
        status.className = 'status-msg ok';
        status.textContent = json.message || `Merci ${data.prenom}, c’est bien reçu. Je vous rappelle rapidement.`;
      }
      form.reset(); fichiers = []; if (pjList) afficherPj();
    } catch {
      status.className = 'status-msg err';
      status.textContent = CONFIG.telephone ? `Ça n’est pas parti. Appelez-moi au ${CONFIG.telephone} ou écrivez à ${CONFIG.email}.` : `Ça n’est pas parti. Écrivez-moi directement à ${CONFIG.email}.`;
    } finally { btn.disabled = false; }
  });
}

function heroLogo() {
  const v = document.querySelector('.hero-logo'); if (!v) return;
  // mouvement réduit demandé : on montre directement l'image finale
  if (matchMedia('(prefers-reduced-motion: reduce)').matches) { v.removeAttribute('autoplay'); v.pause(); return; }
  v.addEventListener('click', () => { v.currentTime = 0; v.play(); });   // un clic rejoue l'animation
}

document.addEventListener('DOMContentLoaded', () => {
  layout(); tape(); reveal(); compare(); heroLogo();
  const $ = s => document.querySelector(s);
  if ($('#samples')) renderSamples($('#samples'));
  if ($('#fiches')) renderFiches($('#fiches'), { limit: +$('#fiches').dataset.limit || 0, filters: $('#filters') });
  if ($('#metiers')) renderSavoirFaire($('#metiers'), $('#nope-list'));
  if ($('#chantier')) renderChantier($('#chantier'));
  if ($('#devis')) contactForm($('#devis'));
});
