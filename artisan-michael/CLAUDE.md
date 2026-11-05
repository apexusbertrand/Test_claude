# Site de Michael (artisan carrelage, plâtrerie, couverture)

Maquette de site vitrine pour Michael, artisan indépendant multi-métiers : carrelage et plâtrerie en activité principale, couverture pour les petits chantiers. Brief complet : `../prompts/site-artisan-batiment.md`. Branche de travail : `SITE_MICHAEL`.

Statut : maquette de présentation. Coordonnées, SIRET, adresse, zone et le chantier n°2 sont fictifs. Le bandeau jaune en haut des pages et la mention du pied de page le disent. Tout est en `noindex` (meta + `robots.txt`).

## Lancer en local

`python3 -m http.server 8000` dans ce dossier, puis http://localhost:8000. Les pages lisent les JSON de `content/` avec `fetch` : en ouvrant le fichier directement (file://), les chantiers et les métiers ne s'affichent pas.

## Stack

HTML, CSS et JS natifs, sans build. Polices Google Fonts : Bricolage Grotesque (titres et texte) et IBM Plex Mono (annotations, étiquettes).

- `js/site.js` : `CONFIG` en haut du fichier (nom, téléphone, e-mail, adresse, zone, horaires, SIRET, URL des webhooks, `maquette: true/false`). L'en-tête, le pied de page, la barre mobile et le mètre ruban sont injectés par ce script : on les modifie à un seul endroit. Le même fichier génère les cartes métiers, les fiches chantier, la page chantier et gère le formulaire.
- `js/chat.js` : chat. Si `CONFIG.chatWebhook` est vide, mode démo qui répond à partir de `content/faq.json` par mots-clés. S'il est renseigné, chargement du widget `@n8n/chat` (version figée dans `N8N_CHAT_VERSION`), sur le modèle d'`apexus-site`.
- Formulaire : si `CONFIG.formWebhook` est vide, rien n'est envoyé et un message le dit. Sinon POST JSON vers n8n (format dans `n8n/README.md`). Anti-spam : champ caché `website` et 3 s minimum de remplissage.

## Pages

- `index.html` : accueil (titre « Du brut au fini », comparateur avant/après, métiers, coupe de mur, 2 derniers chantiers, méthode, bandeau d'appel)
- `savoir-faire.html` : les 3 métiers et « Ce que je ne fais pas », générés depuis `content/competences.json`
- `realisations.html` : liste filtrable des chantiers
- `chantier.html?c=<slug>` : une page par chantier, générée depuis `content/realisations.json`
- `qui-suis-je.html` : présentation, emplacement du portrait, règles de chantier
- `contact.html` : formulaire de devis, coordonnées, zone
- `mentions-legales.html` : mentions et confidentialité (bouchon)

## Charte

Variables dans `:root` de `css/style.css`. Plâtre `--plaster` #F2EFE8 (fond), anthracite `--ink` #141517, brique vive `--brick` #E8582A (accent), bleu cordeau `--cordeau` #2B50E8 (cotes, soulignés), jaune mètre #F6C744 (mètre ruban, niveau à bulle, notes).

Les éléments qui font l'identité du site, à garder si on retouche : le mètre ruban en haut qui suit le défilement, la poignée du comparateur en niveau à bulle, les textures de matériaux en CSS (`.tex-tile`, `.tex-plaster`, `.tex-roof`, `.tex-brick`…), la coupe de mur en 5 couches, les fiches façon panneau de chantier. Angles droits, bordures de 2 px, ombres décalées pleines. Pas de dégradés décoratifs, pas d'emojis, pas de coins arrondis.

## Logo

Fictif. `assets/logo.svg` et copie inline dans `js/site.js` (constante `LOGO`). Un M en carreaux avec joints (carrelage) sous un toit (couverture), sur fond lisse (plâtrerie). Un carreau en brique. Chaque carreau a la classe `lg-t`, le toit `lg-roof` : prévu pour l'animation (pose carreau par carreau, puis le toit qui se trace).

## Ajouter du contenu

- Un chantier : voir `tools/ajout-realisation.md`.
- Une compétence ou une prestation : ajouter une ligne dans `prestations` du métier concerné dans `content/competences.json`. Un refus : dans `ne_fait_pas`.
- Une réponse du chat : ajouter une entrée dans `content/faq.json` (`question`, `reponse`, `metier`, `fait` oui/non/sur devis, `mots_cles`).

## Contenu à ne pas inventer

Chantier n°1 (`combles-briques`) : vraies photos fournies par Bertrand. La commune, la date et la durée ne sont pas connues (« à confirmer »). Les textes des étapes décrivent ce qu'on voit sur les photos, à relire avec Michael. On ne sait pas si le sol et le radiateur de la photo finale sont de lui.

Chantier n°2 (`salle-de-bain-exemple`) : entièrement fictif, visuels SVG d'illustration. `"fictif": true` affiche l'étiquette « Exemple » et un avertissement. À supprimer dès qu'un vrai deuxième chantier arrive.

Les engagements de `qui-suis-je.html`, la liste « Ce que je ne fais pas », les réponses de la FAQ et le délai de rappel sont des propositions à valider.

## À obtenir de Michael

Nom de famille et nom commercial, SIRET, adresse, assurance décennale, téléphone, e-mail, horaires, zone réelle, parcours, portrait, photos de lui au travail (et savoir qui est qui sur la photo 03), liste exacte de ce qu'il fait et refuse, infos du chantier des combles (commune, date, durée), chantiers suivants.

## Publication

Le workflow `.github/workflows/deploy-pages.yml` publie `guillaume-echanson` depuis `main` : ne pas le modifier sans accord. Pour montrer la maquette sans toucher à Pages, raw.githack sert les fichiers de la branche :
https://raw.githack.com/apexusbertrand/Test_claude/SITE_MICHAEL/artisan-michael/index.html
(nécessite un dépôt public.)
