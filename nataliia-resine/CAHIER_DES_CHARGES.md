# Nataliia Résine : cahier des charges du site web

Version 0.1, 18 novembre 2026. Document de cadrage, rien n'est encore développé.

Sources utilisées, et uniquement elles :

- la maquette réalisée par la cliente avec une IA, en ligne sur https://headless-rkelfehhkdy-shevcukn-1408.wix-site-host.com/index.html (5 pages, copie archivée dans `maquette-origine/`) ;
- le message de la cliente (demande de site, accès administration, idée de simulation sur photo).

Tout ce qui n'y figure pas est soit une proposition technique de notre part (signalée comme telle), soit une question listée dans `QUESTIONS_CLIENT.md`.

## 1. Contexte et objectifs

La cliente pose des revêtements extérieurs en moquette de pierre (granulats de marbre) : terrasses, allées, escaliers d'entrée, abords de piscine. Elle vend aussi des produits (granulats de marbre, profils de finition).

Elle a construit une maquette avec une IA et veut un vrai site, qu'elle puisse faire évoluer seule.

Objectifs exprimés dans son message :

1. Reprendre la maquette et la compléter (certaines rubriques sont vides de son propre aveu).
2. Pouvoir ajouter et supprimer elle-même des photos de réalisations, des échantillons de granulats et des références de produits.
3. Permettre au visiteur de charger une photo de sa terrasse ou de son allée, de choisir un granulat et de voir le rendu sur sa propre photo.
4. Recevoir notre avis de professionnel sur ce qui est utile ou non.

Hébergement : d'abord sur GitHub (GitHub Pages, comme le site Guillaume l'Échanson déjà présent dans ce dépôt), puis sur un nom de domaine.

## 2. Découpage en lots

| Lot | Contenu | Dépend de |
|---|---|---|
| 1 | Site vitrine (reprise et complément de la maquette) | contenus de la cliente |
| 2 | Administration autonome (ajout/suppression de photos, granulats, produits) | lot 1 (structure des contenus) |
| 3 | Simulateur de rendu avant/après sur la photo du visiteur | lot 1, et le catalogue de granulats du lot 2 |

Les lots 2 et 3 peuvent démarrer en parallèle une fois le modèle de contenu du lot 1 figé.

## 3. Existant : ce que contient la maquette

### 3.1 Identité visuelle

- Logo fourni : `assets/maquette/logo-nataliia-resine.jpg` (1280 x 640, fond noir, monogramme « NS » doré sous un toit, texte « NATALIIA SHEVCHUK »).
- Image d'accueil : `assets/maquette/hero-terrasse.png` (1672 x 941, 3,7 Mo). Une femme pose des granulats sur une terrasse au coucher du soleil. Le rendu ressemble à une image générée par IA.
- Nom affiché sur le site : « Nataliia Résine ». Le pied de page utilise un monogramme texte « NR ». Le logo, lui, dit « NS » et « NATALIIA SHEVCHUK ». Point à trancher (voir questions).
- Couleurs (variables CSS de la maquette) : noir `#11100f`, papier `#f7f4ee`, encre `#292722`, gris `#777168`, or `#b58a50`, or foncé `#8d6739`, or clair `#d2b27b`, filets `#e5ddd0`, blanc cassé `#fffefa`.
- Polices : Playfair Display (titres), DM Sans (texte), via Google Fonts.
- Signature : « Une solution créative. Un style qui vous ressemble. »

### 3.2 Pages

| Page | Fichier maquette | Contenu actuel |
|---|---|---|
| Accueil | `index.html` | Image plein écran, 4 onglets de navigation posés sur l'image (Produits, Prestations, Contact, Créez votre projet), signature |
| Produits | `produits.html` | 2 cartes texte : Granulats de marbre, Profil de finition. Bouton « Demander des informations » vers Contact. Aucune photo |
| Prestations | `prestations.html` | 4 cartes texte : Terrasses, Allées, Escaliers d'entrée, Abords de piscine. Bouton « Parlons de votre projet ». Aucune photo |
| Contact | `contact.html` | Téléphone 07 81 07 21 22, WhatsApp (même numéro), e-mail nataliiaresine@gmail.com. Pas de formulaire, pas d'adresse |
| Créez votre projet | `configurateur.html` | Chargement d'une photo, choix parmi 6 couleurs provisoires (Ivoire, Beige sable, Pierre, Gris, Gris foncé, Noir), champ description, envoi par ouverture de la messagerie (la photo n'est pas jointe automatiquement, la couleur ne s'applique pas sur la photo) |

### 3.3 Limites constatées

- Le bouton « menu » (3 traits) en haut à droite renvoie seulement vers l'accueil. Il n'y a pas de vrai menu sur les pages intérieures : pour passer de Produits à Prestations, il faut repasser par l'accueil.
- Aucune page Réalisations ni catalogue de granulats, alors que le message de la cliente en parle.
- Aucune mention légale, aucune politique de confidentialité (obligatoires pour un site professionnel en France, et d'autant plus avec un envoi de photos).
- La maquette tourne sur une infrastructure Wix (script Wix chargé dans les pages). On ne la réutilise pas : on repart des fichiers HTML/CSS.
- L'image d'accueil pèse 3,7 Mo : à convertir en WebP/AVIF et décliner en plusieurs tailles.
- Le configurateur envoie un `mailto:` : le visiteur doit joindre sa photo à la main, beaucoup ne le feront pas.

## 4. Lot 1 : site vitrine

### 4.1 Périmètre

Reprendre la charte et le ton de la maquette, garder sa structure, combler les manques.

Pages :

| Page | Source | Évolution prévue |
|---|---|---|
| Accueil | maquette | Conserver l'image plein écran et les onglets. Ajouter sous la signature un aperçu des dernières réalisations (si la cliente fournit des photos) |
| Prestations | maquette | Une carte par prestation avec photo. Texte actuel conservé, à enrichir si la cliente fournit plus d'éléments |
| Produits | maquette | Garder les 2 familles (granulats, profils). La famille Granulats renvoie vers le nuancier |
| Nuancier des granulats | message cliente (« échantillons de granulats ») | Nouvelle page : une fiche par granulat (photo de l'échantillon, nom, éventuellement granulométrie). Alimente aussi le simulateur du lot 3 |
| Réalisations | message cliente (« photos de mes réalisations ») | Nouvelle page : galerie, filtrable par type de prestation (terrasse, allée, escalier, piscine). Une réalisation = titre, type, photos, granulat utilisé si connu |
| Contact | maquette | Garder téléphone, WhatsApp, e-mail. Ajouter les infos manquantes si fournies (zone d'intervention, adresse) |
| Créez votre projet | maquette | Remplacé par le simulateur du lot 3. En attendant le lot 3, version actuelle conservée |
| Mentions légales | obligation légale | Nouvelle page |
| Politique de confidentialité | obligation légale (RGPD) | Nouvelle page, indispensable dès que le visiteur envoie une photo |

Navigation : vrai menu sur toutes les pages (menu déroulant au clic sur les 3 traits, sur mobile comme sur ordinateur), en plus des onglets de l'accueil.

### 4.2 Exigences

- Responsive : téléphone d'abord (la maquette est déjà pensée pour), tablette, ordinateur.
- Accessibilité : contrastes suffisants, textes alternatifs sur toutes les images, navigation au clavier (la maquette a déjà de bonnes bases).
- Performance : images optimisées (WebP/AVIF, plusieurs tailles, chargement différé), objectif Lighthouse supérieur à 90 sur mobile.
- Référencement : balise title et description par page, données structurées « LocalBusiness » (nécessite la zone d'intervention et les coordonnées), sitemap, Open Graph pour le partage sur WhatsApp et réseaux.
- Préversion sur GitHub Pages en `noindex` (comme pour Guillaume l'Échanson), à retirer au passage sur le nom de domaine.
- Liens directs : appel téléphonique, WhatsApp, e-mail (déjà présents).

### 4.3 Proposition technique

Site statique généré à partir de fichiers de contenu (Markdown/JSON), avec un générateur léger type Eleventy ou Astro. Raison : le lot 2 a besoin que les contenus (réalisations, granulats, produits) soient des fichiers séparés du code, pour que l'administration puisse les modifier sans toucher aux pages. Un site HTML écrit à la main, comme la maquette ou le site Guillaume l'Échanson, ne le permet pas.

Déploiement : GitHub Actions vers GitHub Pages, à chaque modification (y compris celles faites par la cliente depuis l'administration).

### 4.4 Livrables

- Code source dans ce dépôt, dossier `nataliia-resine/`.
- Préversion en ligne sur GitHub Pages.
- Bascule sur le nom de domaine (DNS, HTTPS) quand il sera disponible.

## 5. Lot 2 : administration autonome

### 5.1 Besoin

Message de la cliente : « ajouter moi-même, par la suite, de nouvelles photos, réalisations et références de produits ». Elle n'est pas technicienne, l'interface doit ressembler à un formulaire, sans code ni Git visible.

### 5.2 Ce que la cliente peut faire seule

| Contenu | Actions |
|---|---|
| Réalisations | créer, modifier, supprimer, réordonner ; ajouter/retirer des photos ; choisir le type de prestation et le granulat utilisé |
| Granulats (nuancier) | créer, modifier, supprimer ; photo de l'échantillon ; nom ; texture utilisée par le simulateur (lot 3) |
| Produits | créer, modifier, supprimer une référence ; photo, nom, description |
| Prestations | modifier texte et photo des 4 prestations existantes |
| Coordonnées | modifier téléphone, e-mail, zone d'intervention |
| Image et phrase d'accueil | remplacer |

Hors périmètre de l'administration (reste à notre charge) : charte graphique, mise en page, ajout d'un nouveau type de page.

### 5.3 Exigences

- Accès par identifiant, depuis un navigateur, sur ordinateur et sur téléphone (elle prendra probablement ses photos de chantier avec son téléphone : à confirmer).
- Envoi de photos directement depuis le téléphone, avec redimensionnement et compression automatiques (une photo de 8 Mo ne doit pas finir telle quelle sur le site).
- Aperçu avant publication.
- Mise en ligne en un clic, effective en quelques minutes.
- Historique : toute modification est réversible (Git conserve chaque version).
- Champs obligatoires contrôlés (pas de réalisation sans photo, pas de photo sans texte alternatif, avec un texte proposé par défaut).
- Un guide d'utilisation de 2 ou 3 pages, avec captures, et une séance de prise en main.

### 5.4 Proposition technique

Un CMS « Git » qui écrit directement dans le dépôt GitHub : Decap CMS, Sveltia CMS ou Pages CMS. Pas de serveur ni de base de données à maintenir, compatible GitHub Pages, gratuit.

Recommandation : Sveltia CMS (successeur compatible de Decap, interface plus moderne, bonne gestion mobile et compression des images à l'envoi). La connexion GitHub demande un petit relais d'authentification (OAuth), hébergeable gratuitement (Cloudflare Workers par exemple). Alternative sans relais : Pages CMS, service hébergé, la cliente se connecte avec un compte GitHub.

Dans tous les cas la cliente aura besoin d'un compte GitHub, ajouté comme collaboratrice du dépôt. Si on sépare son site dans un dépôt dédié (recommandé, voir 8), elle n'aura accès qu'à son site.

Limite à connaître : GitHub Pages recommande moins de 1 Go par site. Avec des photos compressées (300 à 500 Ko), cela laisse plusieurs milliers de photos. Si le volume explose, on déplacera les photos vers un service d'images (Cloudinary, Cloudflare Images).

## 6. Lot 3 : simulateur de rendu avant/après

### 6.1 Besoin

Message de la cliente : le futur client charge une photo de sa terrasse ou de son allée, choisit un échantillon de granulats et voit un aperçu du résultat sur sa propre photo. La maquette contient déjà une première version (page « Créez votre projet ») qui ne recolore pas la photo.

### 6.2 Parcours visiteur

1. Charger une photo (appareil photo du téléphone ou fichier).
2. Délimiter la surface à recouvrir (terrasse, allée, marches).
3. Choisir un granulat dans le nuancier (les vrais granulats saisis dans l'administration, plus les 6 couleurs provisoires de la maquette).
4. Voir le rendu, comparer avant/après avec un curseur que l'on fait glisser sur l'image.
5. Changer de granulat et comparer.
6. Télécharger l'image avant/après.
7. Envoyer son projet à Nataliia : photo d'origine, rendu, granulat choisi, description, coordonnées.

### 6.3 Exigences fonctionnelles

- Délimitation de la zone : le visiteur pose des points autour de la surface (polygone), corrige au doigt ou à la souris, peut gommer les objets posés dessus (pot de fleurs, meuble).
- Perspective : la texture doit suivre la perspective du sol, sinon le rendu fait « papier peint ». Le visiteur indique 4 points du sol (un rectangle vu en perspective), on en déduit l'inclinaison.
- Réalisme : la texture garde les ombres et la lumière de la photo d'origine (fusion avec la luminosité de l'image), la taille du grain reste cohérente avec la distance.
- Curseur avant/après, plein écran sur téléphone.
- Envoi du projet avec la photo jointe automatiquement (et non plus via `mailto:`).
- Mention claire sous le rendu : aperçu indicatif, le rendu réel dépend du support et de la pose.

### 6.4 Exigences techniques

- Traitement dans le navigateur du visiteur : la photo ne quitte pas son appareil tant qu'il n'a pas cliqué sur « Envoyer ». Argument de confidentialité et zéro coût serveur.
- Fonctionne sur les téléphones récents (iPhone Safari et Android Chrome), photos jusqu'à 12 mégapixels (réduites pour le calcul).
- Envoi du projet : GitHub Pages ne sait pas recevoir de formulaire. Il faut un service d'envoi (Formspree, Web3Forms, ou une petite fonction Cloudflare Workers) capable de transmettre la photo par e-mail à la cliente.
- Le nuancier vient des fiches granulats du lot 2 : chaque fiche porte une texture « simulateur » (photo prise à plat, bien éclairée, raccordable en mosaïque).

### 6.5 Découpage proposé

| Étape | Contenu | Commentaire |
|---|---|---|
| 3a | Zone dessinée à la main, perspective par 4 points, texture, curseur avant/après, téléchargement, envoi | Version fiable et maîtrisée, livrable en premier |
| 3b | Détection automatique du sol par IA dans le navigateur (modèle de segmentation type SAM), le visiteur touche la terrasse et la zone se sélectionne seule | Plus confortable, mais lourd à charger sur mobile et pas fiable à 100 % : on garde l'outil manuel pour corriger |
| Option | Rendu par IA générative (le visiteur obtient une image « photo réaliste » refaite par un modèle) | Plus spectaculaire, mais payant à chaque rendu, photo envoyée à un service tiers, résultat parfois éloigné du vrai granulat. Déconseillé pour l'instant |

### 6.6 Prérequis côté cliente

Le simulateur ne vaut que par la qualité des textures. Il faut, pour chaque granulat proposé, une photo d'un échantillon posé à plat, prise à la verticale, en lumière du jour sans ombre portée, avec une règle ou une pièce pour l'échelle. Idéalement aussi 2 ou 3 photos avant/après de vrais chantiers pour vérifier que le rendu du simulateur ressemble au résultat réel.

## 7. Planning indicatif (ordre, sans dates)

1. Validation de ce cahier des charges et réponses aux questions.
2. Réception des contenus (photos, textes, granulats).
3. Lot 1 : mise en place du générateur, pages, préversion GitHub Pages, recette avec la cliente.
4. Lot 2 : administration, tests par la cliente sur des contenus réels, guide et prise en main.
5. Lot 3a : simulateur, tests sur des photos de vrais chantiers.
6. Bascule sur le nom de domaine.
7. Lot 3b si validé.

## 8. Nos recommandations à la cliente

Elle nous demande notre avis. Voici ce qu'on lui propose, à valider :

- Ajouter une page Réalisations. C'est ce qui vend le plus dans ce métier, et c'est ce qu'elle veut pouvoir alimenter elle-même.
- Transformer « Granulats de marbre » en nuancier visuel. Le texte seul ne parle pas, et ce nuancier sert aussi au simulateur.
- Mettre un vrai menu sur toutes les pages.
- Remplacer l'image d'accueil générée par IA par une photo d'un vrai chantier dès qu'elle en a une belle. Un visiteur qui découvre plus tard que la photo n'est pas réelle perd confiance. À défaut, la garder en attendant.
- Clarifier le nom : « Nataliia Résine » sur le site, « Nataliia Shevchuk » sur le logo. Il faut choisir le nom commercial et, si besoin, adapter le logo.
- Ajouter les mentions légales et la politique de confidentialité (obligatoire).
- Ajouter des avis clients si elle en a (Google, témoignages écrits), et le lien vers sa fiche Google Business si elle existe.
- Héberger le site dans un dépôt GitHub dédié plutôt que dans ce dépôt de test partagé avec d'autres projets : la cliente y aura un accès sans voir le reste.
- Ne pas passer en vente en ligne pour l'instant (la maquette n'en prévoit pas), sauf demande de sa part.

## 9. Fichiers du dossier

- `CAHIER_DES_CHARGES.md` : ce document.
- `QUESTIONS_CLIENT.md` : liste des informations à obtenir de la cliente.
- `assets/maquette/` : logo et image d'accueil récupérés depuis la maquette.
- `maquette-origine/` : copie brute des 5 pages et de la feuille de style de la maquette.
