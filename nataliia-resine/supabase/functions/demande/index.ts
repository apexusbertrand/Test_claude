// Fonction Supabase « demande » : reçoit le projet envoyé depuis le simulateur du site.
// 1. contrôle anti-robot (Cloudflare Turnstile) et validation
// 2. photos d'origine et rendus dans le stockage privé « demandes »
// 3. dossier prospect (créé ou mis à jour d'après l'e-mail) et demande
// 4. e-mail d'alerte à Nataliia, e-mail de confirmation au visiteur avec ses rendus (Brevo)
//
// Déployer sans vérification de JWT (appelée par les visiteurs) :
//   supabase functions deploy demande --no-verify-jwt
// Secrets : voir INSTALLATION_ADMIN.md (ALLOWED_ORIGINS, TURNSTILE_SECRET, BREVO_API_KEY, SENDER_EMAIL,
//           ALERT_EMAIL, ADMIN_URL). SUPABASE_URL et SUPABASE_SERVICE_ROLE_KEY sont fournis par Supabase.

import { valider, ext, type Demande } from './valider.ts';

const env = (k: string, d = '') => Deno.env.get(k) ?? d;
const MAX_REQUETE = 16 * 1024 * 1024;

function cors(req: Request) {
  const origin = req.headers.get('Origin') || '';
  const allowed = env('ALLOWED_ORIGINS').split(',').map((s) => s.trim()).filter(Boolean);
  const ok = !allowed.length || allowed.includes(origin);
  return {
    'Access-Control-Allow-Origin': ok ? origin || '*' : allowed[0],
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
    Vary: 'Origin',
  };
}

// Accès base et stockage avec la clé service (côté serveur uniquement)
function sb() {
  const url = env('SUPABASE_URL'); const key = env('SUPABASE_SERVICE_ROLE_KEY');
  const h = { apikey: key, Authorization: `Bearer ${key}` };
  return {
    async rest(path: string, init: RequestInit = {}) {
      const r = await fetch(`${url}/rest/v1/${path}`, { ...init, headers: { ...h, 'Content-Type': 'application/json', Prefer: 'return=representation', ...(init.headers || {}) } });
      if (!r.ok) throw new Error(`base ${r.status} ${await r.text()}`);
      const t = await r.text();
      return t ? JSON.parse(t) : null;
    },
    async upload(path: string, octets: Uint8Array, type: string) {
      const r = await fetch(`${url}/storage/v1/object/demandes/${path}`, { method: 'POST', headers: { ...h, 'Content-Type': type, 'x-upsert': 'false' }, body: octets });
      if (!r.ok) throw new Error(`stockage ${r.status} ${await r.text()}`);
    },
    async remove(paths: string[]) {
      if (!paths.length) return;
      await fetch(`${url}/storage/v1/object/demandes`, { method: 'DELETE', headers: { ...h, 'Content-Type': 'application/json' }, body: JSON.stringify({ prefixes: paths }) });
    },
  };
}

async function turnstileOk(token: string, ip: string) {
  const secret = env('TURNSTILE_SECRET');
  if (!secret) return true; // non configuré : contrôle désactivé (préversion)
  if (!token) return false;
  const body = new FormData();
  body.append('secret', secret); body.append('response', token); if (ip) body.append('remoteip', ip);
  const r = await fetch(env('TURNSTILE_URL', 'https://challenges.cloudflare.com/turnstile/v0/siteverify'), { method: 'POST', body });
  return r.ok && (await r.json()).success === true;
}

const esc = (s: unknown) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]!));
const b64 = (u: Uint8Array) => { let s = ''; for (let i = 0; i < u.length; i += 0x8000) s += String.fromCharCode(...u.subarray(i, i + 0x8000)); return btoa(s); };

async function envoyerMail(msg: Record<string, unknown>) {
  const key = env('BREVO_API_KEY');
  if (!key) return 'non configuré';
  const r = await fetch(env('BREVO_URL', 'https://api.brevo.com/v3/smtp/email'), {
    method: 'POST',
    headers: { 'api-key': key, 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ sender: { email: env('SENDER_EMAIL'), name: env('SENDER_NAME', 'Nataliia Résine') }, ...msg }),
  });
  return r.ok ? 'envoyé' : `erreur ${r.status}`;
}

function mailAlerte(d: Demande, prospectId: string) {
  const lien = `${env('ADMIN_URL')}#/demandes/${prospectId}`;
  return {
    to: [{ email: env('ALERT_EMAIL') }],
    replyTo: { email: d.email, name: d.nom },
    subject: `Nouvelle demande : ${d.nom}, ${d.ville}`,
    htmlContent: `<div style="font-family:Arial,sans-serif;color:#292722;max-width:560px">
      <h2 style="font-family:Georgia,serif;font-weight:normal">Nouvelle demande depuis le site</h2>
      <p><strong>${esc(d.nom)}</strong>, ${esc(d.ville)}<br>Tél. ${esc(d.telephone)} · ${esc(d.email)}</p>
      <p>${esc(d.type_surface || 'Surface non précisée')}${d.surface_m2 ? `, ${esc(d.surface_m2)} m²` : ''}${d.delai ? `, délai : ${esc(d.delai)}` : ''}</p>
      ${d.message ? `<p style="white-space:pre-wrap">${esc(d.message)}</p>` : ''}
      <p>${d.photos.length} photo(s) simulée(s) : ${d.photos.map((p) => esc(p.granulatNom || p.granulat)).join(', ')}.</p>
      <p><a href="${esc(lien)}" style="display:inline-block;background:#b58a50;color:#17130e;padding:10px 16px;text-decoration:none;font-weight:bold;border-radius:6px">Ouvrir le dossier</a></p>
      <p style="color:#6f695f;font-size:12px">Les photos sont dans le dossier (accès sécurisé). Répondre à cet e-mail écrit directement au client.</p></div>`,
  };
}

function mailConfirmation(d: Demande) {
  return {
    to: [{ email: d.email, name: d.nom }],
    replyTo: { email: env('ALERT_EMAIL'), name: 'Nataliia Résine' },
    subject: 'Votre projet a bien été reçu, Nataliia Résine',
    htmlContent: `<div style="font-family:Arial,sans-serif;color:#292722;max-width:560px">
      <h2 style="font-family:Georgia,serif;font-weight:normal">Merci, votre projet est bien arrivé</h2>
      <p>Bonjour ${esc(d.nom)},</p>
      <p>J’ai bien reçu votre demande et vos photos. Je vous recontacte rapidement pour en parler.</p>
      <p>Vous trouverez en pièce jointe ${d.photos.length > 1 ? 'les simulations réalisées' : 'la simulation réalisée'} sur le site. Il s’agit d’un aperçu indicatif : le rendu réel dépend du support, de la lumière et de la pose.</p>
      <p>Nataliia Résine</p>
      <p style="color:#6f695f;font-size:12px">Vos données servent uniquement à répondre à votre demande. Elles sont conservées 3 ans au plus après notre dernier échange. Pour les consulter ou les faire supprimer, répondez simplement à cet e-mail.</p></div>`,
    attachment: d.photos.map((p, i) => ({ name: `simulation-${i + 1}.${ext(p.rendu.type)}`, content: b64(p.rendu.octets) })),
  };
}

export async function traiter(req: Request): Promise<Response> {
  const h = cors(req);
  const reply = (status: number, body: unknown) => new Response(JSON.stringify(body), { status, headers: { ...h, 'Content-Type': 'application/json' } });
  if (req.method === 'OPTIONS') return new Response('ok', { headers: h });
  if (req.method !== 'POST') return reply(405, { erreur: 'Méthode non autorisée.' });
  if (Number(req.headers.get('content-length') || 0) > MAX_REQUETE) return reply(413, { erreur: 'Envoi trop lourd.' });

  let fd: FormData;
  try { fd = await req.formData(); } catch { return reply(400, { erreur: 'Envoi illisible.' }); }

  // Piège à robots : champ invisible rempli = on fait semblant d'accepter
  if (String(fd.get('site_web') || '').trim()) return reply(201, { ok: true });

  const ip = (req.headers.get('x-forwarded-for') || '').split(',')[0].trim();
  if (!(await turnstileOk(String(fd.get('turnstile') || ''), ip))) return reply(400, { erreur: 'Vérification anti-robot échouée. Rechargez la page et réessayez.' });

  const v = await valider(fd);
  if (!v.ok) return reply(400, { erreur: v.erreurs.join(' ') });
  const d = v.demande;
  const db = sb();
  const now = new Date();

  // Dossier prospect : un par adresse e-mail
  const existants = await db.rest(`prospects?select=id,statut&email=eq.${encodeURIComponent(d.email)}`, { method: 'GET' });
  let prospectId: string;
  if (existants.length) {
    prospectId = existants[0].id;
    // Limite : 3 demandes par dossier et par heure
    const recentes = await db.rest(`demandes?select=id&prospect_id=eq.${prospectId}&created_at=gt.${encodeURIComponent(new Date(now.getTime() - 3600e3).toISOString())}`, { method: 'GET' });
    if (recentes.length >= 3) return reply(429, { erreur: 'Plusieurs demandes viennent d’être envoyées. Merci de patienter ou d’appeler directement.' });
  }

  const demandeId = crypto.randomUUID();
  const dossier = `${now.getUTCFullYear()}/${String(now.getUTCMonth() + 1).padStart(2, '0')}/${demandeId}`;
  const envoyes: string[] = [];
  try {
    const photos = [];
    for (let i = 0; i < d.photos.length; i++) {
      const p = d.photos[i];
      const original = `${dossier}/photo${i + 1}-original.${ext(p.original.type)}`;
      const rendu = `${dossier}/photo${i + 1}-rendu.${ext(p.rendu.type)}`;
      await db.upload(original, p.original.octets, p.original.type); envoyes.push(original);
      await db.upload(rendu, p.rendu.octets, p.rendu.type); envoyes.push(rendu);
      photos.push({ original, rendu, produit: p.granulat, produit_nom: p.granulatNom });
    }
    if (existants.length) {
      await db.rest(`prospects?id=eq.${prospectId!}`, {
        method: 'PATCH',
        body: JSON.stringify({ nom: d.nom, telephone: d.telephone, ville: d.ville, dernier_contact: now.toISOString(), ...(existants[0].statut === 'sans_suite' ? { statut: 'nouveau' } : {}) }),
      });
    } else {
      const [p] = await db.rest('prospects', { method: 'POST', body: JSON.stringify({ nom: d.nom, email: d.email, telephone: d.telephone, ville: d.ville, dernier_contact: now.toISOString() }) });
      prospectId = p.id;
    }
    await db.rest('demandes', {
      method: 'POST',
      body: JSON.stringify({ id: demandeId, prospect_id: prospectId!, type_surface: d.type_surface, surface_m2: d.surface_m2, delai: d.delai, message: d.message, photos, consentement_at: now.toISOString() }),
    });
  } catch (e) {
    console.error('demande', e);
    await db.remove(envoyes).catch(() => {});
    return reply(500, { erreur: 'Enregistrement impossible pour le moment. Réessayez ou appelez-nous.' });
  }

  // Les e-mails ne bloquent pas : la demande est enregistrée quoi qu'il arrive
  const [alerte, confirmation] = await Promise.all([
    envoyerMail(mailAlerte(d, prospectId!)).catch((e) => `erreur ${e}`),
    envoyerMail(mailConfirmation(d)).catch((e) => `erreur ${e}`),
  ]);
  if (alerte !== 'envoyé' || confirmation !== 'envoyé') console.warn('e-mails', { alerte, confirmation });
  return reply(201, { ok: true });
}

if (import.meta.main) Deno.serve(traiter);
