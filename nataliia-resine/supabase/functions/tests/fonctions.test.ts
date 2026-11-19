// Tests des fonctions « demande » et « purger » contre un faux Supabase (REST + stockage),
// un faux Brevo et un faux Turnstile. Lancer : deno test -A supabase/functions/tests/
import { assert, assertEquals } from 'jsr:@std/assert@1';

// --- Faux services --------------------------------------------------------
type Row = Record<string, any>;
const db: { prospects: Row[]; demandes: Row[] } = { prospects: [], demandes: [] };
const files = new Map<string, number>();
const mails: any[] = [];
let failDemandes = false;
let failBrevo = false;

const mock = Deno.serve({ port: 0, onListen() {} }, async (req) => {
  const u = new URL(req.url);
  const json = (b: unknown, s = 200) => new Response(JSON.stringify(b), { status: s, headers: { 'content-type': 'application/json' } });
  if (u.pathname === '/turnstile') { const f = await req.formData(); return json({ success: f.get('response') === 'jeton-valide' }); }
  if (u.pathname === '/brevo') { if (failBrevo) return json({}, 500); mails.push(await req.json()); return json({ messageId: 'x' }, 201); }
  if (req.headers.get('apikey') !== 'service') return json({ message: 'clé' }, 401);
  if (u.pathname.startsWith('/storage/v1/object/demandes')) {
    if (req.method === 'POST') { files.set(u.pathname.replace('/storage/v1/object/demandes/', ''), (await req.arrayBuffer()).byteLength); return json({}); }
    if (req.method === 'DELETE') { (await req.json()).prefixes.forEach((p: string) => files.delete(p)); return json([]); }
  }
  const m = u.pathname.match(/^\/rest\/v1\/(\w+)$/);
  if (m) {
    const t = m[1] as 'prospects' | 'demandes';
    const filt = (r: Row) => [...u.searchParams].every(([k, v]) => {
      if (k === 'select') return true;
      const [op, ...rest] = v.split('.'); const val = rest.join('.');
      if (op === 'eq') return String(r[k]) === val;
      if (op === 'neq') return String(r[k]) !== val;
      if (op === 'gt') return r[k] > val;
      if (op === 'lt') return r[k] < val;
      if (op === 'in') return val.slice(1, -1).split(',').includes(r[k]);
      return true;
    });
    if (req.method === 'GET') {
      const rows = db[t].filter(filt).map((r) => (u.searchParams.get('select')?.includes('demandes(')
        ? { ...r, demandes: db.demandes.filter((d) => d.prospect_id === r.id) } : r));
      return json(rows);
    }
    if (req.method === 'POST') {
      if (t === 'demandes' && failDemandes) return json({ message: 'panne' }, 500);
      const row = { id: crypto.randomUUID(), statut: 'nouveau', created_at: new Date().toISOString(), ...(await req.json()) };
      if (t === 'prospects' && db.prospects.some((p) => p.email === row.email)) return json({ code: '23505' }, 409);
      db[t].push(row); return json([row], 201);
    }
    if (req.method === 'PATCH') { const b = await req.json(); db[t].filter(filt).forEach((r) => Object.assign(r, b)); return json([]); }
    if (req.method === 'DELETE') {
      const del = db[t].filter(filt); db[t] = db[t].filter((r) => !del.includes(r));
      if (t === 'prospects') db.demandes = db.demandes.filter((d) => !del.some((p) => p.id === d.prospect_id));
      return json([]);
    }
  }
  return json({ message: 'inconnu ' + u.pathname }, 404);
});
const base = `http://localhost:${mock.addr.port}`;
Object.entries({
  SUPABASE_URL: base, SUPABASE_SERVICE_ROLE_KEY: 'service', BREVO_API_KEY: 'brevo', BREVO_URL: `${base}/brevo`,
  SENDER_EMAIL: 'site@nataliia-resine.test', ALERT_EMAIL: 'nataliia@test.fr', ADMIN_URL: 'https://site.test/admin/',
  TURNSTILE_URL: `${base}/turnstile`, ALLOWED_ORIGINS: 'https://site.test', CRON_SECRET: 'nuit',
}).forEach(([k, v]) => Deno.env.set(k, v));

// Les appels fetch vers le faux serveur gardent des connexions ouvertes entre deux tests : normal ici
const test = (name: string, fn: () => Promise<void>) => Deno.test({ name, fn, sanitizeOps: false, sanitizeResources: false });

const { traiter } = await import('../demande/index.ts');
const { purger } = await import('../purger/index.ts');

// --- Requêtes de test ------------------------------------------------------
const JPEG = new Uint8Array([0xff, 0xd8, 0xff, 0xe0, ...new Array(200).fill(7)]);
function form(over: Record<string, string | null> = {}, photos = 2, bytes = JPEG) {
  const fd = new FormData();
  const vals: Record<string, string | null> = { nom: 'Jean Testeur', telephone: '06 12 34 56 78', email: 'Jean@Example.TEST', ville: 'Douai', type_surface: 'Terrasses', surface_m2: '35', delai: 'printemps', message: 'Terrasse béton', consentement: 'oui', site_web: '', ...over };
  Object.entries(vals).forEach(([k, v]) => v !== null && fd.append(k, v));
  for (let k = 1; k <= photos; k++) {
    fd.append(`photo${k}_original`, new File([bytes], `o${k}.jpg`, { type: 'image/jpeg' }));
    fd.append(`photo${k}_rendu`, new File([bytes], `r${k}.jpg`, { type: 'image/jpeg' }));
    fd.append(`photo${k}_granulat`, 'marbre-blanc'); fd.append(`photo${k}_granulat_nom`, 'Marbre blanc');
  }
  return fd;
}
const post = (fd: FormData, origin = 'https://site.test') => traiter(new Request('https://f.test/demande', { method: 'POST', body: fd, headers: { Origin: origin } }));
const reset = () => { db.prospects = []; db.demandes = []; files.clear(); mails.length = 0; failDemandes = false; failBrevo = false; Deno.env.delete('TURNSTILE_SECRET'); };

test('demande valide : photos privées, dossier, demande, 2 e-mails', async () => {
  reset();
  const r = await post(form());
  assertEquals(r.status, 201);
  assertEquals(r.headers.get('Access-Control-Allow-Origin'), 'https://site.test');
  assertEquals(files.size, 4);
  assertEquals(db.prospects.length, 1);
  assertEquals(db.prospects[0].email, 'jean@example.test');
  const d = db.demandes[0];
  assertEquals(d.photos.length, 2);
  assert(files.has(d.photos[0].original) && files.has(d.photos[1].rendu));
  assertEquals(d.photos[0].produit_nom, 'Marbre blanc');
  assert(d.consentement_at);
  assertEquals(mails.length, 2);
  const alerte = mails.find((m) => m.to[0].email === 'nataliia@test.fr');
  assert(alerte.htmlContent.includes(`#/demandes/${db.prospects[0].id}`));
  assertEquals(alerte.replyTo.email, 'jean@example.test');
  const conf = mails.find((m) => m.to[0].email === 'jean@example.test');
  assertEquals(conf.attachment.length, 2);
});

test('même client : dossier réutilisé, « sans suite » repasse en « nouveau »', async () => {
  reset();
  await post(form());
  db.prospects[0].statut = 'sans_suite';
  const r = await post(form({ email: 'jean@example.test', ville: 'Lille' }, 1));
  assertEquals(r.status, 201);
  assertEquals(db.prospects.length, 1);
  assertEquals(db.prospects[0].statut, 'nouveau');
  assertEquals(db.prospects[0].ville, 'Lille');
  assertEquals(db.demandes.length, 2);
});

test('limite : 4ᵉ demande dans l’heure refusée', async () => {
  reset();
  for (let i = 0; i < 3; i++) assertEquals((await post(form({}, 1))).status, 201);
  assertEquals((await post(form({}, 1))).status, 429);
  assertEquals(db.demandes.length, 3);
});

test('robot (champ piège rempli) : réponse normale, rien enregistré', async () => {
  reset();
  assertEquals((await post(form({ site_web: 'http://spam' }))).status, 201);
  assertEquals(db.prospects.length + files.size + mails.length, 0);
});

test('données invalides : refus et rien enregistré', async () => {
  reset();
  const r = await post(form({ consentement: null, telephone: 'abc', email: 'pas-un-mail' }));
  assertEquals(r.status, 400);
  const { erreur } = await r.json();
  assert(/Téléphone/.test(erreur) && /E-mail/.test(erreur) && /Accord/.test(erreur));
  const faux = await post(form({}, 1, new TextEncoder().encode('<script>pas une image</script>')));
  assertEquals(faux.status, 400);
  assertEquals((await post(form({}, 3))).status, 400);
  assertEquals((await post(form({}, 0))).status, 400);
  assertEquals(db.prospects.length + files.size, 0);
});

test('anti-robot Turnstile : jeton refusé puis accepté', async () => {
  reset();
  Deno.env.set('TURNSTILE_SECRET', 'secret');
  assertEquals((await post(form({ turnstile: 'faux' }))).status, 400);
  assertEquals((await post(form({ turnstile: 'jeton-valide' }))).status, 201);
  Deno.env.delete('TURNSTILE_SECRET');
});

test('panne de la base : erreur et photos déjà envoyées supprimées', async () => {
  reset();
  failDemandes = true;
  assertEquals((await post(form())).status, 500);
  assertEquals(files.size, 0);
});

test('panne des e-mails : la demande reste enregistrée', async () => {
  reset();
  failBrevo = true;
  assertEquals((await post(form())).status, 201);
  assertEquals(db.demandes.length, 1);
});

test('origine non autorisée : le navigateur ne pourra pas lire la réponse', async () => {
  reset();
  const r = await post(form(), 'https://autre-site.test');
  assertEquals(r.headers.get('Access-Control-Allow-Origin'), 'https://site.test');
  await r.body?.cancel();
});

test('purge RGPD : dossiers de plus de 3 ans et leurs photos, sauf clients signés', async () => {
  reset();
  await post(form({ email: 'vieux@test.fr' }));
  await post(form({ email: 'signe@test.fr' }));
  await post(form({ email: 'recent@test.fr' }));
  const vieux = '2020-01-01T00:00:00.000Z';
  db.prospects.find((p) => p.email === 'vieux@test.fr')!.dernier_contact = vieux;
  Object.assign(db.prospects.find((p) => p.email === 'signe@test.fr')!, { dernier_contact: vieux, statut: 'signe' });
  assertEquals((await purger(new Request('https://f.test/purger', { method: 'POST', headers: { 'x-cron-secret': 'faux' } }))).status, 403);
  const r = await purger(new Request('https://f.test/purger', { method: 'POST', headers: { 'x-cron-secret': 'nuit' } }));
  assertEquals(await r.json(), { dossiers: 1, photos: 4 });
  assertEquals(db.prospects.map((p) => p.email).sort(), ['recent@test.fr', 'signe@test.fr']);
  assertEquals(files.size, 8);
});

Deno.test({ name: 'arrêt du faux serveur', fn: async () => { await mock.shutdown(); }, sanitizeOps: false, sanitizeResources: false });
