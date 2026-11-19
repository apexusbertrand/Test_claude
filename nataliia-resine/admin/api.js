// Couche de données de l'administration.
// Deux implémentations avec la même interface :
//  - SupabaseApi : production (base, stockage, connexion, publication)
//  - DemoApi     : démonstration, tout reste dans le navigateur (localStorage)

const CONTENT_TABLES = ['prestations', 'produits', 'realisations', 'avis'];
const ORDER = {
  prestations: [['ordre', true], ['created_at', true]],
  produits: [['ordre', true], ['created_at', true]],
  realisations: [['ordre', true], ['created_at', false]],
  avis: [['ordre', true], ['created_at', false]],
  prospects: [['created_at', false]],
  demandes: [['created_at', false]],
};

const uuid = () => (crypto.randomUUID ? crypto.randomUUID()
  : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0; return (c === 'x' ? r : (r & 3) | 8).toString(16);
  }));

export function slugify(s) {
  return String(s).normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
    .replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 60) || 'element';
}

// Chemins de fichiers dans le bucket « media » : realisations/2026/uuid.webp
export function newMediaPath(folder, ext) {
  return `${folder}/${new Date().getFullYear()}/${uuid()}.${ext}`;
}

// ---------------------------------------------------------------------------
class SupabaseApi {
  constructor(url, key) {
    this.mode = 'supabase';
    this.url = url;
    this.sb = window.supabase.createClient(url, key, { auth: { persistSession: true, detectSessionInUrl: true } });
    this.mediaBase = `${url}/storage/v1/object/public/media/`;
  }

  // Connexion
  async session() {
    const { data } = await this.sb.auth.getSession();
    return data.session ? { email: data.session.user.email } : null;
  }
  onAuth(cb) { this.sb.auth.onAuthStateChange((event, s) => cb(event, s ? { email: s.user.email } : null)); }
  async login(email, password) {
    const { error } = await this.sb.auth.signInWithPassword({ email, password });
    if (error) throw new Error(error.message === 'Invalid login credentials' ? 'E-mail ou mot de passe incorrect.' : error.message);
  }
  async logout() { await this.sb.auth.signOut(); }
  async resetPassword(email) {
    const { error } = await this.sb.auth.resetPasswordForEmail(email, { redirectTo: location.href.split('#')[0] });
    if (error) throw error;
  }
  async updatePassword(password) {
    const { error } = await this.sb.auth.updateUser({ password });
    if (error) throw error;
  }
  async isAdmin() {
    const { data, error } = await this.sb.rpc('is_admin');
    if (error) throw error;
    return data === true;
  }

  // Données
  async list(table) {
    let q = this.sb.from(table).select('*');
    for (const [col, asc] of ORDER[table] || []) q = q.order(col, { ascending: asc, nullsFirst: false });
    const { data, error } = await q;
    if (error) throw error;
    return data;
  }
  async get(table, id) {
    const { data, error } = await this.sb.from(table).select('*').eq('id', id).maybeSingle();
    if (error) throw error;
    return data;
  }
  async save(table, row) {
    const clean = { ...row };
    delete clean.created_at; delete clean.updated_at;
    const req = clean.id
      ? this.sb.from(table).update(clean).eq('id', clean.id).select().single()
      : this.sb.from(table).insert(clean).select().single();
    const { data, error } = await req;
    if (error) throw new Error(error.code === '23505' ? 'Cet élément existe déjà (nom en double).' : error.message);
    return data;
  }
  async remove(table, id) {
    const { error } = await this.sb.from(table).delete().eq('id', id);
    if (error) throw error;
  }
  async reorder(table, ids) {
    for (let i = 0; i < ids.length; i++) {
      const { error } = await this.sb.from(table).update({ ordre: i }).eq('id', ids[i]);
      if (error) throw error;
    }
  }
  async getSettings() {
    const { data, error } = await this.sb.from('reglages').select('valeur').eq('cle', 'site').maybeSingle();
    if (error) throw error;
    return data?.valeur || {};
  }
  async saveSettings(valeur) {
    const { error } = await this.sb.from('reglages').upsert({ cle: 'site', valeur });
    if (error) throw error;
  }

  // Fichiers
  async upload(path, blob) {
    const { error } = await this.sb.storage.from('media').upload(path, blob, { contentType: blob.type, cacheControl: '31536000', upsert: false });
    if (error) throw new Error('Envoi du fichier impossible : ' + error.message);
    return path;
  }
  async removeFiles(paths) {
    if (!paths.length) return;
    await this.sb.storage.from('media').remove(paths);
  }
  url(m) {
    if (!m) return '';
    if (m.static) return `../img/${m.static}.webp`;
    return this.mediaBase + (m.small || m.path);
  }
  fullUrl(m) {
    if (!m) return '';
    if (m.static) return `../img/${m.static}.webp`;
    return this.mediaBase + m.path;
  }
  async removePrivateFiles(paths) {
    if (!paths.length) return;
    const { error } = await this.sb.storage.from('demandes').remove(paths);
    if (error) throw new Error('Suppression des photos impossible : ' + error.message);
  }
  async privateUrl(path) {
    const { data, error } = await this.sb.storage.from('demandes').createSignedUrl(path, 600);
    if (error) throw error;
    return data.signedUrl;
  }

  // Historique et publication
  async history(limit = 100) {
    const { data, error } = await this.sb.from('historique').select('*').order('at', { ascending: false }).limit(limit);
    if (error) throw error;
    return data;
  }
  async restore(entry) {
    if (entry.action === 'INSERT') return this.remove(entry.table_name, entry.row_id);
    const { error } = await this.sb.from(entry.table_name).upsert(entry.avant);
    if (error) throw error;
  }
  async pending() {
    const [h, p] = await Promise.all([
      this.sb.from('historique').select('at').order('at', { ascending: false }).limit(1),
      this.sb.from('publications').select('at').eq('statut', 'envoyee').order('at', { ascending: false }).limit(1),
    ]);
    const last = h.data?.[0]?.at;
    const pub = p.data?.[0]?.at;
    return { changes: !!last && (!pub || last > pub), lastPublication: pub || null };
  }
  async publish() {
    const { data, error } = await this.sb.functions.invoke('publier', { method: 'POST' });
    if (error) throw new Error('La publication n’a pas pu être lancée. Réessayez dans un instant.');
    return data;
  }
  async publishStatus() {
    const { data, error } = await this.sb.functions.invoke('publier', { method: 'GET' });
    if (error) return null;
    return data;
  }
}

// ---------------------------------------------------------------------------
class DemoApi {
  constructor() {
    this.mode = 'demo';
    this.KEY = 'nr-admin-demo-v1';
    this.FILES = 'nr-admin-demo-files-v1';
    this.listeners = [];
  }
  load() {
    try { return JSON.parse(localStorage.getItem(this.KEY)); } catch { return null; }
  }
  store(db) {
    try { localStorage.setItem(this.KEY, JSON.stringify(db)); }
    catch { throw new Error('Mémoire du navigateur pleine (mode démonstration). Supprimez quelques photos.'); }
  }
  files() { try { return JSON.parse(localStorage.getItem(this.FILES)) || {}; } catch { return {}; } }
  async init() {
    if (this.load()) return;
    const seed = await (await fetch('demo-seed.json')).json();
    const now = new Date().toISOString();
    const st = (name, alt) => ({ static: name, alt });
    const db = {
      session: null,
      reglages: {
        telephone: seed.site.contact.telephone, email: seed.site.contact.email, whatsapp: seed.site.contact.whatsapp,
        zone: seed.site.zone, zoneTexte: seed.site.zoneTexte, accroche: seed.site.accroche, accueil: null, logo: null,
      },
      prestations: seed.prestations.map((p, i) => ({ id: uuid(), slug: p.id, titre: p.titre, resume: p.resume, texte: p.texte, photo: null, ordre: i, publie: true, created_at: now })),
      produits: seed.produits.map((p, i) => ({
        id: uuid(), slug: p.id, nom: p.nom, categorie: p.categorie, descriptif: p.descriptif,
        photos: (p.photos || []).map((ph) => st(ph, p.nom)), texture: p.texture ? st(p.texture, 'Texture ' + p.nom) : null,
        grain_mm: p.grainMm, lien: p.lien, pdf: null, exemple: !!p.exemple, ordre: i, publie: true, created_at: now,
      })),
      realisations: [], avis: [], prospects: [], demandes: [], historique: [], publications: [],
    };
    this.store(db);
  }
  emit(event, s) { this.listeners.forEach((cb) => cb(event, s)); }

  async session() { await this.init(); return this.load().session; }
  onAuth(cb) { this.listeners.push(cb); }
  async login(email, password) {
    if (!email || !password) throw new Error('Saisissez un e-mail et un mot de passe.');
    const db = this.load(); db.session = { email }; this.store(db);
    this.emit('SIGNED_IN', db.session);
  }
  async logout() { const db = this.load(); db.session = null; this.store(db); this.emit('SIGNED_OUT', null); }
  async resetPassword() {}
  async updatePassword() {}
  async isAdmin() { return true; }

  sortRows(table, rows) {
    const keys = ORDER[table] || [];
    return [...rows].sort((a, b) => {
      for (const [col, asc] of keys) {
        if (a[col] === b[col]) continue;
        return (a[col] > b[col] ? 1 : -1) * (asc ? 1 : -1);
      }
      return 0;
    });
  }
  journal(db, table, rowId, action, avant, apres) {
    db.historique.unshift({ id: Date.now() + Math.random(), table_name: table, row_id: rowId, action, avant, apres, auteur: db.session?.email || 'démo', at: new Date().toISOString() });
    db.historique = db.historique.slice(0, 200);
  }
  // Demandes envoyées depuis le simulateur du site en mode démonstration
  importInbox() {
    let inbox;
    try { inbox = JSON.parse(localStorage.getItem('nr-demo-inbox')) || []; } catch { inbox = []; }
    if (!inbox.length) return;
    const db = this.load();
    const files = this.files();
    for (const it of inbox) {
      let p = db.prospects.find((x) => x.email.toLowerCase() === String(it.prospect.email).toLowerCase());
      if (p) Object.assign(p, { nom: it.prospect.nom, telephone: it.prospect.telephone, ville: it.prospect.ville, dernier_contact: it.at, statut: p.statut === 'sans_suite' ? 'nouveau' : p.statut });
      else {
        p = { id: uuid(), ...it.prospect, statut: 'nouveau', notes: '', dernier_contact: it.at, created_at: it.at, updated_at: it.at };
        db.prospects.push(p);
      }
      db.demandes.push({
        id: it.id, prospect_id: p.id, ...it.demande, consentement_at: it.at, created_at: it.at,
        photos: it.photos.map((ph) => ({ original: ph.original, rendu: ph.rendu, produit: ph.produit, produit_nom: ph.produit_nom })),
      });
      it.photos.forEach((ph) => { files[ph.original] = ph.data[0]; files[ph.rendu] = ph.data[1]; });
    }
    this.store(db);
    localStorage.setItem(this.FILES, JSON.stringify(files));
    localStorage.removeItem('nr-demo-inbox');
  }
  async list(table) { await this.init(); this.importInbox(); return this.sortRows(table, this.load()[table] || []); }
  async get(table, id) { this.importInbox(); return (this.load()[table] || []).find((r) => r.id === id) || null; }
  async save(table, row) {
    const db = this.load();
    const rows = db[table];
    const now = new Date().toISOString();
    if (row.slug && rows.some((r) => r.slug === row.slug && r.id !== row.id)) throw new Error('Cet élément existe déjà (nom en double).');
    let saved;
    if (row.id) {
      const i = rows.findIndex((r) => r.id === row.id);
      const avant = rows[i];
      saved = { ...avant, ...row, updated_at: now };
      rows[i] = saved;
      if (CONTENT_TABLES.includes(table)) this.journal(db, table, row.id, 'UPDATE', avant, saved);
    } else {
      saved = { ...row, id: uuid(), created_at: now, updated_at: now };
      rows.push(saved);
      if (CONTENT_TABLES.includes(table)) this.journal(db, table, saved.id, 'INSERT', null, saved);
    }
    this.store(db);
    return saved;
  }
  async remove(table, id) {
    const db = this.load();
    const avant = db[table].find((r) => r.id === id);
    db[table] = db[table].filter((r) => r.id !== id);
    if (table === 'prospects') db.demandes = db.demandes.filter((d) => d.prospect_id !== id);
    if (avant && CONTENT_TABLES.includes(table)) this.journal(db, table, id, 'DELETE', avant, null);
    this.store(db);
  }
  async reorder(table, ids) {
    const db = this.load();
    ids.forEach((id, i) => { const r = db[table].find((x) => x.id === id); if (r) r.ordre = i; });
    this.store(db);
  }
  async getSettings() { await this.init(); return this.load().reglages; }
  async saveSettings(v) {
    const db = this.load();
    this.journal(db, 'reglages', 'site', 'UPDATE', { cle: 'site', valeur: db.reglages }, { cle: 'site', valeur: v });
    db.reglages = v; this.store(db);
  }

  async upload(path, blob) {
    const data = await new Promise((ok, ko) => { const r = new FileReader(); r.onload = () => ok(r.result); r.onerror = ko; r.readAsDataURL(blob); });
    const f = this.files(); f[path] = data;
    try { localStorage.setItem(this.FILES, JSON.stringify(f)); }
    catch { throw new Error('Mémoire du navigateur pleine (mode démonstration). Supprimez quelques photos.'); }
    return path;
  }
  async removeFiles(paths) {
    const f = this.files(); paths.forEach((p) => delete f[p]);
    localStorage.setItem(this.FILES, JSON.stringify(f));
  }
  url(m) {
    if (!m) return '';
    if (m.static) return `../img/${m.static}.webp`;
    const f = this.files();
    return f[m.small] || f[m.path] || '';
  }
  fullUrl(m) {
    if (!m) return '';
    if (m.static) return `../img/${m.static}.webp`;
    return this.files()[m.path] || '';
  }
  async privateUrl(path) { return this.files()[path] || ''; }
  async removePrivateFiles(paths) { return this.removeFiles(paths); }

  async history(limit = 100) { return this.load().historique.slice(0, limit); }
  async restore(entry) {
    const db = this.load();
    if (entry.table_name === 'reglages') { db.reglages = entry.avant.valeur; this.store(db); return; }
    const rows = db[entry.table_name];
    const i = rows.findIndex((r) => r.id === entry.row_id);
    if (entry.action === 'INSERT') { if (i >= 0) rows.splice(i, 1); }
    else if (i >= 0) rows[i] = entry.avant; else rows.push(entry.avant);
    this.journal(db, entry.table_name, entry.row_id, 'UPDATE', null, entry.avant);
    this.store(db);
  }
  async pending() {
    const db = this.load();
    const last = db.historique[0]?.at;
    const pub = db.publications[0]?.at;
    return { changes: !!last && (!pub || last > pub), lastPublication: pub || null };
  }
  async publish() {
    const db = this.load();
    db.publications.unshift({ at: new Date().toISOString() }); this.store(db);
    return { ok: true, demo: true };
  }
  async publishStatus() { return { statut: 'completed', conclusion: 'success', demo: true }; }

  reset() { localStorage.removeItem(this.KEY); localStorage.removeItem(this.FILES); }
}

export async function createApi() {
  const cfg = window.NR_CONFIG || {};
  if (cfg.supabaseUrl && cfg.supabaseAnonKey) return new SupabaseApi(cfg.supabaseUrl.replace(/\/$/, ''), cfg.supabaseAnonKey);
  const api = new DemoApi();
  await api.init();
  return api;
}
