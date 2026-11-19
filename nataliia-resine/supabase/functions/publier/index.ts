// Fonction Supabase « publier » : reconstruit le site public.
// POST : vérifie que l'appelant est admin, puis déclenche le workflow GitHub (repository_dispatch).
// GET  : renvoie l'état de la dernière exécution du workflow (en cours, réussi, échoué).
//
// Secrets à définir (supabase secrets set ...) :
//   GITHUB_TOKEN  jeton GitHub à grain fin, dépôt du site uniquement, droits Contents: write et Actions: read
//   GITHUB_REPO   ex. apexusbertrand/Test_claude
// SUPABASE_URL, SUPABASE_ANON_KEY et SUPABASE_SERVICE_ROLE_KEY sont fournis automatiquement.

import { createClient } from 'npm:@supabase/supabase-js@2.45.4';

const WORKFLOW = 'deploy-pages.yml';
const cors = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
};
const reply = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { ...cors, 'Content-Type': 'application/json' } });

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: cors });

  // Qui appelle ?
  const jwt = (req.headers.get('Authorization') || '').replace('Bearer ', '');
  const userClient = createClient(Deno.env.get('SUPABASE_URL')!, Deno.env.get('SUPABASE_ANON_KEY')!, {
    global: { headers: { Authorization: `Bearer ${jwt}` } },
  });
  const { data: { user } } = await userClient.auth.getUser(jwt);
  if (!user) return reply(401, { erreur: 'Non connecté' });
  const { data: estAdmin } = await userClient.rpc('is_admin');
  if (!estAdmin) return reply(403, { erreur: 'Accès réservé' });

  const token = Deno.env.get('GITHUB_TOKEN');
  const repo = Deno.env.get('GITHUB_REPO');
  if (!token || !repo) return reply(500, { erreur: 'Publication non configurée (GITHUB_TOKEN, GITHUB_REPO)' });
  const gh = (path: string, init: RequestInit = {}) =>
    fetch(`https://api.github.com/repos/${repo}${path}`, {
      ...init,
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'application/vnd.github+json',
        'X-GitHub-Api-Version': '2022-11-28',
        'User-Agent': 'nataliia-resine-admin',
        ...(init.headers || {}),
      },
    });

  if (req.method === 'GET') {
    const r = await gh(`/actions/workflows/${WORKFLOW}/runs?per_page=1`);
    if (!r.ok) return reply(502, { erreur: `GitHub ${r.status}` });
    const run = (await r.json()).workflow_runs?.[0];
    return reply(200, run
      ? { statut: run.status, conclusion: run.conclusion, debut: run.run_started_at, fin: run.updated_at }
      : { statut: 'aucune' });
  }

  if (req.method !== 'POST') return reply(405, { erreur: 'Méthode non autorisée' });

  const r = await gh('/dispatches', {
    method: 'POST',
    body: JSON.stringify({ event_type: 'nataliia-publier', client_payload: { par: user.email } }),
  });
  const admin = createClient(Deno.env.get('SUPABASE_URL')!, Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!);
  await admin.from('publications').insert({
    demandee_par: user.email,
    statut: r.ok ? 'envoyee' : 'erreur',
    detail: r.ok ? null : `GitHub ${r.status} ${await r.text()}`,
  });
  if (!r.ok) return reply(502, { erreur: `GitHub a refusé la demande (${r.status})` });
  return reply(202, { ok: true });
});
