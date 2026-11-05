# Prompt : site vitrine d'un artisan du bâtiment multi-métiers

À coller tel quel dans une nouvelle session Claude Code ouverte sur le dépôt `apexusbertrand/Test_claude`. Les éléments entre `[CROCHETS]` sont à remplacer avant de lancer, ou à laisser en placeholder si l'info n'est pas encore connue.

---

## Contexte

Tu vas créer le site vitrine de [PRÉNOM NOM], artisan indépendant dans le bâtiment, installé à [VILLE] ([DÉPARTEMENT]). Il est multi-métiers :

- carrelage (activité principale) : sols, murs, faïence, salles de bain, [à compléter]
- plâtrerie (activité principale) : cloisons, doublages, plafonds, plaques de plâtre, enduits, [à compléter]
- couverture (activité secondaire, petits chantiers) : [à compléter, ex. réparation de tuiles, faîtage, zinguerie légère]

Raison sociale : [NOM DE L'ENTREPRISE]. SIRET : [À COMPLÉTER]. Zone d'intervention : [ex. 30 km autour de VILLE]. Assurance décennale : [ASSUREUR, n° de contrat]. Labels éventuels (RGE, Qualibat…) : [À COMPLÉTER ou aucun].

Le but du site : montrer son savoir-faire avec des photos et des vidéos de vrais chantiers, inspirer confiance à des particuliers (et quelques pros), et générer des demandes de devis. Le site va évoluer au fil du temps : je (Bertrand) ajouterai régulièrement des réalisations, des compétences et des précisions sur ce qu'il fait ou ne fait pas, à partir des infos qu'il me donne. Tout doit donc être pensé pour que ces mises à jour soient rapides et ne demandent pas de retoucher le HTML.

Le dépôt contient déjà deux sites qui servent de références :

- `apexus-site/` : site vitrine d'Apexus-BMT. C'est le modèle pour le chat n8n (`@n8n/chat`, widget en mode fenêtre, couleurs surchargées par variables CSS, message d'accueil, métadonnées visiteur) et pour le formulaire de contact (POST JSON vers un webhook n8n, message de retour, repli par e-mail en cas d'erreur). Lis `apexus-site/index.html` (blocs `<script type="module">` avec `createChat` et le script du `#contactForm`) avant d'écrire le tien.
- `guillaume-echanson/` : site multi-pages en HTML/CSS statique avec un `CLAUDE.md` qui documente le projet. C'est le modèle pour l'organisation du dossier et la documentation.

## Ce que je veux

### 1. Structure et stack

- Nouveau dossier `artisan-[nom-court]/` à la racine du dépôt. Ne touche pas aux autres sites.
- HTML, CSS et JavaScript natifs, sans framework ni étape de build. Le site doit marcher en ouvrant les fichiers via `python3 -m http.server`.
- Le contenu qui évolue (réalisations, compétences, FAQ "on fait / on ne fait pas") vit dans des fichiers JSON dans `content/`, lus par un petit script JS qui génère les cartes et les pages. Ajouter une réalisation = ajouter une entrée JSON et déposer les photos, rien d'autre.
- Responsive mobile en priorité : la plupart des clients arriveront depuis un téléphone. Testé à 375 px de large, sans scroll horizontal.
- En-tête et pied de page identiques partout. Si tu les dupliques dans chaque page, documente-le dans le `CLAUDE.md` du dossier.

### 2. Pages

1. Accueil : accroche courte sur le métier, photo de lui sur chantier, les 3 métiers en cartes, les 3 dernières réalisations, bouton "Demander un devis" bien visible (aussi en barre fixe en bas sur mobile, avec un bouton d'appel `tel:`).
2. Savoir-faire : une section par métier (carrelage, plâtrerie, couverture) avec ce qu'il fait concrètement, les matériaux et techniques qu'il maîtrise, 2 ou 3 photos. Une rubrique "Ce que je ne fais pas" claire (ex. gros œuvre, électricité, si c'est le cas). Tout est généré depuis `content/competences.json`.
3. Réalisations (le blog) : liste filtrable par métier, chaque réalisation a sa page avec titre, date, commune (pas d'adresse précise), métier(s), texte court avant/après, galerie photos et vidéo éventuelle. Données dans `content/realisations.json`.
4. Qui suis-je : portrait, parcours, photos de lui au travail, ses engagements (propreté du chantier, respect des délais, devis gratuit… à valider avec lui).
5. Contact : formulaire, téléphone, e-mail, zone d'intervention (carte ou liste de communes), horaires.
6. Mentions légales et politique de confidentialité : obligatoires dès qu'il y a un formulaire. Placeholders pour SIRET, assurance décennale, hébergeur.

### 3. Photos et vidéos

- Photos dans `assets/realisations/<slug>/`, redimensionnées à 1600 px max, en WebP avec un JPEG de repli si tu le juges utile, `loading="lazy"`, `alt` descriptifs.
- Vidéos : pas de gros MP4 dans le dépôt (GitHub limite à 100 Mo par fichier et Pages n'est pas fait pour ça). Prévois l'intégration de vidéos YouTube en "non répertorié" via une façade légère (vignette + bouton lecture, l'iframe ne se charge qu'au clic, domaine `youtube-nocookie.com`). Garde une option pour un court MP4 compressé (moins de 10 Mo) hébergé dans `assets/`.
- Écris un script `tools/ajout-realisation.md` (ou un petit script Node/Python dans `tools/`) qui explique ou automatise : redimensionner les photos d'un dossier, les copier au bon endroit, créer l'entrée JSON squelette.

### 4. Formulaire de demande de devis

Même principe que `apexus-site` : envoi en JSON par `fetch` vers un webhook n8n, qui envoie un mail à l'artisan.

- URL du webhook : `[https://apexus.app.n8n.cloud/webhook/contact-artisan-XXX]`, dans une constante en haut du script.
- Champs : prénom, nom, téléphone (obligatoire, c'est son canal principal), e-mail, commune du chantier, type de travaux (cases à cocher : carrelage, plâtrerie, couverture, autre), surface approximative, délai souhaité (liste : urgent, sous 1 mois, 1 à 3 mois, pas pressé), description libre, case de consentement RGPD.
- Anti-spam sans captcha : champ honeypot caché + temps minimum de remplissage.
- Message de succès et message d'erreur avec repli vers le téléphone et l'e-mail de l'artisan.
- Les photos du client (pièce à rénover) seraient utiles mais compliquent le webhook. Prévois le champ en option désactivée, avec un commentaire dans le code expliquant comment l'activer (envoi `multipart/form-data`, limite de taille côté navigateur).
- Ajoute un objet `source: "formulaire-site"` et la page d'origine dans le JSON envoyé.

### 5. Chat

Même widget que sur `apexus-site` : `@n8n/chat` en mode `window`, aux couleurs du site, message d'accueil du type "Bonjour, je suis l'assistant de [PRÉNOM]. Posez-moi vos questions sur ses travaux de carrelage, plâtrerie ou couverture.".

- Webhook : `[URL DU WEBHOOK CHAT N8N]`, dans une constante.
- Fige la version du paquet (`@n8n/chat@x.y.z`) au lieu de `@latest`, pour éviter qu'une mise à jour casse le site.
- Métadonnées envoyées avec chaque conversation : page en cours, langue, référent. Pas de géolocalisation IP tant que l'utilisateur n'a pas accepté les cookies.
- Le chat doit savoir dire ce que l'artisan fait et ne fait pas, ne jamais donner de prix ferme ni de délai garanti, et orienter vers le formulaire ou le téléphone dès qu'il y a un projet concret.

Côté n8n (à préparer, pas à déployer sans mon accord) :

- Le flux du chat lit `content/competences.json` et `content/faq.json` directement depuis l'URL publiée du site. Une seule source de vérité : quand je mets à jour une compétence sur le site, le chat la connaît sans toucher au flux.
- Plus tard, une petite base de connaissances (réponses types, cas limites) sera ajoutée. Prévois la structure de `content/faq.json` pour ça : `question`, `reponse`, `metier`, `fait` (oui/non/sur devis), `mots_cles`.
- Rédige dans `n8n/prompt-agent-chat.md` le prompt système de l'agent (ton, règles, ce qu'il ne doit jamais dire, comment il passe la main) et dans `n8n/README.md` la description des deux flux (formulaire → Gmail, chat → agent IA + base de connaissances), avec le format JSON attendu par chaque webhook.

### 6. Charte graphique

Pas encore définie. Propose une direction sobre qui évoque le métier (matières, minéral, chantier propre) : une couleur d'accent franche, des neutres chauds, une police lisible pour les titres. Variables CSS dans `:root`. Évite les dégradés décoratifs, les emojis, les icônes clipart de maison et de truelle. Si l'artisan a un logo, je le déposerai dans `assets/logo.*`.

### 7. Référencement local

- Balises title et description par page, centrées sur métier + ville (ex. "Carreleur plaquiste à [VILLE]").
- Données structurées `HomeAndConstructionBusiness` (JSON-LD) avec zone desservie, téléphone, horaires.
- `sitemap.xml`, `robots.txt`, balises Open Graph, favicon.
- En préversion : `noindex` partout, comme pour `guillaume-echanson`. À retirer à la vraie mise en ligne.

### 8. Cookies et RGPD

Bandeau de consentement sur le modèle d'`apexus-site`. YouTube et la géolocalisation IP ne se chargent qu'après accord. Politique de confidentialité qui dit où partent les données du formulaire et du chat (n8n, Gmail) et combien de temps elles sont gardées.

## Règles à respecter

- N'invente rien de factuel sur l'artisan : parcours, années d'expérience, certifications, chantiers, avis clients, prix. Mets un placeholder visible `[À COMPLÉTER]` et liste tout ce qui manque dans le `CLAUDE.md`.
- Pas de faux avis clients ni de fausses réalisations. Les cartes vides affichent "Réalisation à venir".
- Les textes doivent être simples, directs, à la première personne ("je"), comme un artisan qui parle à un client.
- Ne modifie pas `.github/workflows/deploy-pages.yml` sans me demander : il publie actuellement `guillaume-echanson`. Propose-moi plutôt les options de publication (sous-dossier, autre dépôt, domaine dédié).

## Livrables

- Le dossier `artisan-[nom-court]/` complet et fonctionnel en local.
- `content/competences.json`, `content/realisations.json`, `content/faq.json` avec 1 ou 2 entrées d'exemple clairement marquées comme exemples.
- `CLAUDE.md` du dossier : stack, pages, charte, procédure pour ajouter une réalisation, une compétence, une entrée de FAQ, liste des infos à obtenir de l'artisan.
- `n8n/README.md` et `n8n/prompt-agent-chat.md`.
- Une capture d'écran mobile et desktop de l'accueil pour validation.
- Commit sur la branche de travail, sans pull request.

## Infos à demander à l'artisan (à compléter avant ou après la première version)

- Nom commercial, SIRET, adresse du siège, assurance décennale
- Téléphone, e-mail, horaires, zone d'intervention précise
- Liste détaillée de ce qu'il fait par métier, et de ce qu'il refuse
- Parcours (formation, années de métier, anciennes entreprises)
- Photos de lui (portrait + en action), logo s'il en a un
- 3 à 5 premiers chantiers avec photos avant/après et quelques lignes
- Chaîne YouTube ou compte Instagram/Facebook existant
- Ce qu'il veut recevoir par mail quand un client remplit le formulaire
