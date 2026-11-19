# Nataliia Résine

Site vitrine de Nataliia Résine (moquette de pierre, Somain), son administration et son simulateur avant/après.

- Cahier des charges : `CAHIER_DES_CHARGES.md`
- Questions client : `QUESTIONS_CLIENT.md`
- Mise en service de l'administration et du simulateur : `INSTALLATION_ADMIN.md`

Préversion : https://apexusbertrand.github.io/Test_claude/nataliia-resine/ (noindex). Administration : `/admin/`. Publiés par `.github/workflows/deploy-pages.yml` à chaque push sur `main` et à chaque clic sur « Publier le site » dans l'administration.

## Structure

    site/
      build.mjs          générateur sans dépendance : node build.mjs → ../www/
      fetch-content.mjs  en CI : remplace content/*.json par les contenus publiés dans Supabase
      content/*.json     contenus (valeurs par défaut tant que Supabase n'est pas branché)
      static/            css, js, polices auto-hébergées, images optimisées
      static/js/sim-engine.js   moteur du simulateur (perspective, texture, lumière), sans DOM
      static/js/simulateur.js   interface du simulateur (page projet.html)
      tests/             tests du moteur : node --test tests/*.test.mjs
      tools/images.py    prépare les images depuis assets/maquette (Pillow)
      tools/seed.mjs     génère supabase/seed.sql depuis content/*.json
    admin/               administration (statique, copiée dans www/admin/ au build)
      app.js, api.js     interface et couche de données (Supabase ou démonstration)
      images.js          redimensionnement et compression des photos dans le navigateur
      aide.html          guide d'utilisation pour la cliente
      vendor/            supabase-js 2.45.4 (MIT)
    supabase/
      schema.sql         tables, droits (RLS), historique, stockage
      seed.sql           données initiales
      functions/publier  fonction qui déclenche la publication
      functions/demande  réception des demandes du simulateur (photos privées, dossier, e-mails)
      functions/purger   purge RGPD à 3 ans, photos comprises
      functions/tests    tests Deno : deno test -A supabase/functions/tests/
      emails/            modèles d'e-mails en français
      tests/             tests SQL sur Postgres local (run.sh)
    www/                 site généré (non versionné)
    assets/maquette/     logo et image d'accueil d'origine
    maquette-origine/    copie de la maquette Wix de la cliente

## Travailler en local

    cd site && node build.mjs
    python3 -m http.server -d ../www 8000

Puis http://localhost:8000 (site) et http://localhost:8000/admin/ (administration en mode démonstration).

Tests de la base (Postgres 16 local) :

    PGHOST=... PGPORT=... bash supabase/tests/run.sh

## Modes de l'administration

- Démonstration (par défaut, `admin/config.js` vide) : tout reste dans le navigateur (localStorage), « Publier » ne publie rien. Sert à essayer et à faire tester la cliente.
- Production : en CI, si les variables de dépôt `NR_SUPABASE_URL` et `NR_SUPABASE_ANON_KEY` existent, `build.mjs` écrit `www/admin/config.js` et `fetch-content.mjs` récupère les contenus publiés. Voir `INSTALLATION_ADMIN.md`.

Avant la vraie mise en ligne : `preversion: false` et `url` dans `site/content/site.json`.
