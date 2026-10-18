# Site de Guillaume l'Échanson

Site vitrine (6 pages) pour Guillaume l'Échanson, ancien sommelier devenu vigneron et négociant-vinificateur à Chassagne-Montrachet (Bourgogne). Le site est un projet à soumettre à Guillaume. Une préversion de validation est en ligne sur GitHub Pages (https://apexusbertrand.github.io/Test_claude/), déployée à chaque push sur `main` par `.github/workflows/deploy-pages.yml`. Elle est en `noindex` (balise meta dans chaque page + `robots.txt`) : à retirer à la vraie mise en ligne.

Instagram : @the_wine_nomad (https://www.instagram.com/the_wine_nomad/). C'est le seul contact réel pour l'instant.

## Stack

HTML et CSS statiques, sans framework, sans build. Une feuille de style commune (`style.css`), pas de JavaScript. Polices Google Fonts : Playfair Display (titres) et EB Garamond (texte).

Pour voir le site : `python3 -m http.server 8000` dans ce dossier, puis http://localhost:8000.

## Pages

- `index.html` : accueil (logo, accroche, frise grecque, trois cartes)
- `homme.html` : portrait et parcours
- `biodynamie.html` : la biodynamie (philosophie et pratiques)
- `reportage.html` : les 7 étapes d'un millésime en photos, de la vendange à la mise en bouteille
- `vins.html` : les 4 cuvées (cadres bouteille + commentaire de dégustation à venir)
- `contact.html` : Instagram, coordonnées à compléter

L'en-tête, la navigation et le pied de page sont recopiés dans chaque page. Si on change un lien de menu ou le pied de page, il faut le faire dans les 6 fichiers.

## Charte

Le thème vient du logo (sérif noir, majuscules) et de la frise grecque en noir et blanc : étiquette de vin, parchemin, bordeaux, un peu d'or. Variables dans `:root` de `style.css`.

- `--cream` #F7F2E7 (fond), `--panel` #EFE6D3 (cartes), `--ink` #201B14 (texte, pied de page)
- `--wine` #6E1B2A et `--wine-dark` #4A121C (accents, bandeau biodynamie)
- `--gold` #A9803D (filets, petites capitales)

Éviter : dégradés décoratifs, emojis, Inter/Roboto/Arial, cartes avec liseré gauche coloré. Le logo et la frise ont un fond blanc, ils sont fondus dans le parchemin avec `mix-blend-mode: multiply`. Si on les remplace par des versions à fond transparent, on peut retirer cette règle.

## Images (dossier `assets/`)

- `logo.png` : logo « GUILLAUME L'ÉCHANSON » (accueil)
- `frise-echanson.png` : frise grecque, signature visuelle (accueil). Fichier un peu lourd (1,7 Mo), à convertir en WebP si on optimise.
- `encuvage.jpg` : Guillaume qui remplit la cuve au seau (bandeau de `homme.html`, étape 2 du reportage)
- `foulage.jpg` : Guillaume qui foule aux pieds dans la cuve (étape 3 du reportage)

Les autres étapes du reportage (1, 4, 5, 6, 7) ont un cadre « Photo à venir » (`.placeholder`). Pour ajouter une photo, remplacer le `<div class="placeholder">…</div>` par `<figure><img class="step-photo" src="assets/xxx.jpg" alt="…"><figcaption>…</figcaption></figure>`. Les photos arrivent au fil des vendanges. Redimensionner à 1600 px max et compresser avant de les mettre dans `assets/`.

Pour les bouteilles de `vins.html`, remplacer chaque `.bottle-slot` par une `<img>` (même hauteur, `object-fit: contain` sur fond `--panel` marche bien) et réécrire le commentaire sous le nom de la cuvée.

## Contenu à ne pas inventer

La bio de `homme.html` vient du brief de départ (Tour d'Argent 2008-2010, sommelier en Australie, Seychelles, Bora-Bora, Corse et Saint-Barth, formations chez Domaine de Montille, Face B, WasenHaus, Millton Vineyards, Domaine Saint-Nicolas, BTS viticulture et œnologie biologique 2021, vinifications chez Vin Noé, premier millésime officiel 2024 en cave partagée à Chassagne-Montrachet). Ne rien ajouter de factuel sur lui sans qu'il l'ait confirmé.

Cuvées : Bourgogne Aligoté, Bourgogne Blanc, Bourgogne Côte d'Or (rouge), Savigny-lès-Beaune (rouge, vinifié sans soufre).

## À valider avec Guillaume avant publication

- `biodynamie.html` est un texte général sur la biodynamie rédigé pour la maquette, pas des propos de Guillaume. À relire avec lui : pratiques réelles du domaine, éventuelle certification (Demeter, Ecocert, etc.), vocabulaire qu'il emploie.
- Les textes des étapes du reportage sont génériques (vendange manuelle, levures indigènes, pressurage lent…). Les ajuster à sa façon de travailler.
- Le tagline de l'accueil (« le vin que l'on tend, avec soin ») joue sur le sens d'« échanson ». À lui de dire s'il s'y reconnaît.
- Citation à insérer dans `homme.html` (bloc `.pullquote`).
- Coordonnées de `contact.html` : e-mail, téléphone, adresse de la cave.
- La mention « document de travail, non publié » du pied de page est à retirer à la mise en ligne.
- Pas de mentions légales, pas de bandeau cookies pour l'instant. Si le site est vendu en ligne ou héberge des formulaires, il faudra les ajouter. Mention obligatoire sur l'alcool à prévoir selon les règles françaises.

`sw.js` n'est pas un vrai service worker du site : il désinstalle celui de l'ancienne app PhotoTag, publiée avant à la même adresse et restée en cache chez ceux qui l'avaient ouverte. Le garder tant que le site reste sur cette URL.

## Ce qui reste à faire côté technique

- Choisir l'hébergement et le nom de domaine (non décidés).
- Balises Open Graph (aperçu de partage) et favicon.
- Texte `alt` définitifs quand toutes les photos seront là.
- Optimiser les images (WebP).

## Historique

Maquette conçue dans claude.ai (canvas de design, 6 artboards), puis convertie en HTML/CSS standard pour continuer dans Claude Code. La version canvas n'est plus à jour dès que ce dossier est modifié.
