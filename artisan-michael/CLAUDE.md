# Site de LOGE&NERJ, l'entreprise de Michael (rénovation tous corps d'état et dépannage)

Maquette de site vitrine pour Michael, artisan indépendant en rénovation tous corps d'état (mise à jour du 12/11/2026, d'après ses messages) : salle de bain et carrelage, placo et isolation, toiture, menuiserie extérieure (fenêtres, volets roulants, portails, clôtures rigides), peinture, électricité, plomberie, et dépannage électricité / plomberie / fenêtres. Brief de départ (antérieur, plus étroit) : `../prompts/site-artisan-batiment.md`. Branche de travail : `SITE_MICHAEL`.

Nom de société : LOGE&NERJ (donné par Bertrand le 10/11/2026), signature « Rénover, isoler, économiser ». Dans le HTML, l'écrire `LOGE&amp;NERJ`. Domaine visé : logenerj.fr (disponibilité et marque INPI non vérifiées : réseau de la session bloqué).

Statut : maquette de présentation. Coordonnées, e-mail (`contact@logenerj.example`), SIRET, adresse, zone et le chantier n°2 sont fictifs. Le bandeau jaune en haut des pages et la mention du pied de page le disent. Tout est en `noindex` (meta + `robots.txt`).

## Lancer en local

`python3 -m http.server 8000` dans ce dossier, puis http://localhost:8000. Les pages lisent les JSON de `content/` avec `fetch` : en ouvrant le fichier directement (file://), les chantiers et les métiers ne s'affichent pas.

## Stack

HTML, CSS et JS natifs, sans build. Polices Google Fonts : Bricolage Grotesque (titres et texte) et IBM Plex Mono (annotations, étiquettes).

- `js/site.js` : `CONFIG` en haut du fichier (nom, téléphone, e-mail, adresse, zone, horaires, SIRET, URL des webhooks, `maquette: true/false`). L'en-tête, le pied de page, la barre mobile et le mètre ruban sont injectés par ce script : on les modifie à un seul endroit. Le même fichier génère les cartes métiers, les fiches chantier, la page chantier et gère le formulaire.
- `js/chat.js` : chat. Si `CONFIG.chatWebhook` est vide, mode démo qui répond à partir de `content/faq.json` par mots-clés. S'il est renseigné, chargement du widget `@n8n/chat` (version figée dans `N8N_CHAT_VERSION`), sur le modèle d'`apexus-site`.
- Formulaire : si `CONFIG.formWebhook` est vide, rien n'est envoyé et un message le dit. Sinon POST JSON vers n8n (format dans `n8n/README.md`). Anti-spam : champ caché `website` et 3 s minimum de remplissage.

## Pages

- `index.html` : accueil (titre « Du brut au fini », comparateur avant/après, métiers, coupe de mur, 2 derniers chantiers, méthode, bandeau d'appel)
- `savoir-faire.html` : les 6 métiers et la section « À voir ensemble » (ce qui reste à confirmer, masquée si la liste `ne_fait_pas` est vide), générés depuis `content/competences.json`
- `realisations.html` : liste filtrable des chantiers
- `chantier.html?c=<slug>` : une page par chantier, générée depuis `content/realisations.json`
- `qui-suis-je.html` : présentation, emplacement du portrait, règles de chantier
- `contact.html` : formulaire de devis, coordonnées, zone
- `mentions-legales.html` : mentions et confidentialité (bouchon)

## Charte

Tout le site est « charté » sur le logo officiel de Michael (`assets/brand/logo-officiel.webp`, reçu le 10/11/2026). Le détail est dans `docs/charte-graphique.md`, et la page `charte.html` (liée depuis le pied de page tant que `CONFIG.maquette` est vrai) sert à la faire valider.

En bref : verts du logo (forêt #425A1B, champ #597424, feuille #749A30), bleus des panneaux (#01CCFF, #01B0EB, #006CC0), jaune soleil #FFCD01 pour les boutons d'action, fond blanc cassé. Polices : Oswald (titres, comme le logotype), Amatic SC (petites touches, comme la signature), Nunito Sans (texte). Formes : coins arrondis, pastilles, blocs séparés par un filet blanc, bandes de champs incurvées (`assets/brand/champs.svg`) entre les sections. Variables dans `:root` de `css/style.css`.

Éléments d'identité à garder : le titre d'accueil « Rénover, isoler, économiser » (« isoler » en dégradé bleu panneaux, point final en soleil), le mètre jaune qui suit le défilement, le niveau à bulle du comparateur, les textures de matériaux en CSS, la coupe de mur en 5 couches.

## Logo

Logo officiel, à ne jamais redessiner ni recolorer. Fichiers dans `assets/brand/` :

- `logo-officiel.webp` : fichier d'origine fourni (référence, fond blanc)
- `logo-transparent.webp`, `logo-transparent-900.webp` : logo complet détouré (pied de page)
- `embleme.webp`, `embleme-400.webp`, `embleme-160.webp` : emblème seul détouré (en-tête via la constante `LOGO` de `js/site.js`, chat)
- `favicon-32.png`, `favicon-192.png`, `og-image.jpg` (aperçu de partage)

Logo animé (13 s, 30 i/s, carré ; la première image montre déjà la maison, la dernière est le logo officiel) : `logo-animation.mp4` (1080 px) et `logo-animation-720.mp4` (utilisé en haut de l'accueil, comme sur le site Apexus), image de repli `logo-animation-poster.webp`. Scénario : vieux pignon en brique fissuré, rénovation (ossature, isolant, plaques), maison neuve, soleil levant, panneaux solaires, fenêtres qui s'allument, feuilles, champs et sapins. Vers 8 s, la caméra recule : les murs s'effacent, le toit solaire glisse sous les panneaux du logo et s'y fond, le soleil rejoint celui du logo. Logotype puis signature, et le logo reste affiché sur la dernière seconde. Le fichier 1080 px porte aussi le logo en image de couverture (vignette dans les lecteurs vidéo). Source modifiable : `tools/logo-animation/animation.html` (s'ouvre dans un navigateur et tourne en boucle), rendu vidéo avec `tools/logo-animation/render.cjs` (commandes en tête du fichier). Fond blanc pur : sur la page, `mix-blend-mode: multiply` le fait disparaître dans le fond papier. Un clic sur la vidéo la rejoue ; en mouvement réduit, seule l'image finale s'affiche.

Le détourage est automatique (blanc rendu transparent). Si Michael a un fichier vectoriel (SVG, AI, PDF), le récupérer : il remplacera ces dérivés. Avec un fichier vectoriel, la fin de l'animation pourrait assembler l'emblème pièce par pièce au lieu d'un fondu.

## Ajouter du contenu

- Un chantier : voir `tools/ajout-realisation.md`.
- Une compétence ou une prestation : ajouter une ligne dans `prestations` du métier concerné dans `content/competences.json`. Un refus : dans `ne_fait_pas`.
- Une réponse du chat : ajouter une entrée dans `content/faq.json` (`question`, `reponse`, `metier`, `fait` oui/non/sur devis, `mots_cles`).

## Contenu à ne pas inventer

Chantier n°1 (`combles-briques`) : vraies photos fournies par Bertrand. La commune, la date et la durée ne sont pas connues (« à confirmer »). Les textes des étapes décrivent ce qu'on voit sur les photos, à relire avec Michael. On ne sait pas si le sol et le radiateur de la photo finale sont de lui.

Chantier n°2 (`salle-de-bain-exemple`) : entièrement fictif, visuels SVG d'illustration. `"fictif": true` affiche l'étiquette « Exemple » et un avertissement. À supprimer dès qu'un vrai deuxième chantier arrive.

Les engagements de `qui-suis-je.html`, les détails marqués « à confirmer » dans `content/competences.json`, la liste `ne_fait_pas` (gros œuvre, charpente neuve), les réponses de la FAQ et le délai de rappel sont des propositions à valider.

## À obtenir de Michael

Fichier vectoriel du logo, statut RGE ou non (la signature parle d'économies d'énergie : les clients demanderont s'ils ont droit aux aides), nom de famille, forme juridique de LOGE&NERJ, SIRET, adresse, assurance décennale, téléphone, e-mail, horaires, zone réelle, parcours, portrait, photos de lui au travail (et savoir qui est qui sur la photo 03), liste exacte de ce qu'il fait et refuse, infos du chantier des combles (commune, date, durée), chantiers suivants.

## Publication

Le workflow `.github/workflows/deploy-pages.yml` publie `guillaume-echanson` depuis `main` : ne pas le modifier sans accord. Pour montrer la maquette sans toucher à Pages, raw.githack sert les fichiers de la branche :
https://raw.githack.com/apexusbertrand/Test_claude/SITE_MICHAEL/artisan-michael/index.html
(nécessite un dépôt public.)

## Facebook

Page Facebook « Loge&nerj » : https://www.facebook.com/p/Logenerj-61591852021732/ (le lien de partage reçu, facebook.com/share/1Fsabbduiz, y redirige). Elle fait foi sur le contenu. Lien dans `CONFIG.facebook` (`js/site.js`), affiché dans le pied de page, la page Contact, Qui suis-je, Chantiers, le chat (`content/faq.json`) et les données structurées de l'accueil (`sameAs`).

Ce qu'on a pu lire le 12/11/2026 (fiche publique, sans compte) : « Loge&nerj, Somain, Nord. Société familiale spécialisée dans la rénovation de l'habitat : plomberie, peinture, électricité, toitures, fenêtres, isolation, pompe à… » (texte coupé par Facebook ; « pompe à chaleur » ajouté au site, à confirmer). Le site a été mis à jour en conséquence : Somain (59490) au lieu de Lille, société familiale, carte « Pompe à chaleur » (métier 07), zone d'intervention autour de Somain (communes à confirmer).

Les publications et photos ne sont pas lisibles sans compte Facebook (la version mobile renvoie vers la connexion). Pour les reprendre sur le site :

1. Le plus complet : depuis la page, « Paramètres > Télécharger vos informations », format JSON, en cochant Publications et Photos et vidéos. Déposer le .zip dans la session : textes, dates et photos s'importent dans `content/realisations.json` et `assets/realisations/`, et le chat est enrichi d'après les vrais chantiers.
2. Le plus rapide : des captures d'écran des publications, ou les photos avec le texte de chaque publication.

Réseau : www.facebook.com et m.facebook.com ont été ajoutés aux hôtes autorisés de l'environnement « DEV » le 12/11/2026.
