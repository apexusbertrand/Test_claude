# Administration Nataliia Résine : mise en service

Procédure à suivre une fois pour brancher l'administration (lot 2) sur une vraie base. Tant qu'elle n'est pas faite, `/admin/` fonctionne en mode démonstration : les modifications restent dans le navigateur et rien n'est publié.

Durée : environ 45 minutes. Comptes nécessaires : Supabase (gratuit), accès administrateur au dépôt GitHub.

## Architecture

```
Navigateur de la cliente ── /admin/ (statique, sur GitHub Pages)
        │  connexion e-mail + mot de passe, lecture/écriture
        ▼
Supabase (UE, Francfort) : Auth, Postgres + règles d'accès, Storage (photos), fonction « publier »
        │  bouton « Publier le site » → repository_dispatch
        ▼
GitHub Actions : fetch-content.mjs (contenus publiés) → build.mjs → GitHub Pages
```

Le site public reste 100 % statique. Les photos du site sont servies depuis le stockage public Supabase. Les données des prospects (lot 3) restent dans un stockage privé.

## 1. Créer le projet Supabase

1. supabase.com, « New project ». Nom : `nataliia-resine`. Région : **Central EU (Frankfurt)**. Mot de passe de base : le garder dans le gestionnaire de mots de passe.
2. Settings > API : noter **Project URL** et la clé **anon public**.

## 2. Créer la base

SQL Editor, coller puis exécuter dans l'ordre :

1. `supabase/schema.sql` (tables, règles d'accès, historique, buckets de stockage) ;
2. `supabase/seed.sql` (prestations et produits d'exemple du lot 1).

Les deux scripts sont relançables sans perte de données. Ils sont testés sur Postgres 16 avec `supabase/tests/run.sh` (droits visiteur, compte non admin, admin, contraintes, purge RGPD).

Purge RGPD automatique (Database > Extensions : activer `pg_cron`), puis :

```sql
select cron.schedule('purge-prospects', '17 3 * * *', 'select public.purger_prospects()');
```

## 3. Réglages de connexion

Authentication > Sign In / Providers :

- **Allow new users to sign up : désactivé** (seules les personnes invitées peuvent se connecter) ;
- Email : activé, « Confirm email » activé.

Authentication > URL Configuration :

- Site URL : `https://apexusbertrand.github.io/Test_claude/nataliia-resine/admin/` (puis l'adresse définitive après le passage sur le nom de domaine) ;
- Redirect URLs : la même adresse.

Authentication > Emails : remplacer les modèles « Invite user » et « Reset password » par ceux de `supabase/emails/` (en français).

Mot de passe : Authentication > Providers > Email, longueur minimale 10 caractères.

## 4. Créer le compte de la cliente

1. Authentication > Users > « Invite user » : e-mail de la cliente. Elle reçoit un lien pour choisir son mot de passe.
2. Lui donner l'accès à l'administration (SQL Editor) :

```sql
insert into public.admins (user_id, email)
select id, email from auth.users where email = 'adresse@de-la-cliente.fr';
```

Même procédure plus tard pour un deuxième utilisateur. Pour retirer un accès : supprimer la ligne dans `admins` (et l'utilisateur dans Auth).

## 5. Fonction « publier »

Jeton GitHub à grain fin (github.com > Settings > Developer settings > Fine-grained tokens) :

- Repository access : uniquement le dépôt du site ;
- Permissions : **Contents : Read and write** (nécessaire pour `repository_dispatch`), **Actions : Read** (état de la publication) ;
- Expiration : 1 an, avec un rappel pour le renouveler.

Puis, avec la CLI Supabase (`npx supabase`) depuis `nataliia-resine/` :

```bash
npx supabase login
```

```bash
npx supabase link --project-ref <ref-du-projet>
```

```bash
npx supabase secrets set GITHUB_TOKEN=<jeton> GITHUB_REPO=apexusbertrand/Test_claude
```

```bash
npx supabase functions deploy publier
```

La fonction vérifie elle-même que l'appelant est administrateur. Elle n'a jamais besoin d'être appelée sans connexion.

## 6. Brancher le site

GitHub, dépôt du site > Settings > Secrets and variables > Actions > onglet **Variables** (pas Secrets : ces valeurs sont publiques par conception) :

- `NR_SUPABASE_URL` : Project URL ;
- `NR_SUPABASE_ANON_KEY` : clé anon public.

Relancer le workflow « Deploy client sites to GitHub Pages » (Actions > Run workflow). À partir de là :

- le site est construit avec les contenus publiés dans Supabase ;
- `/admin/config.js` est généré avec ces valeurs : l'administration sort du mode démonstration.

## 7. Vérifier

1. Ouvrir `/admin/`, se connecter avec le compte de la cliente (ou un compte de test ajouté à `admins`).
2. Créer une réalisation avec 2 photos, l'enregistrer, cliquer « Publier le site ».
3. Dans GitHub Actions, un run « repository_dispatch » démarre. 2 minutes plus tard, la réalisation est sur le site, photos servies depuis `…supabase.co/storage/v1/object/public/media/…`.
4. Historique : « Revenir à cette version » sur la création, republier, la réalisation disparaît.
5. Se connecter avec un compte qui n'est pas dans `admins` : refus « Ce compte n'a pas accès ».

## Points d'attention

- **Mise en pause** : un projet Supabase gratuit sans activité pendant 7 jours est mis en pause. Le site public n'est pas touché (il est statique), mais l'administration et la publication ne répondent plus jusqu'à la relance dans le tableau de bord. Si la cliente publie peu, passer à l'offre Pro (25 $/mois) ou programmer une requête hebdomadaire.
- **Échec de récupération** : si Supabase ne répond pas pendant un build, le workflow échoue et le site en ligne reste dans son état précédent. Cela bloque aussi le déploiement du site Guillaume l'Échanson tant que Supabase ne répond pas : à garder en tête tant que les deux sites partagent ce workflow.
- **Jeton GitHub** : expire au bout d'un an. Symptôme : « La publication n'a pas pu être lancée ». Le renouveler puis `supabase secrets set GITHUB_TOKEN=...`.
- **Nom de domaine** : au changement d'adresse, mettre à jour `url` dans `site/content/site.json`, la Site URL et les Redirect URLs Supabase.
- **Sauvegardes** : l'offre gratuite n'a pas de sauvegarde à un instant donné. Exporter la base de temps en temps (Database > Backups, ou `pg_dump`). L'historique des modifications permet déjà d'annuler une erreur de saisie.
