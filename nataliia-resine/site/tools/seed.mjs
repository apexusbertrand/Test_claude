// Génère supabase/seed.sql à partir de site/content/*.json (contenus du lot 1).
// À exécuter une fois après schema.sql. N'écrase rien : « on conflict do nothing ».
// Usage : node tools/seed.mjs

import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const json = (f) => JSON.parse(readFileSync(join(ROOT, 'content', f), 'utf8'));
const q = (v) => (v === null || v === undefined ? 'null' : `'${String(v).replace(/'/g, "''")}'`);
const j = (v) => (v === null || v === undefined ? 'null' : `${q(JSON.stringify(v))}::jsonb`);

const site = json('site.json');
const prestations = json('prestations.json');
const { produits } = json('produits.json');

// Photo locale (image fournie avec le site) : {"static": "nom"}
const stat = (name, alt) => ({ static: name, alt });

const lines = ['-- Généré par site/tools/seed.mjs, ne pas modifier à la main.', 'begin;', ''];

lines.push(`insert into public.reglages (cle, valeur) values ('site', ${j({
  telephone: site.contact.telephone,
  email: site.contact.email,
  whatsapp: site.contact.whatsapp,
  zone: site.zone,
  zoneTexte: site.zoneTexte,
  accroche: site.accroche,
  accueil: null,
  logo: null,
})}) on conflict (cle) do nothing;`, '');

prestations.forEach((p, i) => {
  lines.push(`insert into public.prestations (slug, titre, resume, texte, photo, ordre) values (${q(p.id)}, ${q(p.titre)}, ${q(p.resume)}, ${q(p.texte)}, null, ${i}) on conflict (slug) do nothing;`);
});
lines.push('');

produits.forEach((p, i) => {
  lines.push(`insert into public.produits (slug, nom, categorie, descriptif, photos, texture, grain_mm, exemple, ordre) values (${q(p.id)}, ${q(p.nom)}, ${q(p.categorie)}, ${q(p.descriptif)}, ${j(p.photos.map((ph) => stat(ph, p.nom)))}, ${j(p.texture ? stat(p.texture, 'Texture ' + p.nom) : null)}, ${p.grainMm ?? 'null'}, ${p.exemple ? 'true' : 'false'}, ${i}) on conflict (slug) do nothing;`);
});

lines.push('', 'commit;', '');
writeFileSync(join(ROOT, '..', 'supabase', 'seed.sql'), lines.join('\n'));
console.log('supabase/seed.sql écrit');
