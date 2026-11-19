# Nataliia Résine

Site vitrine de Nataliia Résine (moquette de pierre, Somain). Cahier des charges : `CAHIER_DES_CHARGES.md`. Questions client : `QUESTIONS_CLIENT.md`.

Préversion : https://apexusbertrand.github.io/Test_claude/nataliia-resine/ (noindex), publiée à chaque push sur `main` par `.github/workflows/deploy-pages.yml`.

## Structure

    site/
      build.mjs        générateur sans dépendance : node build.mjs
      content/*.json   contenus (site, prestations, produits, réalisations, avis)
      static/          css, js, polices auto-hébergées, images optimisées
      tools/images.py  prépare les images depuis assets/maquette (Pillow)
    www/               site généré, publié tel quel (à reconstruire après chaque modif)
    assets/maquette/   logo et image d'accueil d'origine
    maquette-origine/  copie de la maquette Wix de la cliente

## Modifier

    cd site && node build.mjs

Puis voir le résultat : `python3 -m http.server -d www 8000`.

Une réalisation dans `content/realisations.json` :

    { "id": "terrasse-douai", "titre": "...", "type": "terrasses", "ville": "Douai",
      "texte": "...", "apres": ["img-sans-extension", ...], "avant": [...], "produits": ["id-produit"] }

Les images vont dans `static/img/` en `.webp` et `.jpg` (même nom). La page Avis n'apparaît que si `avis.json` n'est pas vide.

Avant la vraie mise en ligne : `preversion: false` et `url` dans `content/site.json`.
