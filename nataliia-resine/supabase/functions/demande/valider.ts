// Validation d'une demande envoyée par le simulateur. Pur : testé par demande.test.ts.

export const MAX_PHOTOS = 2;
export const MAX_FICHIER = 6 * 1024 * 1024; // 6 Mo par image

export type Fichier = { nom: string; type: string; octets: Uint8Array };
export type Photo = { original: Fichier; rendu: Fichier; granulat: string; granulatNom: string };
export type Demande = {
  nom: string; telephone: string; email: string; ville: string;
  type_surface: string | null; surface_m2: number | null; delai: string | null; message: string | null;
  photos: Photo[];
};

const txt = (v: FormDataEntryValue | null, max: number) => (typeof v === 'string' ? v.trim().slice(0, max) : '');

// Type réel d'une image d'après ses premiers octets (on ne croit pas l'en-tête envoyé)
export function typeImage(b: Uint8Array): string | null {
  if (b.length > 3 && b[0] === 0xff && b[1] === 0xd8 && b[2] === 0xff) return 'image/jpeg';
  if (b.length > 8 && b[0] === 0x89 && b[1] === 0x50 && b[2] === 0x4e && b[3] === 0x47) return 'image/png';
  if (b.length > 12 && String.fromCharCode(...b.slice(0, 4)) === 'RIFF' && String.fromCharCode(...b.slice(8, 12)) === 'WEBP') return 'image/webp';
  return null;
}

export async function valider(fd: FormData): Promise<{ ok: true; demande: Demande } | { ok: false; erreurs: string[] }> {
  const erreurs: string[] = [];
  const nom = txt(fd.get('nom'), 100);
  const telephone = txt(fd.get('telephone'), 30);
  const email = txt(fd.get('email'), 150).toLowerCase();
  const ville = txt(fd.get('ville'), 100);
  if (!nom) erreurs.push('Nom manquant.');
  if (!/^[+\d][\d .-]{8,}$/.test(telephone)) erreurs.push('Téléphone invalide.');
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) erreurs.push('E-mail invalide.');
  if (!ville) erreurs.push('Ville manquante.');
  if (txt(fd.get('consentement'), 3) !== 'oui') erreurs.push('Accord sur le traitement des données manquant.');
  const surfaceTxt = txt(fd.get('surface_m2'), 10).replace(',', '.');
  const surface = surfaceTxt ? Number(surfaceTxt) : null;
  if (surface !== null && !(surface > 0 && surface < 100000)) erreurs.push('Surface invalide.');

  const photos: Photo[] = [];
  for (let k = 1; k <= MAX_PHOTOS + 1; k++) {
    const o = fd.get(`photo${k}_original`); const r = fd.get(`photo${k}_rendu`);
    if (!o && !r) continue;
    if (k > MAX_PHOTOS) { erreurs.push(`${MAX_PHOTOS} photos au maximum.`); break; }
    if (!(o instanceof File) || !(r instanceof File)) { erreurs.push(`Photo ${k} incomplète.`); continue; }
    const fichiers: Fichier[] = [];
    for (const f of [o, r]) {
      if (f.size > MAX_FICHIER) { erreurs.push(`Photo ${k} trop lourde.`); break; }
      const octets = new Uint8Array(await f.arrayBuffer());
      const type = typeImage(octets);
      if (!type) { erreurs.push(`Photo ${k} : format non reconnu.`); break; }
      fichiers.push({ nom: f.name, type, octets });
    }
    if (fichiers.length === 2) {
      photos.push({ original: fichiers[0], rendu: fichiers[1], granulat: txt(fd.get(`photo${k}_granulat`), 80), granulatNom: txt(fd.get(`photo${k}_granulat_nom`), 120) });
    }
  }
  if (!photos.length && !erreurs.some((e) => e.startsWith('Photo'))) erreurs.push('Au moins une photo est nécessaire.');
  if (erreurs.length) return { ok: false, erreurs };
  return {
    ok: true,
    demande: {
      nom, telephone, email, ville,
      type_surface: txt(fd.get('type_surface'), 60) || null,
      surface_m2: surface,
      delai: txt(fd.get('delai'), 100) || null,
      message: txt(fd.get('message'), 2000) || null,
      photos,
    },
  };
}

export const ext = (type: string) => (type === 'image/png' ? 'png' : type === 'image/webp' ? 'webp' : 'jpg');
