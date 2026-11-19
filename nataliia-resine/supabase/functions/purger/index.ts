// Fonction Supabase « purger » : suppression RGPD des dossiers prospects
// sans contact depuis 3 ans (sauf clients signés), photos comprises.
// Appelée chaque nuit par pg_cron (voir INSTALLATION_ADMIN.md), avec l'en-tête x-cron-secret.
// Déployer : supabase functions deploy purger --no-verify-jwt   (secret : CRON_SECRET)

const env = (k: string) => Deno.env.get(k) ?? '';

export async function purger(req: Request): Promise<Response> {
  const secret = env('CRON_SECRET');
  if (!secret || req.headers.get('x-cron-secret') !== secret) return new Response('Accès refusé', { status: 403 });

  const url = env('SUPABASE_URL'); const key = env('SUPABASE_SERVICE_ROLE_KEY');
  const h = { apikey: key, Authorization: `Bearer ${key}`, 'Content-Type': 'application/json' };
  const limite = new Date(Date.now() - 3 * 365.25 * 24 * 3600e3).toISOString();

  const r = await fetch(`${url}/rest/v1/prospects?select=id,demandes(photos)&statut=neq.signe&dernier_contact=lt.${encodeURIComponent(limite)}`, { headers: h });
  if (!r.ok) return new Response(`base ${r.status}`, { status: 502 });
  const dossiers: { id: string; demandes: { photos: { original?: string; rendu?: string }[] }[] }[] = await r.json();
  if (!dossiers.length) return Response.json({ dossiers: 0, photos: 0 });

  const chemins = dossiers.flatMap((p) => p.demandes.flatMap((d) => (d.photos || []).flatMap((ph) => [ph.original, ph.rendu]))).filter(Boolean) as string[];
  // Photos d'abord : si leur suppression échoue, on garde le dossier pour réessayer la nuit suivante
  for (let i = 0; i < chemins.length; i += 100) {
    const s = await fetch(`${url}/storage/v1/object/demandes`, { method: 'DELETE', headers: h, body: JSON.stringify({ prefixes: chemins.slice(i, i + 100) }) });
    if (!s.ok) return new Response(`stockage ${s.status}`, { status: 502 });
  }
  const ids = dossiers.map((p) => p.id).join(',');
  const d = await fetch(`${url}/rest/v1/prospects?id=in.(${ids})`, { method: 'DELETE', headers: h });
  if (!d.ok) return new Response(`base ${d.status}`, { status: 502 });
  return Response.json({ dossiers: dossiers.length, photos: chemins.length });
}

if (import.meta.main) Deno.serve(purger);
