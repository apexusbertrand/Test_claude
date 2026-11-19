// Générateur du site Nataliia Résine. Aucune dépendance : node build.mjs
// Lit content/*.json, écrit le site statique dans ../www/
// Les contenus seront plus tard alimentés par l'administration (lot 2).

import { readFileSync, writeFileSync, mkdirSync, rmSync, cpSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = dirname(fileURLToPath(import.meta.url));
const OUT = join(ROOT, '..', 'www');
const json = (f) => JSON.parse(readFileSync(join(ROOT, 'content', f), 'utf8'));

const site = json('site.json');
const prestations = json('prestations.json');
const { categories, produits } = json('produits.json');
const realisations = json('realisations.json');
const avis = json('avis.json');

const esc = (s = '') => String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const typeLabel = Object.fromEntries(prestations.map((p) => [p.id, p.titre]));
const produitById = Object.fromEntries(produits.map((p) => [p.id, p]));
const granulatsSimulables = produits.filter((p) => p.categorie === 'granulats' && p.texture);

// Image : <picture> WebP + JPEG. `name` sans extension, dans static/img.
const pic = (name, alt, { sizes, widths, cls = '', eager = false } = {}) => {
  const lazy = eager ? 'fetchpriority="high"' : 'loading="lazy" decoding="async"';
  if (widths) {
    const set = (ext) => widths.map((w) => `img/${name}-${w}.${ext} ${w}w`).join(', ');
    const big = widths[widths.length - 1];
    return `<picture><source type="image/webp" srcset="${set('webp')}" sizes="${sizes}"><img class="${cls}" src="img/${name}-${big}.jpg" srcset="${set('jpg')}" sizes="${sizes}" alt="${esc(alt)}" ${lazy}></picture>`;
  }
  return `<picture><source type="image/webp" srcset="img/${name}.webp"><img class="${cls}" src="img/${name}.jpg" alt="${esc(alt)}" ${lazy}></picture>`;
};

// Navigation
const nav = [
  ['index.html', 'Accueil'],
  ['prestations.html', 'Prestations'],
  ['realisations.html', 'Réalisations'],
  ['produits.html', 'Produits'],
  ...(avis.length ? [['avis.html', 'Avis clients']] : []),
  ['contact.html', 'Contact'],
];

const jsonLd = () => JSON.stringify({
  '@context': 'https://schema.org',
  '@type': 'HomeAndConstructionBusiness',
  name: site.nom,
  description: site.description,
  url: site.url,
  logo: site.url + 'img/logo-640.jpg',
  image: site.url + 'img/partage.jpg',
  telephone: site.contact.telephoneLien,
  email: site.contact.email,
  areaServed: site.zone.map((v) => ({ '@type': 'City', name: v })),
});

function layout({ file, title, description, body, current = file, extraHead = '' }) {
  const fullTitle = file === 'index.html' ? `${site.nom} | Moquette de pierre, terrasses et allées dans le Nord` : `${title} | ${site.nom}`;
  const menu = nav.map(([href, label]) => `<li><a href="${href}"${href === current ? ' aria-current="page"' : ''}>${label}</a></li>`).join('');
  return `<!doctype html>
<html lang="fr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>${esc(fullTitle)}</title>
<meta name="description" content="${esc(description || site.description)}">
${site.preversion ? '<meta name="robots" content="noindex,nofollow">\n' : ''}<link rel="canonical" href="${site.url}${file === 'index.html' ? '' : file}">
<meta property="og:type" content="website">
<meta property="og:locale" content="fr_FR">
<meta property="og:site_name" content="${esc(site.nom)}">
<meta property="og:title" content="${esc(fullTitle)}">
<meta property="og:description" content="${esc(description || site.description)}">
<meta property="og:image" content="${site.url}img/partage.jpg">
<meta name="theme-color" content="#11100f">
<link rel="icon" type="image/png" sizes="32x32" href="img/favicon-32.png">
<link rel="apple-touch-icon" href="img/apple-touch-icon.png">
<link rel="preload" href="fonts/playfair-display-latin-500-normal.woff2" as="font" type="font/woff2" crossorigin>
<link rel="preload" href="fonts/dm-sans-latin-400-normal.woff2" as="font" type="font/woff2" crossorigin>
<link rel="stylesheet" href="css/site.css">
${extraHead}${file === 'index.html' ? `<script type="application/ld+json">${jsonLd()}</script>\n` : ''}</head>
<body>
<a class="skip" href="#contenu">Aller au contenu</a>
${site.preversion ? '<div class="preview-bar">Préversion de travail : textes et photos à valider, site non référencé.</div>\n' : ''}<header class="site-header">
  <a class="brand-image" href="index.html" aria-label="${esc(site.nom)}, accueil">${pic('logo-320', 'Logo ' + site.nom, { cls: '', eager: true }).replace('img/logo-320.webp"', 'img/logo-320.webp 320w, img/logo-640.webp 640w" sizes="420px"').replace('src="img/logo-320.jpg"', 'src="img/logo-640.jpg"')}</a>
  <button class="menu-toggle" type="button" aria-expanded="false" aria-controls="site-menu"><span class="menu-bars" aria-hidden="true"><span></span><span></span><span></span></span><span class="menu-label">Menu</span></button>
  <nav class="site-menu" id="site-menu" aria-label="Menu principal" hidden><ul>${menu}<li class="menu-cta"><a href="projet.html"${current === 'projet.html' ? ' aria-current="page"' : ''}>Créez votre projet</a></li></ul></nav>
</header>
<main id="contenu">
${body}
</main>
<footer class="site-footer">
  <div class="footer-grid">
    <div>
      <p class="footer-brand">Nataliia <b>Résine</b></p>
      <p>${esc(site.accroche.join(' '))}</p>
    </div>
    <div>
      <h2>Le site</h2>
      <ul>${nav.slice(1).map(([h, l]) => `<li><a href="${h}">${l}</a></li>`).join('')}<li><a href="projet.html">Créez votre projet</a></li></ul>
    </div>
    <div>
      <h2>Contact</h2>
      <ul>
        <li><a href="tel:${site.contact.telephoneLien}">${site.contact.telephone}</a></li>
        <li><a href="${site.contact.whatsapp}" rel="noopener">WhatsApp</a></li>
        <li><a href="mailto:${site.contact.email}">${site.contact.email}</a></li>
      </ul>
    </div>
    <div>
      <h2>Zone d'intervention</h2>
      <p>${esc(site.zoneTexte)}</p>
    </div>
  </div>
  <div class="footer-bottom">
    <span>© ${new Date().getFullYear()} ${esc(site.nom)}</span>
    <span><a href="mentions-legales.html">Mentions légales</a> · <a href="confidentialite.html">Confidentialité</a></span>
  </div>
</footer>
<script src="js/site.js" defer></script>
</body>
</html>
`;
}

const pageHead = (eyebrow, title, intro, crumbs) => `<section class="page-head">
  ${crumbs ? `<p class="breadcrumb">${crumbs}</p>` : ''}
  <span class="eyebrow">${eyebrow}</span>
  <h1>${title}</h1>
  ${intro ? `<p class="lead">${intro}</p>` : ''}
</section>`;

const cta = (title = 'Un projet en tête ?', text = 'Une question, un devis ou besoin d’un conseil ? Appelez, écrivez sur WhatsApp ou envoyez une photo de votre extérieur.') => `<section class="section section-dark">
  <div>
    <span class="eyebrow">Parlons de votre projet</span>
    <h2 style="font-size:clamp(30px,4vw,48px);margin-top:12px">${title}</h2>
    <p class="lead">${text}</p>
    <div class="actions">
      <a class="btn" href="projet.html">Créez votre projet <span aria-hidden="true">↗</span></a>
      <a class="btn btn-ghost" href="contact.html">Nous contacter</a>
    </div>
  </div>
</section>`;

// Cartes réutilisées
const realisationCard = (r) => `<a class="card" href="realisation-${r.id}.html" data-type="${esc(r.type)}">
  <div class="card-media">${pic(r.apres[0], r.titre)}</div>
  <div class="card-body"><span class="card-index">${esc(typeLabel[r.type] || r.type)}${r.ville ? ' · ' + esc(r.ville) : ''}</span><h3>${esc(r.titre)}</h3><span class="more">Voir la réalisation ↗</span></div>
</a>`;

const produitCard = (p) => `<a class="card" href="produit-${p.id}.html">
  ${p.photos?.length ? `<div class="card-media">${pic(p.photos[0], p.nom)}</div>` : ''}
  <div class="card-body">${p.exemple ? '<span class="badge">Exemple</span>' : ''}<h3>${esc(p.nom)}</h3>${p.descriptif ? `<p>${esc(p.descriptif.split('. ')[0])}.</p>` : ''}<span class="more">Voir la fiche ↗</span></div>
</a>`;

const etapes = [
  ['Visite et conseil', 'On regarde ensemble la surface, l’état du support et vos envies. On choisit le granulat et la finition.'],
  ['Préparation du support', 'Nettoyage, réparation des fissures et des défauts, pose des profils de finition sur les bords.'],
  ['Mélange', 'Les granulats de marbre sont mélangés à la résine sur place, en quantité adaptée à la surface.'],
  ['Application', 'Le mélange est étalé et lissé à la taloche, pour une surface régulière et sans joints.'],
  ['Séchage et livraison', 'La résine durcit, la surface est prête. Le délai avant usage est précisé au devis selon la météo.'],
];

const avantages = [
  ['Drainante', 'L’eau de pluie traverse le revêtement : moins de flaques.'],
  ['Antidérapante', 'Le relief des granulats accroche sous le pied, même mouillé.'],
  ['Agréable pieds nus', 'Une surface douce, appréciée autour des piscines.'],
  ['Sans joints', 'Un revêtement continu, qui suit les courbes et les marches.'],
  ['Sur l’existant', 'Après préparation, pose possible sur béton ou carrelage.'],
  ['Sur mesure', 'Le choix du granulat donne la teinte et le style du revêtement.'],
];

const pages = {};

// Accueil
pages['index.html'] = layout({
  file: 'index.html',
  description: site.description,
  extraHead: '<link rel="preload" as="image" type="image/webp" imagesrcset="img/accueil-640.webp 640w, img/accueil-1024.webp 1024w, img/accueil-1672.webp 1672w" imagesizes="100vw">\n',
  body: `<section class="home-hero" aria-label="Terrasse en moquette de pierre">
  ${pic('accueil', 'Pose d’un revêtement en moquette de pierre sur une terrasse au coucher du soleil', { widths: [640, 1024, 1672], sizes: '100vw', eager: true })}
  <nav class="hero-tabs" aria-label="Accès rapide">
    <a class="hero-tab" href="prestations.html"><span class="hero-tab-mark" aria-hidden="true">◈</span><span>Prestations</span></a>
    <a class="hero-tab" href="realisations.html"><span class="hero-tab-mark" aria-hidden="true">▣</span><span>Réalisations</span></a>
    <a class="hero-tab" href="produits.html"><span class="hero-tab-mark" aria-hidden="true">◇</span><span>Produits</span></a>
    <a class="hero-tab" href="contact.html"><span class="hero-tab-mark" aria-hidden="true">✧</span><span>Contact</span></a>
    <a class="hero-tab" href="projet.html"><span class="hero-tab-mark" aria-hidden="true">⌂</span><span>Créez votre projet</span></a>
  </nav>
</section>
<section class="signature"><span class="eyebrow">Nataliia Résine</span><p>${esc(site.accroche[0])}<br><em>${esc(site.accroche[1])}</em></p></section>

<section class="section section-alt">
  <div class="section-head"><span class="eyebrow">Nos savoir-faire</span><h2>Moquette de pierre, dedans comme dehors</h2>
  <p class="lead">Granulats de marbre et résine pour terrasses, allées, escaliers, abords de piscine et bien d’autres surfaces.</p></div>
  <div class="grid">${prestations.slice(0, 4).map((p, i) => `<a class="card" href="prestations.html#${p.id}"><div class="card-body"><span class="card-index">0${i + 1}</span><h3>${esc(p.titre)}</h3><p>${esc(p.resume)}</p><span class="more">En savoir plus ↗</span></div></a>`).join('')}</div>
  <div class="actions"><a class="btn btn-ghost" href="prestations.html">Toutes les prestations ↗</a></div>
</section>

${realisations.length ? `<section class="section">
  <div class="section-head"><span class="eyebrow">Réalisations</span><h2>Derniers chantiers</h2></div>
  <div class="grid">${realisations.slice(0, 3).map(realisationCard).join('')}</div>
  <div class="actions"><a class="btn btn-ghost" href="realisations.html">Toutes les réalisations ↗</a></div>
</section>` : ''}

<section class="section section-dark">
  <div class="section-head"><span class="eyebrow">Pourquoi la moquette de pierre</span><h2>Un sol beau, et pratique</h2></div>
  <ul class="perks">${avantages.map(([t, d]) => `<li><strong>${t}</strong><span>${d}</span></li>`).join('')}</ul>
</section>

<section class="section">
  <div class="section-head"><span class="eyebrow">Déroulé d’un chantier</span><h2>De la visite à la livraison</h2></div>
  <ol class="steps">${etapes.map(([t, d], i) => `<li><span class="num">${i + 1}</span><h3>${t}</h3><p>${d}</p></li>`).join('')}</ol>
</section>

<section class="section section-alt">
  <div class="section-head"><span class="eyebrow">Zone d’intervention</span><h2>Dans le Nord et le Pas-de-Calais</h2>
  <p class="lead">J’interviens autour de ces villes. Vous êtes un peu plus loin ? Appelez, on en parle.</p></div>
  <ul class="zone">${site.zone.map((v) => `<li>${v}</li>`).join('')}</ul>
</section>

${cta('Voyez votre extérieur autrement', 'Envoyez une photo de votre terrasse ou de votre allée et choisissez un granulat : on prépare votre projet ensemble.')}`,
});

// Prestations
pages['prestations.html'] = layout({
  file: 'prestations.html',
  title: 'Prestations',
  description: 'Terrasses, allées, escaliers, abords de piscine, balcons, plages de garage, intérieur et rénovation en moquette de pierre, autour de ' + site.zoneTexte + '.',
  body: `${pageHead('Nos savoir-faire', 'Prestations', 'Des aménagements qui allient esthétique, confort et caractère.')}
<section class="section" style="padding-top:0">
  <div class="grid grid-2">${prestations.map((p, i) => `<article class="card" id="${p.id}">
    ${p.photo ? `<div class="card-media">${pic(p.photo, p.titre)}</div>` : ''}
    <div class="card-body"><span class="card-index">${String(i + 1).padStart(2, '0')}</span><h2>${esc(p.titre)}</h2><p><strong style="color:var(--ink);font-weight:500">${esc(p.resume)}</strong></p><p>${esc(p.texte)}</p>
    ${realisations.some((r) => r.type === p.id) ? `<a class="more" href="realisations.html">Voir les réalisations ↗</a>` : ''}</div>
  </article>`).join('')}</div>
</section>
<section class="section section-alt">
  <div class="section-head"><span class="eyebrow">Déroulé d’un chantier</span><h2>Comment ça se passe</h2></div>
  <ol class="steps">${etapes.map(([t, d], i) => `<li><span class="num">${i + 1}</span><h3>${t}</h3><p>${d}</p></li>`).join('')}</ol>
  <p class="note" style="margin-top:20px">Entretien : un nettoyage à l’eau suffit au quotidien. Les conseils précis vous sont donnés à la fin du chantier.</p>
</section>
${cta()}`,
});

// Réalisations
const typesPresents = [...new Set(realisations.map((r) => r.type))];
pages['realisations.html'] = layout({
  file: 'realisations.html',
  title: 'Réalisations',
  description: 'Chantiers réalisés en moquette de pierre : photos avant et après, granulats utilisés.',
  body: `${pageHead('En images', 'Réalisations', 'Des chantiers réels, photographiés avant et après, avec les produits utilisés.')}
<section class="section" style="padding-top:0">
  ${realisations.length ? `<div class="filters" role="group" aria-label="Filtrer par type">
    <button class="filter" type="button" data-filter="tout" aria-pressed="true">Tout</button>
    ${typesPresents.map((t) => `<button class="filter" type="button" data-filter="${esc(t)}" aria-pressed="false">${esc(typeLabel[t] || t)}</button>`).join('')}
  </div>
  <div class="grid">${realisations.map(realisationCard).join('')}</div>` : `<div class="empty">
    <span class="eyebrow">Bientôt en ligne</span>
    <h2>Les premières réalisations arrivent</h2>
    <p>Les photos avant et après des chantiers seront publiées ici au fur et à mesure. En attendant, découvrez les prestations ou parlez-moi de votre projet.</p>
    <div class="actions"><a class="btn" href="prestations.html">Voir les prestations</a><a class="btn btn-ghost" href="projet.html">Créez votre projet ↗</a></div>
  </div>`}
</section>
${cta()}`,
});

for (const r of realisations) {
  const prods = (r.produits || []).map((id) => produitById[id]).filter(Boolean);
  const compare = r.avant?.length ? `<div class="compare" style="--pos:50%">
    ${pic(r.avant[0], 'Avant : ' + r.titre)}
    <div class="after">${pic(r.apres[0], 'Après : ' + r.titre)}</div>
    <span class="lbl lbl-before">Avant</span><span class="lbl lbl-after">Après</span>
    <span class="handle"></span>
    <input type="range" min="0" max="100" value="50" aria-label="Comparer avant et après">
  </div>` : '';
  pages[`realisation-${r.id}.html`] = layout({
    file: `realisation-${r.id}.html`,
    current: 'realisations.html',
    title: r.titre,
    description: `${typeLabel[r.type] || ''} en moquette de pierre${r.ville ? ' à ' + r.ville : ''} : photos avant et après.`,
    body: `${pageHead(esc(typeLabel[r.type] || r.type) + (r.ville ? ' · ' + esc(r.ville) : ''), esc(r.titre), r.texte ? esc(r.texte) : '', '<a href="realisations.html">Réalisations</a> / ' + esc(r.titre))}
<section class="section" style="padding-top:0">
  ${compare}
  <div class="gallery" style="margin-top:16px">${r.apres.map((ph) => pic(ph, r.titre)).join('')}${(r.avant || []).slice(1).map((ph) => pic(ph, 'Avant : ' + r.titre)).join('')}</div>
  ${prods.length ? `<h2 style="margin-top:40px;font-size:28px">Produits utilisés</h2><div class="grid">${prods.map(produitCard).join('')}</div>` : ''}
</section>
${cta()}`,
  });
}

// Produits
pages['produits.html'] = layout({
  file: 'produits.html',
  title: 'Produits',
  description: 'Les granulats de marbre et profils de finition utilisés pour les revêtements en moquette de pierre.',
  body: `${pageHead('Notre sélection', 'Produits', 'Les matériaux et accessoires utilisés sur les chantiers, pour des réalisations soignées. Ils ne sont pas vendus séparément.')}
${categories.map((c, i) => {
    const list = produits.filter((p) => p.categorie === c.id);
    return `<section class="section${i % 2 ? ' section-alt' : ''}" style="${i === 0 ? 'padding-top:0' : ''}">
  <div class="section-head"><span class="card-index">${String(i + 1).padStart(2, '0')}</span><h2>${esc(c.titre)}</h2><p class="lead">${esc(c.texte)}</p></div>
  ${list.length ? `<div class="grid">${list.map(produitCard).join('')}</div>` : '<p class="note">Les références arrivent bientôt.</p>'}
</section>`;
  }).join('\n')}
${cta('Un renseignement sur un produit ?', 'Couleurs, granulométrie, rendu : je vous conseille selon votre projet.')}`,
});

for (const p of produits) {
  const cat = categories.find((c) => c.id === p.categorie);
  const facts = [
    ['Catégorie', cat?.titre],
    ['Taille du grain', p.grainMm ? `${p.grainMm} mm environ` : null],
  ].filter(([, v]) => v);
  pages[`produit-${p.id}.html`] = layout({
    file: `produit-${p.id}.html`,
    current: 'produits.html',
    title: p.nom,
    description: p.descriptif || `${p.nom}, ${cat?.titre || ''}.`,
    body: `${pageHead(esc(cat?.titre || ''), esc(p.nom), '', '<a href="produits.html">Produits</a> / ' + esc(p.nom))}
<section class="section" style="padding-top:0">
  <div class="product">
    <div class="product-media">${p.photos?.length ? p.photos.map((ph) => pic(ph, p.nom)).join('') : ''}</div>
    <div>
      ${p.exemple ? '<p class="notice">Produit d’exemple : il sera remplacé par les vraies références.</p>' : ''}
      ${p.descriptif ? `<p class="lead">${esc(p.descriptif)}</p>` : ''}
      ${facts.length ? `<ul class="facts">${facts.map(([k, v]) => `<li><span>${k}</span><strong>${esc(v)}</strong></li>`).join('')}</ul>` : ''}
      <div class="actions">
        ${p.texture ? `<a class="btn" href="projet.html?granulat=${p.id}">Essayer sur ma photo <span aria-hidden="true">↗</span></a>` : ''}
        ${p.lien ? `<a class="btn btn-ghost" href="${esc(p.lien)}" rel="noopener" target="_blank">Site du fournisseur ↗</a>` : ''}
        ${p.pdf ? `<a class="btn btn-ghost" href="${esc(p.pdf)}">Fiche technique (PDF)</a>` : ''}
        <a class="btn btn-ghost" href="contact.html">Demander des informations</a>
      </div>
    </div>
  </div>
</section>`,
  });
}

// Avis (page créée seulement s'il y a des avis)
if (avis.length) {
  pages['avis.html'] = layout({
    file: 'avis.html',
    title: 'Avis clients',
    body: `${pageHead('Ils m’ont fait confiance', 'Avis clients', '')}
<section class="section" style="padding-top:0"><div class="grid">${avis.map((a) => `<figure class="card" style="margin:0"><div class="card-body"><blockquote style="margin:0"><p>${esc(a.texte)}</p></blockquote><figcaption class="card-index" style="margin-top:12px">${esc(a.nom)}${a.ville ? ' · ' + esc(a.ville) : ''}</figcaption></div></figure>`).join('')}</div></section>
${cta()}`,
  });
}

// Contact
pages['contact.html'] = layout({
  file: 'contact.html',
  title: 'Contact',
  description: 'Contactez Nataliia Résine par téléphone, WhatsApp ou e-mail pour un devis de moquette de pierre.',
  body: `${pageHead('Un projet en tête ?', 'Contact', 'Une question, un devis ou besoin d’un conseil ? Contactez-moi directement.')}
<section class="section" style="padding-top:0">
  <div class="contact-list">
    <a class="contact-row" href="tel:${site.contact.telephoneLien}"><span class="card-index">Téléphone</span><strong>${site.contact.telephone}</strong><span class="arrow" aria-hidden="true">↗</span></a>
    <a class="contact-row" href="${site.contact.whatsapp}" target="_blank" rel="noopener"><span class="card-index">WhatsApp</span><strong>Écrire sur WhatsApp</strong><span class="arrow" aria-hidden="true">↗</span></a>
    <a class="contact-row" href="mailto:${site.contact.email}"><span class="card-index">E-mail</span><strong>${site.contact.email}</strong><span class="arrow" aria-hidden="true">↗</span></a>
    <a class="contact-row" href="projet.html"><span class="card-index">Avec une photo</span><strong>Créez votre projet</strong><span class="arrow" aria-hidden="true">↗</span></a>
  </div>
  <h2 style="margin-top:56px;font-size:28px">Zone d’intervention</h2>
  <ul class="zone">${site.zone.map((v) => `<li>${v}</li>`).join('')}</ul>
  <p class="note" style="margin-top:14px">Et leurs alentours. Pour un chantier plus éloigné, appelez pour en parler.</p>
</section>`,
});

// Créez votre projet (version provisoire, remplacée par le simulateur au lot 3)
pages['projet.html'] = layout({
  file: 'projet.html',
  title: 'Créez votre projet',
  description: 'Envoyez une photo de votre terrasse ou de votre allée et choisissez votre granulat de marbre.',
  body: `${pageHead('Votre extérieur, votre style', 'Créez votre projet', 'Téléchargez une photo de votre terrasse, de votre allée ou de votre escalier, puis choisissez un granulat pour préparer votre projet.')}
<section class="section" style="padding-top:0">
  <form class="panel" id="project-form" data-email="${site.contact.email}">
    <p class="notice">Bientôt : vous verrez le granulat choisi posé directement sur votre photo, avec un curseur avant/après.</p>
    <h2 style="margin-top:26px">1. Votre photo</h2>
    <div class="upload-zone" id="upload-zone">
      <div class="upload-prompt"><strong>Ajoutez une photo de votre extérieur</strong><p class="note">Depuis votre téléphone ou votre ordinateur. Cadrez sur la surface à recouvrir.</p></div>
      <img id="photo-preview" alt="Aperçu de votre photo">
      <label class="visually-hidden" for="photo">Choisir une photo</label>
      <input id="photo" type="file" accept="image/*">
    </div>
    <h2>2. Votre granulat</h2>
    ${granulatsSimulables.length ? `<div class="swatches">${granulatsSimulables.map((g) => `<button class="swatch" type="button" data-id="${g.id}" data-name="${esc(g.nom)}" aria-pressed="false">${pic(g.texture, '', { eager: true })}${esc(g.nom)}</button>`).join('')}</div>
    <p class="note">Granulat choisi : <strong id="chosen"></strong></p>` : '<p class="note">Les granulats arrivent bientôt.</p><span id="chosen" hidden></span>'}
    <h2>3. Vos coordonnées et votre projet</h2>
    <div class="fields">
      <div class="field"><label for="nom">Nom</label><input id="nom" autocomplete="name"></div>
      <div class="field"><label for="tel">Téléphone</label><input id="tel" type="tel" autocomplete="tel"></div>
      <div class="field"><label for="ville">Ville</label><input id="ville" autocomplete="address-level2"></div>
      <div class="field"><label for="type">Type de surface</label><select id="type"><option value="">Choisir</option>${prestations.map((p) => `<option>${esc(p.titre)}</option>`).join('')}<option>Autre</option></select></div>
      <div class="field"><label for="surface">Surface approximative (m²)</label><input id="surface" inputmode="decimal"></div>
    </div>
    <div class="field"><label for="message">Votre projet (facultatif)</label><textarea id="message" rows="4" placeholder="Dimensions, délai souhaité, détails utiles"></textarea></div>
    <div class="actions"><button class="btn" type="submit">Envoyer mon projet <span aria-hidden="true">↗</span></button></div>
    <p class="note">Votre messagerie s’ouvre avec les informations préremplies : pensez à y joindre votre photo. Vos informations servent uniquement à répondre à votre demande (voir la <a href="confidentialite.html">politique de confidentialité</a>).</p>
  </form>
</section>`,
});

// Mentions légales
const L = site.legal;
pages['mentions-legales.html'] = layout({
  file: 'mentions-legales.html',
  title: 'Mentions légales',
  body: `${pageHead('Informations', 'Mentions légales', '')}
<section class="section prose" style="padding-top:0">
  <h2 style="margin-top:0">Éditeur du site</h2>
  <dl>
    <dt>Nom</dt><dd>${esc(L.dirigeante)} EI, nom commercial ${esc(site.nom)}</dd>
    <dt>Forme</dt><dd>${esc(L.forme)}</dd>
    <dt>Adresse</dt><dd>${esc(L.adresse)}</dd>
    <dt>SIREN</dt><dd>${esc(L.siren)}</dd>
    <dt>SIRET</dt><dd>${esc(L.siret)}</dd>
    <dt>TVA intracommunautaire</dt><dd>${esc(L.tva)}</dd>
    <dt>Téléphone</dt><dd><a href="tel:${site.contact.telephoneLien}">${site.contact.telephone}</a></dd>
    <dt>E-mail</dt><dd><a href="mailto:${site.contact.email}">${site.contact.email}</a></dd>
    <dt>Directrice de la publication</dt><dd>${esc(L.dirigeante)}</dd>
  </dl>
  <h2>Hébergement</h2>
  <p>${esc(L.hebergeur.nom)}, ${esc(L.hebergeur.adresse)}, <a href="${L.hebergeur.site}" rel="noopener">${L.hebergeur.site.replace('https://', '')}</a> (service GitHub Pages).</p>
  <h2>Propriété des contenus</h2>
  <p>Les textes, photos et logos de ce site appartiennent à ${esc(L.dirigeante)} EI, sauf mention contraire. Toute reproduction sans autorisation est interdite.</p>
  <p>L’image de la page d’accueil est une illustration, elle ne représente pas un chantier réel. Les produits marqués « Exemple » sont des démonstrations.</p>
  <h2>Données personnelles</h2>
  <p>Voir la <a href="confidentialite.html">politique de confidentialité</a>.</p>
</section>`,
});

// Confidentialité
pages['confidentialite.html'] = layout({
  file: 'confidentialite.html',
  title: 'Politique de confidentialité',
  body: `${pageHead('Vos données', 'Politique de confidentialité', '')}
<section class="section prose" style="padding-top:0">
  <h2 style="margin-top:0">Responsable du traitement</h2>
  <p>${esc(L.dirigeante)} EI (${esc(site.nom)}), ${esc(L.adresse)}, <a href="mailto:${site.contact.email}">${site.contact.email}</a>.</p>
  <h2>Données collectées</h2>
  <p>Le site ne crée pas de compte et ne dépose aucun cookie de suivi ni de mesure d’audience. Les polices de caractères sont hébergées avec le site : aucune donnée n’est envoyée à un service tiers pendant la visite.</p>
  <p>Quand vous nous contactez (téléphone, WhatsApp, e-mail, ou page « Créez votre projet », qui ouvre votre messagerie), nous recevons les informations que vous choisissez de transmettre : nom, coordonnées, ville, description du projet et photos.</p>
  <h2>Utilisation</h2>
  <p>Ces informations servent uniquement à répondre à votre demande et à établir un devis. Elles ne sont ni vendues ni transmises à des tiers.</p>
  <h2>Durée de conservation</h2>
  <p>Les échanges avec une personne qui n’est pas devenue cliente sont conservés 3 ans après le dernier contact, puis supprimés.</p>
  <h2>Vos droits</h2>
  <p>Vous pouvez demander l’accès à vos données, leur rectification ou leur suppression en écrivant à <a href="mailto:${site.contact.email}">${site.contact.email}</a>. Vous pouvez aussi saisir la CNIL (cnil.fr).</p>
</section>`,
});

// 404
pages['404.html'] = layout({
  file: '404.html',
  title: 'Page introuvable',
  body: `${pageHead('Erreur 404', 'Page introuvable', 'Cette page n’existe pas ou a été déplacée.')}
<section class="section" style="padding-top:0"><a class="btn" href="index.html">Retour à l’accueil</a></section>`,
});

// Écriture
if (existsSync(OUT)) rmSync(OUT, { recursive: true });
mkdirSync(OUT, { recursive: true });
cpSync(join(ROOT, 'static'), OUT, { recursive: true });
for (const [f, html] of Object.entries(pages)) writeFileSync(join(OUT, f), html);

// 404 servie à la racine par GitHub Pages : liens absolus nécessaires
const base = new URL(site.url).pathname;
writeFileSync(join(OUT, '404.html'), pages['404.html'].replace(/(href|src|srcset|imagesrcset)="(?!https?:|mailto:|tel:|#|\/)([^"]+)"/g, (m, a, v) => `${a}="${v.split(', ').map((s) => base + s).join(', ')}"`));

const indexable = Object.keys(pages).filter((f) => f !== '404.html');
writeFileSync(join(OUT, 'sitemap.xml'), `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${indexable.map((f) => `  <url><loc>${site.url}${f === 'index.html' ? '' : f}</loc></url>`).join('\n')}\n</urlset>\n`);
writeFileSync(join(OUT, 'robots.txt'), site.preversion ? 'User-agent: *\nDisallow: /\n' : `User-agent: *\nAllow: /\nSitemap: ${site.url}sitemap.xml\n`);
writeFileSync(join(OUT, '.nojekyll'), '');

console.log(`${Object.keys(pages).length} pages écrites dans www/`);
