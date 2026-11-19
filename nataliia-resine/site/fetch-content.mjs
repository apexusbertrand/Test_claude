// Récupère les contenus publiés dans Supabase et réécrit site/content/*.json avant le build.
// Lancé par GitHub Actions. Sans SUPABASE_URL / SUPABASE_ANON_KEY, ne fait rien :
// le site est alors construit avec les fichiers JSON du dépôt.
// Usage : SUPABASE_URL=... SUPABASE_ANON_KEY=... node fetch-content.mjs

import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const URL_ = process.env.SUPABASE_URL;
const KEY = process.env.SUPABASE_ANON_KEY;
if (!URL_ || !KEY) {
  console.log('fetch-content : Supabase non configuré, contenus du dépôt utilisés.');
  process.exit(0);
}

const DIR = join(dirname(fileURLToPath(import.meta.url)), 'content');
const read = (f) => JSON.parse(readFileSync(join(DIR, f), 'utf8'));
const write = (f, v) => writeFileSync(join(DIR, f), JSON.stringify(v, null, 2) + '\n');

async function get(table, query = '') {
  // La clé anon ne voit que les lignes publiées (règles RLS)
  const r = await fetch(`${URL_}/rest/v1/${table}?select=*${query}`, {
    headers: { apikey: KEY, Authorization: `Bearer ${KEY}` },
  });
  if (!r.ok) throw new Error(`${table} : HTTP ${r.status} ${await r.text()}`);
  return r.json();
}

const [reglages, prestations, produits, realisations, avis] = await Promise.all([
  get('reglages', '&cle=eq.site'),
  get('prestations', '&publie=is.true&order=ordre,created_at'),
  get('produits', '&publie=is.true&order=ordre,created_at'),
  get('realisations', '&publie=is.true&order=ordre,date_chantier.desc.nullslast,created_at.desc'),
  get('avis', '&publie=is.true&order=ordre,date_avis.desc.nullslast'),
]);

// Réglages : coordonnées et accroche viennent de l'administration, le reste (mentions légales) du dépôt
const site = read('site.json');
const r = reglages[0]?.valeur;
if (r) {
  site.contact = { ...site.contact, telephone: r.telephone, email: r.email, whatsapp: r.whatsapp,
    telephoneLien: '+33' + String(r.telephone).replace(/\D/g, '').replace(/^0/, '') };
  site.zone = r.zone;
  site.zoneTexte = r.zoneTexte;
  site.accroche = r.accroche;
  site.accueil = r.accueil || null;
  site.logo = r.logo || null;
}
site.mediaBase = `${URL_}/storage/v1/object/public/media/`;
write('site.json', site);

write('prestations.json', prestations.map((p) => ({
  id: p.slug, titre: p.titre, resume: p.resume, texte: p.texte, photo: p.photo,
})));

const slugById = Object.fromEntries(produits.map((p) => [p.id, p.slug]));
const prodFile = read('produits.json');
write('produits.json', {
  categories: prodFile.categories,
  produits: produits.map((p) => ({
    id: p.slug, exemple: p.exemple, nom: p.nom, categorie: p.categorie, descriptif: p.descriptif,
    photos: p.photos, texture: p.texture, grainMm: p.grain_mm === null ? null : Number(p.grain_mm),
    lien: p.lien, pdf: p.pdf,
  })),
});

write('realisations.json', realisations.map((x) => ({
  id: x.slug, titre: x.titre, type: x.type, ville: x.ville, texte: x.texte,
  apres: x.apres, avant: x.avant, date: x.date_chantier,
  produits: (x.produits || []).map((id) => slugById[id]).filter(Boolean),
})));

write('avis.json', avis.map((a) => ({ nom: a.nom, ville: a.ville, texte: a.texte, date: a.date_avis })));

console.log(`fetch-content : ${prestations.length} prestations, ${produits.length} produits, ${realisations.length} réalisations, ${avis.length} avis.`);
