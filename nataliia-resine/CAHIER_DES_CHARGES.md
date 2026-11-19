# Nataliia Résine : cahier des charges du site web

Version 0.8, 19 novembre 2026. Document de cadrage, rien n'est encore développé.

Historique : 0.1 première version à partir de la maquette. 0.2 intègre les réponses de la cliente aux questions 1 à 18 et les données légales (Insee, RNE au 17/11/2026). 0.3 intègre les réponses 19 à 21 (administration). 0.4 intègre les réponses 29 à 32 (domaine, e-mail, Wix, délai) et ajoute un planning daté. 0.5 intègre les réponses 22 à 28 (produits, simulateur, dossiers prospects, RGPD) : nouvelle architecture avec un service de données privé (chapitre 6). 0.6 : validation des points A12 à A15 et de l'architecture recommandée (P1). 0.7 : lot 1 en ligne, lot 2 réalisé (7.6). 0.8 : lot 3a réalisé (8.10).

Sources utilisées, et uniquement elles :

- la maquette réalisée par la cliente avec une IA, en ligne sur https://headless-rkelfehhkdy-shevcukn-1408.wix-site-host.com/index.html (5 pages, copie archivée dans `maquette-origine/`) ;
- le message de la cliente (demande de site, accès administration, idée de simulation sur photo) ;
- ses réponses aux questions 1 à 32 de `QUESTIONS_CLIENT.md` ;
- la fiche entreprise (Insee, RNE).

Tout le reste est soit une proposition technique de notre part (signalée comme telle), soit une question encore ouverte dans `QUESTIONS_CLIENT.md`.

## 1. Contexte et objectifs

La cliente pose des revêtements extérieurs en moquette de pierre (granulats de marbre et résine) : terrasses, allées, escaliers d'entrée, abords de piscine, et d'autres surfaces (voir 5.1). Elle ne vend pas de produits aux particuliers : la page Produits présente les matériaux qu'elle utilise.

Elle a construit une maquette avec une IA et veut un vrai site, qu'elle puisse faire évoluer seule depuis son site, sans passer par GitHub.

Objectifs exprimés dans son message :

1. Reprendre la maquette et la compléter (certaines rubriques sont vides de son propre aveu).
2. Pouvoir ajouter et supprimer elle-même des photos de réalisations, des échantillons de granulats et des références de produits.
3. Permettre au visiteur de charger une photo de sa terrasse ou de son allée, de choisir un granulat et de voir le rendu sur sa propre photo.
4. Conserver chaque demande de prospect (coordonnées, photos, simulations) dans un dossier, dans le respect du RGPD (réponse 28).
5. Recevoir notre avis de professionnel sur ce qui est utile ou non.

Hébergement : sur GitHub Pages (comme le site Guillaume l'Échanson déjà présent dans ce dépôt), d'abord à l'adresse GitHub, puis derrière un nom de domaine à choisir ensemble, géré comme les domaines Apexus (apexus.fr) ou Belmaris.

Échéance : mise en ligne au 31 décembre 2026. Le budget est géré par le prestataire, il ne figure pas dans ce document.

## 2. Informations légales de l'entreprise

| Élément | Valeur |
|---|---|
| Dirigeante | Nataliia SHEVCHUK |
| Forme juridique | Entrepreneur individuel |
| Nom commercial | Nataliia Résine |
| SIREN | 941 244 295 |
| SIRET (siège) | 941 244 295 00015 |
| TVA intracommunautaire | FR41941244295 |
| Date de création | 24 février 2025 |
| Adresse du siège | 15 route de Bouchain, 59490 Somain |
| Code NAF/APE déclaré | 8121Z, Nettoyage courant des bâtiments |
| Assurance décennale | pas encore souscrite |

Conséquences pour le site :

- L'adresse n'apparaît pas sur la page Contact (choix de la cliente). Elle doit en revanche figurer dans les mentions légales : la loi impose à un professionnel d'indiquer son adresse sur son site.
- Pour un entrepreneur individuel, le nom doit être suivi de « EI » ou « entrepreneur individuel » sur les documents et le site (règle en vigueur depuis mai 2022). Exemple en mentions légales : « Nataliia SHEVCHUK EI, nom commercial Nataliia Résine ».
- Pas d'assurance décennale pour l'instant : les textes du site ne parlent ni de garantie décennale ni d'assurance. À ajouter quand le contrat existera.
- Le code NAF déclaré (nettoyage de bâtiments) ne correspond pas à l'activité de pose de revêtements. Ce n'est pas un sujet pour le site, mais l'assureur pourra le relever au moment de la décennale. À signaler à la cliente.

## 3. Découpage en lots

| Lot | Contenu | Dépend de |
|---|---|---|
| 1 | Site vitrine (reprise et complément de la maquette) | contenus de la cliente, textes rédigés par nous |
| 2 | Administration autonome, accessible depuis son site (réalisations, produits, avis, demandes des prospects) | lot 1 (structure des contenus) |
| 3 | Simulateur de rendu avant/après sur la photo du visiteur, envoi de la demande, dossier prospect | lot 1, catalogue de produits et écran Demandes du lot 2 |

Les lots 2 et 3 peuvent démarrer en parallèle une fois le modèle de contenu du lot 1 figé.

## 4. Existant : ce que contient la maquette

### 4.1 Identité visuelle

- Logo fourni : `assets/maquette/logo-nataliia-resine.jpg` (1280 x 640, fond noir, monogramme « NS » doré sous un toit, texte « NATALIIA SHEVCHUK »). Décision : on le garde tel quel au lancement. Un nouveau logo, vectoriel, sera créé plus tard après échange avec la cliente. Le site doit permettre de le remplacer sans retouche (voir 6.2).
- Pas de version vectorielle ni de petite version pour l'icône d'onglet : on en tire une provisoire du logo actuel (monogramme NS recadré).
- Image d'accueil : `assets/maquette/hero-terrasse.png` (1672 x 941, 3,7 Mo). Image générée par IA, confirmé par la cliente. Décision : on la garde au lancement, elle sera remplacée plus tard par une vraie photo.
- Nom commercial affiché : « Nataliia Résine ». Le pied de page de la maquette utilise un monogramme texte « NR ». À harmoniser avec le logo NS (voir 9).
- Couleurs (variables CSS de la maquette) : noir `#11100f`, papier `#f7f4ee`, encre `#292722`, gris `#777168`, or `#b58a50`, or foncé `#8d6739`, or clair `#d2b27b`, filets `#e5ddd0`, blanc cassé `#fffefa`.
- Polices : Playfair Display (titres), DM Sans (texte), via Google Fonts.
- Signature : « Une solution créative. Un style qui vous ressemble. »
- Langue : français uniquement.

### 4.2 Pages

| Page | Fichier maquette | Contenu actuel |
|---|---|---|
| Accueil | `index.html` | Image plein écran, 4 onglets de navigation posés sur l'image (Produits, Prestations, Contact, Créez votre projet), signature |
| Produits | `produits.html` | 2 cartes texte : Granulats de marbre, Profil de finition. Bouton « Demander des informations » vers Contact. Aucune photo |
| Prestations | `prestations.html` | 4 cartes texte : Terrasses, Allées, Escaliers d'entrée, Abords de piscine. Bouton « Parlons de votre projet ». Aucune photo |
| Contact | `contact.html` | Téléphone 07 81 07 21 22, WhatsApp (même numéro), e-mail nataliiaresine@gmail.com. Pas de formulaire, pas d'adresse |
| Créez votre projet | `configurateur.html` | Chargement d'une photo, choix parmi 6 couleurs provisoires (Ivoire, Beige sable, Pierre, Gris, Gris foncé, Noir), champ description, envoi par ouverture de la messagerie (la photo n'est pas jointe automatiquement, la couleur ne s'applique pas sur la photo) |

La maquette ne contient aucun lien vers des réseaux sociaux (Instagram, Facebook, Google Business). Seul WhatsApp est présent.

### 4.3 Limites constatées

- Le bouton « menu » (3 traits) en haut à droite renvoie seulement vers l'accueil. Il n'y a pas de vrai menu sur les pages intérieures.
- Aucune page Réalisations, Avis ni nuancier de granulats, alors que la cliente en veut.
- Aucune mention légale, aucune politique de confidentialité.
- La maquette tourne sur un compte Wix gratuit (script Wix chargé dans les pages). On ne la réutilise pas : on repart des fichiers HTML/CSS. Rien à résilier, mais la maquette devra être dépubliée à la mise en ligne pour ne pas laisser traîner un second site en ligne.
- L'image d'accueil pèse 3,7 Mo : à convertir en WebP/AVIF et décliner en plusieurs tailles.
- Le configurateur envoie un `mailto:` : le visiteur doit joindre sa photo à la main, beaucoup ne le feront pas.

## 5. Lot 1 : site vitrine

### 5.1 Pages

| Page | Source | Contenu prévu |
|---|---|---|
| Accueil | maquette | Image plein écran et onglets conservés. Sous la signature : dernières réalisations, extrait des avis clients, zone d'intervention |
| Prestations | maquette + réponse 12 | Une carte par prestation, avec photo dès qu'il y en a : Terrasses, Allées, Escaliers d'entrée, Abords de piscine (maquette), plus Intérieur, Balcons, Plages de garage, Rénovation (réponse 12, liste à confirmer). Textes rédigés par nous (voir 5.3) |
| Réalisations | message cliente + réponse 9 | Galerie filtrable par type de prestation. Chaque réalisation a sa page (voir 5.2) |
| Avis clients | réponse 15 | Témoignages publiés avec l'accord des clients, gérés dans l'administration |
| Produits | maquette + réponses 13, 14 et 19 | Présentation des matériaux utilisés : granulats, profils de finition. Chaque produit : photo, descriptif, lien vers le fournisseur si elle le souhaite. Pas de prix, pas de vente, pas de panier. Bouton « Demander des informations ». Références ajoutées par la cliente dans l'administration |
| Fiche produit | réponses 22 et 23 | Une page par produit : photos, descriptif, lien, PDF (tout facultatif, voir 7.3). Pour un granulat, bouton « Simuler sur ma photo » s'il a une texture. 2 produits d'exemple au lancement |
| Contact | maquette + réponses 5 et 6 | Téléphone, WhatsApp, e-mail. Zone d'intervention : Somain, Valenciennes, Lille, Arras, Douai et alentours. Pas d'adresse, pas d'horaires |
| Créez votre projet | maquette | Remplacé par le simulateur du lot 3 (chapitre 8) |
| Mentions légales | obligation légale | Données du chapitre 2, hébergeur, propriété des contenus |
| Politique de confidentialité | RGPD | Indispensable dès que le visiteur envoie une photo ou ses coordonnées |

Navigation : vrai menu sur toutes les pages (ouvert au clic sur les 3 traits, sur mobile comme sur ordinateur), en plus des onglets de l'accueil.

### 5.2 Une réalisation

D'après la réponse 9 :

- 3 à 4 photos « après » et 2 photos « avant », assez grandes pour un affichage plein écran sur ordinateur (2560 px de large conseillé, 1920 px minimum) ;
- un curseur avant/après sur la page de la réalisation (même composant que le simulateur du lot 3) ;
- les références des produits utilisés (granulat, profil), avec lien vers leur fiche ;
- type de prestation, ville (si la cliente accepte de l'afficher), titre, court texte.

Les photos avant/après sont ajoutées par la cliente elle-même dans l'administration (réponse 10), à raison de 2 à 5 réalisations par mois (réponse 20). Au lancement, la page Réalisations peut donc être vide ou ne contenir que ce qu'elle aura chargé pendant la recette.

### 5.3 Textes

La cliente nous laisse rédiger une première version des textes (réponse 11), qu'elle adaptera ensuite dans l'administration. On rédige : présentation de chaque prestation, étapes d'une pose (préparation du support, mélange granulats et résine, application, finition), entretien, avantages de la moquette de pierre (drainante, antidérapante, esthétique).

Règles : aucun chiffre non vérifié (durée de vie, prix au mètre carré, délai), aucune mention de garantie ou d'assurance tant que la décennale n'est pas souscrite. Tous les textes passent en validation chez la cliente avant mise en ligne.

### 5.4 Exigences

- Responsive : téléphone d'abord, tablette, ordinateur.
- Accessibilité : contrastes suffisants, textes alternatifs sur toutes les images, navigation au clavier.
- Performance : images optimisées (WebP/AVIF, plusieurs tailles, chargement différé). Les photos plein écran sont servies en grand uniquement sur grand écran. Objectif Lighthouse supérieur à 90 sur mobile.
- Référencement local : titre et description par page qui citent la zone (Somain, Valenciennes, Lille, Arras, Douai), données structurées « LocalBusiness » avec zone desservie et sans adresse publique, sitemap, Open Graph pour le partage sur WhatsApp.
- Préversion sur GitHub Pages en `noindex`, à retirer au passage sur le nom de domaine.

### 5.5 Proposition technique

Site statique généré à partir de fichiers de contenu (Markdown/JSON), avec un générateur léger type Eleventy ou Astro. Les contenus que la cliente modifie (réalisations, avis, produits, textes) sont stockés à part du code (chapitre 6). À chaque publication, le site est reconstruit à partir de ces contenus.

Déploiement : GitHub Actions vers GitHub Pages, à chaque modification, y compris celles faites par la cliente depuis l'administration.

### 5.6 Livrables

- Code source dans ce dépôt, dossier `nataliia-resine/` (ou dépôt dédié, voir 9).
- Préversion en ligne sur GitHub Pages.
- Bascule sur le nom de domaine (voir 5.7).

### 5.7 Nom de domaine et e-mail

- La cliente n'a pas de nom de domaine. On le choisit ensemble, on vérifie sa disponibilité et on l'achète chez le même registraire que les domaines Apexus et Belmaris, pour tout gérer au même endroit.
- Le site reste hébergé sur GitHub Pages. Le domaine pointe vers lui (enregistrements DNS, fichier `CNAME`, HTTPS activé dans GitHub Pages).
- Adresse e-mail sur le domaine (contact@…) : probablement souhaitée. GitHub Pages n'héberge pas d'e-mail, il faut une offre de messagerie à part (souvent incluse ou peu chère chez le registraire, sinon Zoho Mail, Infomaniak, Google Workspace). Décision (A10) : simple redirection de contact@… vers nataliiaresine@gmail.com, pas de boîte séparée.
- Les alertes de nouvelles demandes arrivent sur nataliiaresine@gmail.com (réponse 27).
- Configuration SPF, DKIM et DMARC du domaine pour que les e-mails envoyés par le site ne tombent pas en spam.

## 6. Architecture générale (validée)

Les réponses 18 et 28 imposent deux choses que GitHub Pages ne sait pas faire seul :

- une administration sur son site, avec identifiant et mot de passe, sans compte GitHub ;
- la conservation privée des demandes des prospects (coordonnées, photos, simulations), une fiche par prospect, conforme au RGPD.

Ces données privées ne doivent jamais aller dans le dépôt GitHub : un site GitHub Pages et son dépôt sont publics ou facilement exposés. Il faut donc un service à part pour l'authentification, la base de données et le stockage privé des photos.

Proposition :

| Brique | Rôle | Outil proposé |
|---|---|---|
| Site public | Pages, réalisations, produits, simulateur | Site statique (Eleventy ou Astro) sur GitHub Pages, puis nom de domaine |
| Administration | Écrans `/admin` sur son site : réalisations, produits, avis, textes, demandes | Interface développée par nous, servie par le site lui-même |
| Authentification, base, stockage | Comptes administrateurs, contenus, fichiers, dossiers prospects | Supabase, région Union européenne (Francfort), offre gratuite au démarrage |
| Publication | Reconstruire le site public quand la cliente publie un contenu | GitHub Actions déclenché par l'administration |
| E-mails | Alerte « nouvelle demande » vers nataliiaresine@gmail.com | Service d'envoi transactionnel (Brevo, Resend) ou fonction Supabase |

Pourquoi ce choix plutôt que Decap CMS (proposé en version 0.4) : Decap ne gère que les contenus publics stockés dans GitHub. Il ne sait pas afficher des dossiers prospects privés. On aurait eu deux outils et deux connexions. Avec Supabase, une seule connexion donne accès à tout, plusieurs utilisateurs sont gérés nativement (réponse 21), les photos ne sont plus limitées par le 1 Go de GitHub Pages, et les données restent hébergées en Europe.

Contrepartie : les écrans d'administration sont développés sur mesure (pas d'interface toute faite), donc plus de travail sur le lot 2, et un service tiers de plus à suivre. L'offre gratuite de Supabase suffit au volume prévu (2 à 5 réalisations par mois, quelques demandes par semaine). Point à surveiller : un projet gratuit inactif plusieurs jours est mis en pause par Supabase. Un passage à l'offre payante (environ 25 dollars par mois) peut devenir nécessaire.

Alternative écartée : Decap + DecapBridge pour les contenus et Supabase seulement pour les dossiers prospects. Plus rapide sur la partie contenus, mais deux connexions distinctes pour la cliente. Décision P1 : administration sur mesure sur Supabase, une seule connexion.

## 7. Lot 2 : administration autonome

### 7.1 Besoin

La cliente veut ajouter elle-même photos, réalisations, avis et produits. Elle travaille depuis son téléphone et depuis son ordinateur (réponse 17). Pas de compte GitHub : l'administration est sur son site, à l'adresse `/admin`, avec un identifiant et un mot de passe (réponse 18). Elle y consulte aussi les demandes de ses prospects (réponse 28).

### 7.2 Écrans

Priorités de la cliente (réponse 19) : d'abord les réalisations, ensuite les produits. On y ajoute les demandes des prospects, nécessaires dès la mise en ligne du simulateur.

| Écran | Actions |
|---|---|
| Réalisations (priorité 1) | créer, modifier, supprimer, réordonner ; 3 à 4 photos après, 2 photos avant ; type de prestation ; produits utilisés, choisis dans la liste des produits |
| Produits (priorité 2) | créer, modifier, supprimer ; voir 7.3 |
| Demandes (dossiers prospects) | liste des demandes, fiche détaillée, suivi, suppression ; voir 8.5 |
| Avis clients | ajouter, modifier, masquer, supprimer ; nom ou initiales, ville, texte, date |
| Prestations | modifier texte et photo de chaque prestation, en ajouter ou en retirer |
| Coordonnées et zone | modifier téléphone, e-mail, villes desservies |
| Accueil | remplacer l'image principale et la phrase d'accroche |
| Logo | remplacer le fichier quand le nouveau logo sera prêt |
| Utilisateurs | un seul compte au lancement, ajout d'autres comptes plus tard |

Hors périmètre de l'administration (reste à notre charge) : charte graphique, mise en page, création d'un nouveau type de page.

### 7.3 Fiche produit

D'après les réponses 19, 22, 23 et 24 :

| Champ | Obligatoire | Commentaire |
|---|---|---|
| Nom | oui | |
| Catégorie | oui | granulat, profil de finition, autre |
| Descriptif | non | |
| Photos | non | une ou plusieurs |
| Lien | non | fournisseur ou fiche produit (voir A8) |
| Document PDF | non | fiche technique par exemple |
| Texture pour le simulateur | non | granulats uniquement. Photo de l'échantillon prise à plat (voir 8.6) |
| Taille du grain (mm) | non | granulats uniquement. Sert à mettre la texture à la bonne échelle dans le simulateur |

Pas de prix (réponse 23).

Un granulat n'apparaît dans le simulateur que s'il a une texture. Sans texture, il reste visible sur la page Produits mais n'est pas proposé à la simulation. L'administration l'indique clairement à la cliente au moment de l'enregistrement.

À la livraison, le site contient 2 produits d'exemple, un granulat clair et un granulat foncé, marqués « Exemple », avec une texture de démonstration (validé, A15), pour que le simulateur fonctionne dès le premier jour. La cliente les supprime quand elle a saisi ses vrais granulats (réponse 22).

### 7.4 Exigences

- Accès par e-mail et mot de passe sur `/admin`, mot de passe oublié géré sans nous.
- Interface en français, utilisable au doigt sur téléphone.
- Envoi de photos depuis le téléphone (appareil photo ou galerie), redimensionnement et compression automatiques en conservant une qualité plein écran (2560 px de large).
- Aperçu avant publication, publication en un clic, visible en ligne en quelques minutes.
- Historique des contenus publiés : possibilité de revenir en arrière.
- Champs obligatoires contrôlés (pas de réalisation sans photo, texte alternatif proposé par défaut).
- Un seul compte au lancement, ajout d'autres comptes sans changer d'outil (réponse 21). Chaque utilisateur a son identifiant, les modifications sont tracées.
- Un guide d'utilisation de 2 ou 3 pages, avec captures, et une séance de prise en main.

### 7.5 Volume

2 à 5 réalisations par mois (réponse 20), 6 photos chacune, soit 15 à 20 Mo par mois de photos compressées. Les photos sont stockées dans Supabase et non dans le dépôt GitHub : la limite de 1 Go de GitHub Pages ne s'applique plus. L'offre gratuite de Supabase (1 Go de fichiers) tient 4 à 5 ans à ce rythme, en comptant les dossiers prospects ; au-delà, l'offre payante.

## 8. Lot 3 : simulateur de rendu avant/après

### 8.1 Besoin

Message de la cliente : le futur client charge une photo de sa terrasse ou de son allée, choisit un granulat et voit le résultat sur sa propre photo. Réponses 24 à 28 : la simulation utilise un vrai granulat du catalogue (couleur et grain), un seul granulat par simulation, 1 ou 2 photos par demande, et chaque demande crée un dossier prospect conservé.

### 8.2 Parcours visiteur

1. Charger une photo (appareil photo du téléphone ou fichier). Une deuxième photo est possible, pas plus.
2. Délimiter la surface à recouvrir (terrasse, allée, marches).
3. Choisir un granulat parmi ceux du catalogue qui ont une texture. Un seul granulat pour toute la surface, pas de mélange ni de bordure (réponse 25).
4. Voir le rendu avec la couleur et le grain du granulat choisi, comparer avant/après avec un curseur.
5. Changer de granulat et comparer.
6. Recommencer pour la deuxième photo si besoin.
7. Télécharger les images avant/après.
8. Envoyer sa demande (voir 8.4).

Les 6 couleurs provisoires de la maquette (Ivoire, Beige sable, Pierre, Gris, Gris foncé, Noir) disparaissent : une simulation se fait toujours avec une référence de granulat (réponse 24).

### 8.3 Exigences fonctionnelles

- Délimitation de la zone : le visiteur pose des points autour de la surface (polygone), corrige au doigt ou à la souris, peut gommer les objets posés dessus (pot de fleurs, meuble).
- Perspective : la texture suit la perspective du sol. Le visiteur indique 4 points du sol (un rectangle vu en perspective), on en déduit l'inclinaison.
- Réalisme : la texture du granulat est répétée sans raccord visible, à l'échelle de sa taille de grain, et garde les ombres et la lumière de la photo d'origine.
- Curseur avant/après, plein écran sur téléphone.
- Mention sous le rendu : aperçu indicatif, le rendu réel dépend du support et de la pose.

### 8.4 Demande envoyée par le visiteur

Contenu (réponse 26) :

| Champ | Obligatoire |
|---|---|
| Nom | oui |
| Téléphone | oui |
| E-mail | oui |
| Ville | oui |
| Surface approximative (m²) | non |
| Type de surface (terrasse, allée, escalier, piscine, autre) | oui |
| Délai souhaité | non |
| Message libre | non |
| Photos d'origine | 1 ou 2, obligatoires |
| Rendus simulés, avec le granulat choisi pour chacun | générés automatiquement |
| Consentement au traitement des données | case à cocher obligatoire, non précochée |

À l'envoi : création du dossier prospect, e-mail d'alerte à nataliiaresine@gmail.com (réponse 27) avec le résumé de la demande et un lien vers le dossier dans l'administration. E-mail de confirmation au visiteur avec ses rendus.

### 8.5 Dossiers prospects

Un dossier par demande (réponse 28). Si la même adresse e-mail revient, la nouvelle demande est rattachée au même prospect.

Contenu d'un dossier : coordonnées, ville, type et surface, délai, message, photos d'origine, rendus, granulat(s) choisi(s), date de la demande. Chaque dossier a aussi un statut de suivi (nouveau, contacté, devis envoyé, signé, sans suite) et un champ de notes pour la cliente (validé, A13).

Dans l'administration : liste triable par date et statut, fiche détaillée, téléchargement des photos, suppression d'un dossier.

### 8.6 RGPD

La conservation des photos et des simulations est acceptée par la cliente, avec une gestion RGPD adéquate (réponse 28). Ce que ça implique concrètement :

- Information du visiteur avant l'envoi : qui traite les données (Nataliia SHEVCHUK EI), pourquoi (répondre à la demande, établir un devis), combien de temps, ses droits, comment les exercer. Détail dans la politique de confidentialité.
- Consentement par case à cocher, non précochée.
- Stockage privé, hébergé en Union européenne. Les photos ne sont jamais accessibles par une adresse publique : l'administration les affiche par des liens temporaires.
- Accès limité aux comptes de l'administration.
- Durée de conservation : 3 ans après le dernier contact pour un prospect sans suite (durée recommandée par la CNIL pour la prospection, validée par la cliente, A14), puis suppression automatique. Pour un client signé, le dossier relève de la gestion commerciale, durée à fixer avec elle.
- Droits du visiteur : accès, rectification, suppression. La cliente peut exporter ou supprimer un dossier en un clic depuis l'administration.
- Contrat de sous-traitance (DPA) avec Supabase et le service d'e-mails, inscription du traitement dans un registre simple (modèle fourni).
- Les photos peuvent montrer une maison, une plaque d'immatriculation, des personnes : on le signale au visiteur et on lui conseille de cadrer sur la surface à traiter.

### 8.7 Exigences techniques

- La simulation se calcule dans le navigateur du visiteur. Rien n'est envoyé avant le clic sur « Envoyer ».
- Fonctionne sur les téléphones récents (iPhone Safari et Android Chrome), photos jusqu'à 12 mégapixels (réduites pour le calcul, compressées avant envoi).
- Protection anti-spam du formulaire (captcha discret type Cloudflare Turnstile, limite de taille et de nombre de fichiers).

### 8.8 Découpage proposé

| Étape | Contenu | Commentaire |
|---|---|---|
| 3a | Zone dessinée à la main, perspective par 4 points, texture du granulat, curseur avant/après, 2 photos max, envoi, dossier prospect, RGPD | Livré pour la mise en ligne |
| 3b | Détection automatique du sol par IA dans le navigateur (modèle de segmentation type SAM) | Après la mise en ligne. L'outil manuel reste là pour corriger |
| Option | Rendu par IA générative | Payant à chaque rendu, résultat parfois éloigné du vrai granulat. Déconseillé pour l'instant |

### 8.9 Texture d'un granulat

Le rendu ne vaut que par la qualité de la texture. Pour chaque granulat, la cliente charge dans l'administration une photo d'échantillon posé à plat, prise à la verticale, en lumière du jour sans ombre portée, sur une surface d'au moins 30 x 30 cm, et indique la taille du grain. Une fiche de prise de vue d'une page lui est fournie avec le guide.

### 7.6 Réalisation (lot 2, 19 novembre 2026)

Livré dans `admin/` et `supabase/`, mise en service décrite dans `INSTALLATION_ADMIN.md`.

- Écrans : accueil avec bouton « Publier le site » et indicateur de modifications en attente, réalisations, produits, demandes, avis, prestations, réglages (coordonnées, zone, accroche, image d'accueil, logo), historique avec retour arrière, compte.
- Photos traitées dans le navigateur avant envoi : rotation, 2560 px et 800 px, WebP (JPEG sur les navigateurs qui n'encodent pas le WebP). Alerte sous 1920 px de large. Description proposée par défaut.
- Aperçu des réalisations et produits avec la feuille de style du site, brouillons (« Visible sur le site »), confirmation avant de quitter une saisie non enregistrée.
- Droits testés sur Postgres 16 (`supabase/tests/run.sh`) : visiteur, compte connecté non autorisé, administratrice, contraintes (1 à 4 photos après, 2 avant au plus), purge à 3 ans.
- Mode démonstration tant que Supabase n'est pas configuré : la cliente peut essayer l'outil sans risque.
- Écran Demandes prêt, alimenté au lot 3 (tables `prospects` et `demandes`, stockage privé).
- Non testé faute de projet Supabase : la connexion réelle, l'envoi vers le stockage Supabase et la fonction « publier ». À vérifier à la mise en service (étape 9 de `INSTALLATION_ADMIN.md`).

### 8.10 Réalisation (lot 3a, 19 novembre 2026)

Livré : `site/static/js/sim-engine.js` (moteur), `site/static/js/simulateur.js` (interface), page `projet.html`, fonctions `supabase/functions/demande` et `supabase/functions/purger`.

- Parcours conforme à 8.2 : 1 ou 2 photos, contour de la surface et objets à exclure (points déplaçables au doigt), perspective par 4 points avec quadrillage au sol et longueur réelle du rectangle, granulat du catalogue (présélectionné depuis « Essayer sur ma photo »), comparateur avant/après, téléchargement, envoi. Mode plein écran pour le téléphone.
- Rendu : texture à l'échelle (échantillon de 30 cm), répétée en miroir sans raccord, filtrée selon la distance, éclairée avec la lumière du sol seulement (ombres conservées sans recopier l'ancien revêtement), teintée par la lumière de la scène, bords adoucis. Environ 0,2 s pour une photo de 1600 px.
- Demande : validation côté serveur (contenu réel des images vérifié), anti-robot Turnstile, champ piège, 3 demandes par heure et par adresse au plus, photos dans le stockage privé, dossier par adresse e-mail, alerte à la cliente avec lien vers le dossier, confirmation au visiteur avec ses simulations, retour arrière si l'enregistrement échoue.
- RGPD : consentement explicite, politique de confidentialité mise à jour, suppression d'un dossier = suppression des photos, purge automatique à 3 ans par la fonction `purger`.
- Tests : moteur (10 tests Node), fonctions (11 tests Deno contre de faux services), parcours complet dans Chromium sur téléphone jusqu'au dossier dans l'administration.
- En préversion, sans Supabase : la demande est déposée dans l'administration de démonstration du même navigateur, aucun e-mail ne part.
- Limites : la qualité dépend des textures fournies (voir 8.9) ; la zone se trace à la main (la détection automatique est le lot 3b).

## 9. Planning

Échéance demandée : 31 décembre 2026. Mise en ligne visée le 22 décembre, la dernière semaine sert de marge (période de Noël).

| Semaine | Dates | Travail | Attendu de la cliente |
|---|---|---|---|
| 47 | 18 au 22 nov. | Validation du cahier des charges, choix du nom de domaine | Réponses aux questions ouvertes |
| 48 | 23 au 29 nov. | Lot 1 : structure, pages, textes. Mise en place de Supabase (comptes, base, stockage) | Relecture des textes |
| 49 | 30 nov. au 6 déc. | Lot 1 : préversion GitHub Pages. Lot 2 : connexion, écran Réalisations, publication | Premières photos de chantier, retours sur la préversion |
| 50 | 7 au 13 déc. | Lot 2 : écran Produits, 2 produits d'exemple, autres écrans. Lot 3a : simulateur | Test de l'administration |
| 51 | 14 au 20 déc. | Lot 3a : envoi, dossiers prospects, e-mails, RGPD. Prise en main, guide | Premier vrai granulat avec texture si possible |
| 52 | 21 au 27 déc. | Domaine, DNS, HTTPS, e-mail, retrait du `noindex`, mise en ligne le 22 | Validation finale |
| 53 | 28 au 31 déc. | Marge, corrections, dépublication de la maquette Wix | |

En ligne au 31 décembre : lots 1, 2 et 3a.

Après le 31 décembre : lot 3b, nouveau logo, nouvelle image d'accueil, mention de la décennale, comptes supplémentaires.

Risques :

- Le lot 2 sur mesure et les dossiers prospects ajoutent du travail par rapport à la version 0.4. Le planning est serré. Si on prend du retard, on met en ligne le 22 avec les écrans Réalisations, Produits et Demandes, et les écrans secondaires (avis, prestations, accueil, logo) arrivent en janvier ; d'ici là, on fait ces modifications pour elle.
- Les contenus : textes validés, nom commercial confirmé et nom de domaine sont bloquants pour la mise en ligne.

## 10. Nos recommandations à la cliente

- Page Réalisations avec avant/après : c'est ce qui vend le plus dans ce métier. Retenu.
- Fiches granulats avec texture : retenu, elles servent au simulateur.
- Vrai menu sur toutes les pages : retenu.
- Image d'accueil IA : gardée au lancement, à remplacer par une vraie photo de chantier dès que possible. Un visiteur qui découvre que la photo n'est pas réelle perd confiance.
- Logo : garder l'actuel au lancement. Au moment de refaire le logo vectoriel, harmoniser avec le nom commercial « Nataliia Résine » (le logo actuel affiche « NATALIIA SHEVCHUK » et « NS », le pied de page du site « NR »).
- Avis clients : retenu, à collecter avec accord écrit des clients.
- Assurance décennale : à souscrire avant les premiers chantiers si ce n'est pas déjà prévu, puis à afficher sur le site. Vérifier au passage le code NAF avec l'assureur.
- Fiche Google Business : à créer si elle n'existe pas. Gratuite, c'est ce qui ressort en premier dans une recherche locale (« moquette de pierre Douai »).
- Dépôt GitHub dédié à son site, séparé de ce dépôt de test partagé avec d'autres projets.
- Ne jamais stocker les données des prospects dans GitHub : base et stockage privés en Europe (chapitre 6).
- Pas de vente en ligne : confirmé par la cliente.
- Adresse e-mail sur le nom de domaine : plus professionnelle que Gmail sur un devis. Une simple redirection vers sa boîte Gmail suffit pour commencer.

## 11. Fichiers du dossier

- `CAHIER_DES_CHARGES.md` : ce document.
- `QUESTIONS_CLIENT.md` : questions posées, réponses reçues et questions encore ouvertes.
- `assets/maquette/` : logo et image d'accueil récupérés depuis la maquette.
- `maquette-origine/` : copie brute des 5 pages et de la feuille de style de la maquette.
