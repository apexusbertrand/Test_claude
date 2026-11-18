# Nataliia Résine : cahier des charges du site web

Version 0.4, 18 novembre 2026. Document de cadrage, rien n'est encore développé.

Historique : 0.1 première version à partir de la maquette. 0.2 intègre les réponses de la cliente aux questions 1 à 18 et les données légales (Insee, RNE au 17/11/2026). 0.3 intègre les réponses 19 à 21 (administration). 0.4 intègre les réponses 29 à 32 (domaine, e-mail, Wix, délai) et ajoute un planning daté.

Sources utilisées, et uniquement elles :

- la maquette réalisée par la cliente avec une IA, en ligne sur https://headless-rkelfehhkdy-shevcukn-1408.wix-site-host.com/index.html (5 pages, copie archivée dans `maquette-origine/`) ;
- le message de la cliente (demande de site, accès administration, idée de simulation sur photo) ;
- ses réponses aux questions 1 à 21 et 29 à 32 de `QUESTIONS_CLIENT.md` ;
- la fiche entreprise (Insee, RNE).

Tout le reste est soit une proposition technique de notre part (signalée comme telle), soit une question encore ouverte dans `QUESTIONS_CLIENT.md`.

## 1. Contexte et objectifs

La cliente pose des revêtements extérieurs en moquette de pierre (granulats de marbre et résine) : terrasses, allées, escaliers d'entrée, abords de piscine, et d'autres surfaces (voir 4.1). Elle ne vend pas de produits aux particuliers : la page Produits présente les matériaux qu'elle utilise.

Elle a construit une maquette avec une IA et veut un vrai site, qu'elle puisse faire évoluer seule depuis son site, sans passer par GitHub.

Objectifs exprimés dans son message :

1. Reprendre la maquette et la compléter (certaines rubriques sont vides de son propre aveu).
2. Pouvoir ajouter et supprimer elle-même des photos de réalisations, des échantillons de granulats et des références de produits.
3. Permettre au visiteur de charger une photo de sa terrasse ou de son allée, de choisir un granulat et de voir le rendu sur sa propre photo.
4. Recevoir notre avis de professionnel sur ce qui est utile ou non.

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
| 2 | Administration autonome, accessible depuis son site (photos, réalisations, avis, granulats, produits) | lot 1 (structure des contenus) |
| 3 | Simulateur de rendu avant/après sur la photo du visiteur | lot 1, et le catalogue de granulats du lot 2 |

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
| Nuancier des granulats | message cliente | Une fiche par granulat : photo de l'échantillon, nom. Vide au lancement si la cliente n'a pas encore ses références, alimenté ensuite par l'administration. Sert aussi au simulateur (lot 3) |
| Contact | maquette + réponses 5 et 6 | Téléphone, WhatsApp, e-mail. Zone d'intervention : Somain, Valenciennes, Lille, Arras, Douai et alentours. Pas d'adresse, pas d'horaires |
| Créez votre projet | maquette | Remplacé par le simulateur du lot 3. En attendant, version actuelle conservée |
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

Site statique généré à partir de fichiers de contenu (Markdown/JSON), avec un générateur léger type Eleventy ou Astro. Les contenus que la cliente modifie (réalisations, avis, granulats, produits, textes) sont des fichiers séparés du code, ce qui permet à l'administration du lot 2 de les modifier sans toucher aux pages.

Déploiement : GitHub Actions vers GitHub Pages, à chaque modification, y compris celles faites par la cliente depuis l'administration.

### 5.6 Livrables

- Code source dans ce dépôt, dossier `nataliia-resine/` (ou dépôt dédié, voir 9).
- Préversion en ligne sur GitHub Pages.
- Bascule sur le nom de domaine (voir 5.7).

### 5.7 Nom de domaine et e-mail

- La cliente n'a pas de nom de domaine. On le choisit ensemble, on vérifie sa disponibilité et on l'achète chez le même registraire que les domaines Apexus et Belmaris, pour tout gérer au même endroit.
- Le site reste hébergé sur GitHub Pages. Le domaine pointe vers lui (enregistrements DNS, fichier `CNAME`, HTTPS activé dans GitHub Pages).
- Adresse e-mail sur le domaine (contact@…) : probablement souhaitée. GitHub Pages n'héberge pas d'e-mail, il faut une offre de messagerie à part (souvent incluse ou peu chère chez le registraire, sinon Zoho Mail, Infomaniak, Google Workspace). Possibilité de rediriger simplement contact@… vers l'adresse Gmail actuelle si elle veut garder sa boîte.
- Les demandes du site (formulaire, simulateur) arrivent sur nataliiaresine@gmail.com tant que l'adresse sur le domaine n'existe pas, puis sur la nouvelle.
- Configuration SPF, DKIM et DMARC du domaine pour que les e-mails envoyés par le site ne tombent pas en spam.

## 6. Lot 2 : administration autonome

### 6.1 Besoin

La cliente veut ajouter elle-même photos, réalisations, avis et références de produits. Elle travaillera depuis son téléphone et depuis son ordinateur (réponse 17). Elle n'a pas de compte GitHub et ne veut pas en avoir : l'administration se fait directement depuis son site (réponse 18), par exemple à l'adresse `/admin`, avec un identifiant et un mot de passe.

### 6.2 Ce que la cliente peut faire seule

Priorités de la cliente (réponse 19) : d'abord les réalisations de chantier, ensuite les produits. Ces deux écrans sont livrés en premier et soignés en priorité (ergonomie mobile, envoi de photos). Les autres contenus suivent dans le même lot.

| Contenu | Actions |
|---|---|
| Réalisations (priorité 1) | créer, modifier, supprimer, réordonner ; 3 à 4 photos après, 2 photos avant ; type de prestation ; produits utilisés, choisis dans la liste des produits |
| Avis clients | ajouter, modifier, masquer, supprimer ; nom ou initiales, ville, texte, date |
| Granulats (nuancier) | créer, modifier, supprimer ; photo de l'échantillon ; nom ; texture utilisée par le simulateur |
| Produits (priorité 2) | créer, modifier, supprimer une référence ; photo, nom, descriptif, lien fournisseur facultatif |
| Prestations | modifier texte et photo de chaque prestation, en ajouter ou en retirer |
| Coordonnées et zone | modifier téléphone, e-mail, villes desservies |
| Accueil | remplacer l'image principale et la phrase d'accroche |
| Logo | remplacer le fichier quand le nouveau logo sera prêt |

Hors périmètre de l'administration (reste à notre charge) : charte graphique, mise en page, création d'un nouveau type de page.

### 6.3 Exigences

- Accès par e-mail et mot de passe, sur la page `/admin` de son site, aucun compte GitHub côté cliente.
- Un seul compte au lancement. La solution doit permettre d'ajouter d'autres utilisateurs plus tard sans changer d'outil (réponse 21), chacun avec son propre identifiant pour savoir qui a modifié quoi.
- Interface en français, utilisable au doigt sur téléphone.
- Envoi de photos directement depuis le téléphone (appareil photo ou galerie), avec redimensionnement et compression automatiques en conservant une qualité plein écran (2560 px de large).
- Aperçu avant publication, publication en un clic, visible en ligne en quelques minutes.
- Historique : toute modification est réversible.
- Champs obligatoires contrôlés (pas de réalisation sans photo, texte alternatif proposé par défaut).
- Mot de passe oublié géré sans nous.
- Un guide d'utilisation de 2 ou 3 pages, avec captures, et une séance de prise en main.

### 6.4 Proposition technique

Contrainte : le site reste statique sur GitHub Pages, mais la cliente ne doit jamais voir GitHub. Il faut donc une couche d'authentification entre elle et le dépôt. C'est notre compte (ou un compte technique) qui écrit dans le dépôt, pas le sien.

Option recommandée : Decap CMS installé sur le site à l'adresse `/admin`, avec DecapBridge pour la connexion par e-mail et mot de passe. Les contenus restent des fichiers dans le dépôt, Git garde l'historique, rien à héberger. On invite la cliente par e-mail, elle crée son mot de passe, c'est tout. Point à vérifier avant engagement : conditions et pérennité de l'offre gratuite de DecapBridge.

Alternative 1 : même Decap CMS, mais avec notre propre petit service de connexion (fonction Cloudflare Workers, gratuite à ce volume) qui vérifie l'identifiant de la cliente et écrit dans GitHub via une application GitHub. Plus de travail initial, aucune dépendance à un service tiers. Moins adaptée au passage à plusieurs utilisateurs : la gestion des comptes (invitation, mot de passe oublié, retrait d'accès) serait à développer.

Alternative 2 : un CMS hébergé (Sanity par exemple), dont l'interface s'intègre à `/admin` sur son site. Les photos sont stockées et redimensionnées par le CMS, ce qui lève la limite de taille de GitHub Pages. Contrepartie : un service tiers de plus, gratuit à ce volume mais payant au-delà.

Volume : avec 6 photos par réalisation, compressées en WebP (400 à 700 Ko chacune), une réalisation pèse 3 à 4 Mo. GitHub Pages recommande moins de 1 Go par site, soit environ 200 réalisations. Au rythme annoncé (2 à 5 par mois, donc 24 à 60 par an), la limite arrive au bout de 3 à 8 ans. Prévoir dès le départ un rangement des photos qui permette de les déplacer plus tard vers un service d'images (Cloudinary, Cloudflare Images) sans retoucher les fiches.

Critère de choix ajouté par la réponse 21 : la gestion de plusieurs utilisateurs doit être native. C'est le cas de DecapBridge (invitations par e-mail) et de Sanity, ce qui conforte la recommandation.

## 7. Lot 3 : simulateur de rendu avant/après

### 7.1 Besoin

Message de la cliente : le futur client charge une photo de sa terrasse ou de son allée, choisit un échantillon de granulats et voit un aperçu du résultat sur sa propre photo. La maquette contient déjà une première version (page « Créez votre projet ») qui ne recolore pas la photo.

### 7.2 Parcours visiteur

1. Charger une photo (appareil photo du téléphone ou fichier).
2. Délimiter la surface à recouvrir (terrasse, allée, marches).
3. Choisir un granulat dans le nuancier (les vrais granulats saisis dans l'administration, et en attendant les 6 couleurs provisoires de la maquette).
4. Voir le rendu, comparer avant/après avec un curseur que l'on fait glisser sur l'image.
5. Changer de granulat et comparer.
6. Télécharger l'image avant/après.
7. Envoyer son projet à Nataliia : photo d'origine, rendu, granulat choisi, description, coordonnées.

### 7.3 Exigences fonctionnelles

- Délimitation de la zone : le visiteur pose des points autour de la surface (polygone), corrige au doigt ou à la souris, peut gommer les objets posés dessus (pot de fleurs, meuble).
- Perspective : la texture suit la perspective du sol. Le visiteur indique 4 points du sol (un rectangle vu en perspective), on en déduit l'inclinaison.
- Réalisme : la texture garde les ombres et la lumière de la photo d'origine, la taille du grain reste cohérente avec la distance.
- Curseur avant/après, plein écran sur téléphone.
- Envoi du projet avec la photo jointe automatiquement (et non plus via `mailto:`).
- Mention claire sous le rendu : aperçu indicatif, le rendu réel dépend du support et de la pose.

### 7.4 Exigences techniques

- Traitement dans le navigateur du visiteur : la photo ne quitte pas son appareil tant qu'il n'a pas cliqué sur « Envoyer ».
- Fonctionne sur les téléphones récents (iPhone Safari et Android Chrome), photos jusqu'à 12 mégapixels (réduites pour le calcul).
- Envoi du projet : GitHub Pages ne sait pas recevoir de formulaire. Il faut un service d'envoi (Formspree, Web3Forms, ou la fonction Cloudflare Workers de l'alternative 1 du lot 2) qui transmet la photo par e-mail à la cliente.
- Le nuancier vient des fiches granulats du lot 2 : chaque fiche porte une texture « simulateur » (photo prise à plat, bien éclairée, raccordable en mosaïque).

### 7.5 Découpage proposé

| Étape | Contenu | Commentaire |
|---|---|---|
| 3a | Zone dessinée à la main, perspective par 4 points, texture, curseur avant/après, téléchargement, envoi | Version fiable, livrée en premier |
| 3b | Détection automatique du sol par IA dans le navigateur (modèle de segmentation type SAM) : le visiteur touche la terrasse et la zone se sélectionne seule | Plus confortable, mais lourd à charger sur mobile et pas fiable à 100 % : l'outil manuel reste là pour corriger |
| Option | Rendu par IA générative (image refaite par un modèle) | Payant à chaque rendu, photo envoyée à un service tiers, résultat parfois éloigné du vrai granulat. Déconseillé pour l'instant |

### 7.6 Prérequis côté cliente

Le simulateur ne vaut que par la qualité des textures. Pour chaque granulat : une photo d'un échantillon posé à plat, prise à la verticale, en lumière du jour sans ombre portée, avec une règle ou une pièce pour l'échelle. Idéalement aussi 2 ou 3 photos avant/après de vrais chantiers pour vérifier que le simulateur ressemble au résultat réel.

Tant que les références de granulats ne sont pas connues (réponse 13), le simulateur fonctionne avec les 6 couleurs provisoires de la maquette, en aplat texturé générique.

## 8. Planning

Échéance demandée : 31 décembre 2026. Il reste 6 semaines, dont la période de Noël, où la cliente sera moins disponible pour relire et tester. On vise donc une mise en ligne le 22 décembre, la dernière semaine sert de marge.

| Semaine | Dates | Travail | Attendu de la cliente |
|---|---|---|---|
| 47 | 18 au 22 nov. | Validation du cahier des charges, choix du nom de domaine | Réponses aux questions ouvertes (A1 à A8, 22 à 28) |
| 48 | 23 au 29 nov. | Lot 1 : structure du site, pages, textes rédigés | Relecture des textes |
| 49 | 30 nov. au 6 déc. | Lot 1 : préversion GitHub Pages, recette. Lot 2 : écran Réalisations | Premières photos de chantier, retours sur la préversion |
| 50 | 7 au 13 déc. | Lot 2 : écran Produits, puis le reste. Lot 3a : début du simulateur | Premières références produits, test de l'administration |
| 51 | 14 au 20 déc. | Lot 3a : simulateur, envoi des demandes. Prise en main de l'administration, guide | Photos d'échantillons de granulats si disponibles, séance de prise en main |
| 52 | 21 au 27 déc. | Achat du domaine (fait plus tôt si possible), DNS, HTTPS, e-mail, retrait du `noindex`, mise en ligne le 22 | Validation finale |
| 53 | 28 au 31 déc. | Marge, corrections, dépublication de la maquette Wix | |

Ce qui est en ligne au 31 décembre : lot 1 complet, lot 2 complet, lot 3a (simulateur avec zone dessinée à la main).

Ce qui vient après le 31 décembre : lot 3b (détection automatique du sol), nouveau logo, nouvelle image d'accueil, textures des vrais granulats dans le simulateur si elles ne sont pas prêtes, mention de la décennale, comptes supplémentaires dans l'administration.

Risque principal : les contenus. Le site peut ouvrir avec peu de réalisations et un nuancier vide, mais pas sans textes validés ni sans les réponses aux questions bloquantes (nom commercial, mentions légales, nom de domaine). Si le 3a prend du retard, la page « Créez votre projet » actuelle reste en place et le simulateur arrive en janvier, sans bloquer le reste.

## 9. Nos recommandations à la cliente

- Page Réalisations avec avant/après : c'est ce qui vend le plus dans ce métier. Retenu.
- Nuancier visuel des granulats plutôt que du texte : retenu, il servira au simulateur.
- Vrai menu sur toutes les pages : retenu.
- Image d'accueil IA : gardée au lancement, à remplacer par une vraie photo de chantier dès que possible. Un visiteur qui découvre que la photo n'est pas réelle perd confiance.
- Logo : garder l'actuel au lancement. Au moment de refaire le logo vectoriel, harmoniser avec le nom commercial « Nataliia Résine » (le logo actuel affiche « NATALIIA SHEVCHUK » et « NS », le pied de page du site « NR »).
- Avis clients : retenu, à collecter avec accord écrit des clients.
- Assurance décennale : à souscrire avant les premiers chantiers si ce n'est pas déjà prévu, puis à afficher sur le site. Vérifier au passage le code NAF avec l'assureur.
- Fiche Google Business : à créer si elle n'existe pas. Gratuite, c'est ce qui ressort en premier dans une recherche locale (« moquette de pierre Douai »).
- Dépôt GitHub dédié à son site, séparé de ce dépôt de test partagé avec d'autres projets.
- Pas de vente en ligne : confirmé par la cliente.
- Adresse e-mail sur le nom de domaine : plus professionnelle que Gmail sur un devis. Une simple redirection vers sa boîte Gmail suffit pour commencer.

## 10. Fichiers du dossier

- `CAHIER_DES_CHARGES.md` : ce document.
- `QUESTIONS_CLIENT.md` : questions posées, réponses reçues et questions encore ouvertes.
- `assets/maquette/` : logo et image d'accueil récupérés depuis la maquette.
- `maquette-origine/` : copie brute des 5 pages et de la feuille de style de la maquette.
