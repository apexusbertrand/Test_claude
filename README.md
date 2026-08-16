# PhotoTag

Application web (PWA) de tri et de tag automatique de photos, utilisable sur PC et sur smartphone, sans serveur à maintenir : tout tourne dans le navigateur.

## Fonctionnalités

1. **Choisir un dossier** de photos sur votre appareil : l'app le parcourt, analyse chaque image et lui attribue automatiquement des tags (lieu, personne, animal, événement). Les fichiers originaux ne sont jamais modifiés — seule la lecture y est faite, jamais l'écriture.
2. **Miniatures automatiques** : à chaque nouvelle photo détectée, une miniature est générée et enregistrée par défaut sous `photos/miniatures/<événement>/`, **à l'intérieur du dossier choisi**. Choisir un autre emplacement (ex. la racine du téléphone, pour l'indépendance vis-à-vis du dossier source) reste possible via un bouton **optionnel**.
3. **Recherche par tag** : une barre de recherche + des filtres par catégorie (Lieu / Personne / Animal / Événement) affichent les miniatures correspondantes. Cliquer sur une photo tente d'ouvrir l'original — possible uniquement si le fichier original est accessible depuis l'appareil en cours (même disque/support), sinon un message l'indique.
4. **Page principale** : dès l'ouverture, toutes les miniatures déjà présentes dans l'emplacement de destination s'affichent immédiatement (avec leurs tags), sans attendre de re-choisir le dossier source. Ajout/suppression manuelle de tags, et bouton **Partager** (API Web Share : envoie l'original si disponible sur l'appareil courant, sinon la miniature) fonctionnent directement depuis cette vue.

Catégories de tags : `lieu`, `personne`, `animal`, `evenement`, `autre`.

## Comment ça marche (choix techniques)

- **Aucun serveur applicatif.** L'app est une PWA statique. L'accès aux dossiers se fait via la **File System Access API** du navigateur (Chrome/Edge, desktop et Android) : c'est elle qui garantit qu'on ne peut ouvrir l'original que si le fichier est réellement sur l'appareil utilisé — exactement le comportement demandé au point 3.
- **Emplacement des miniatures : par défaut à l'intérieur du dossier source, indépendant en option.** Par défaut, dès que vous choisissez le dossier de photos, l'app crée/utilise automatiquement `photos/miniatures/<événement>/` à l'intérieur de ce même dossier — aucune deuxième sélection n'est nécessaire, les miniatures déjà présentes s'affichent immédiatement. Un bouton optionnel *"Utiliser un autre emplacement pour les miniatures"* permet de basculer vers un emplacement totalement indépendant (ex. la racine du téléphone/stockage) ; dans ce cas, la structure devient `<emplacement choisi>/photos/miniature/<nom du dossier source>/<événement>/`, ce qui permet à plusieurs dossiers source de partager un même emplacement sans collision.
- **Les photos originales ne sont jamais modifiées.** Le code ne fait jamais d'écriture sur le fichier original : il est uniquement lu (`getFile()`) pour générer la miniature et calculer les tags.
- **Deux couches de stockage des tags, jamais dans l'original** :
  1. Un **sidecar JSON** écrit à côté de chaque miniature (`photos/miniatures/<événement>/<nom>.json` par défaut, ou `photos/miniature/<dossier source>/<événement>/<nom>.json` en emplacement personnalisé) : tags, chemin relatif vers l'original, date, GPS, descripteurs de visages, etc. C'est la source de vérité, durable sur le disque — elle survit à un vidage du cache/des données du navigateur.
  2. **IndexedDB**, en local dans le navigateur, sert de cache rapide pour l'affichage (et stocke aussi les handles de fichiers, propres au navigateur et non portables, donc impossibles à mettre dans un JSON). Si IndexedDB est vidé, le prochain scan retrouve automatiquement les sidecars sur le disque et restaure les tags — sans relancer l'analyse IA.
- **Tags automatiques, 100 % local (hors géocodage)** :
  - *Événement* : les photos sont regroupées chronologiquement (nouvel événement dès 6h sans photo, ou au-delà de 3 jours) à partir de la date EXIF.
  - *Lieu* : si la photo contient des coordonnées GPS EXIF, une requête de géocodage inverse (OpenStreetMap/Nominatim, gratuit, sans clé) donne la ville/le pays. Nécessite une connexion internet ; sans réseau, ce tag est simplement omis.
  - *Animal / présence de personnes* : détection d'objets locale, dans le navigateur, via TensorFlow.js (modèle COCO-SSD léger). Détecte les animaux courants (chien, chat, oiseau, cheval, etc.) et compte les personnes présentes.
  - *Personnes nommées* : reconnaissance faciale locale (dans le navigateur, hors ligne, via `@vladmandic/face-api`, modèles embarqués dans `public/models/faceapi/`). Chaque photo analysée a ses visages détectés et convertis en empreintes (descripteurs) stockées avec elle. Dès qu'un tag "Personne" contenant un prénom est ajouté **manuellement** sur une photo où un seul visage est détecté, ce visage sert d'exemple pour ce prénom. Au **scan suivant** ("Analyser le dossier"), ce prénom est automatiquement proposé sur toutes les autres photos dont un visage correspond d'assez près. Cette fonctionnalité de reconnaissance nominative n'est activée que parce que l'app est destinée à un usage strictement privé/personnel — ne pas déployer publiquement une instance qui l'utilise sur des photos de tiers sans leur consentement.
- **Affichage immédiat, données persistantes.** Au chargement, l'app affiche d'abord tout ce qu'elle a déjà en cache local (IndexedDB), instantanément. Elle tente ensuite de restaurer l'accès aux dossiers, mais uniquement en *interrogeant* la permission déjà accordée — jamais en la redemandant toute seule, ce que les navigateurs interdisent hors d'un clic (et qui, avant correction, empêchait toute restauration silencieuse). Si le navigateur n'a pas gardé la permission en mémoire d'une session à l'autre (fréquent, surtout sur Android), un bouton **"🔄 Reconnecter mes dossiers"** apparaît : un clic suffit pour redemander l'accès aux *mêmes* dossiers déjà enregistrés (pas de nouveau sélecteur de fichiers), après quoi les photos déjà taguées sont reconnues via leur sidecar et restaurées telles quelles — jamais ré-analysées.
- **Compatibilité navigateurs** : la File System Access API n'est pas supportée par Safari/iOS ni Firefox. Sur ces navigateurs, l'app bascule automatiquement en mode **import** : vous sélectionnez un dossier via le sélecteur système classique, les photos sont copiées dans un espace de stockage privé du navigateur (OPFS), puis tout le reste (miniatures, tags, recherche, partage) fonctionne à l'identique. Seule l'ouverture "de l'original sur un autre support" n'a alors de sens que sur l'appareil qui a fait l'import.

## Installation et lancement

```bash
npm install
npm run dev     # build + sert le dossier public/ sur http://localhost:5173
```

- **Sur PC** : ouvrez `http://localhost:5173` dans Chrome ou Edge.
- **Sur smartphone (même réseau Wi-Fi que le PC)** : remplacez `localhost` par l'adresse IP locale du PC, ex. `http://192.168.1.20:5173`, dans Chrome Android. (La File System Access API et le Service Worker exigent un contexte sécurisé ; `http://<ip-locale>` fonctionne pour le développement, mais pour un usage durable — notamment sur smartphone hors réseau local — préférez un hébergement HTTPS statique, ex. GitHub Pages/Netlify/Vercel : `npm run build` puis déployez le contenu de `public/`.)
- **Premier lancement** : cliquez sur *"Choisir le dossier de photos"*. C'est tout — l'emplacement par défaut des miniatures (`photos/miniatures` dans ce même dossier) est configuré automatiquement, et les miniatures déjà taguées s'y trouvant s'affichent aussitôt — **aucune analyse n'est lancée automatiquement**. Un bouton optionnel *"Utiliser un autre emplacement pour les miniatures"* apparaît si vous préférez un emplacement indépendant (ex. la racine du stockage interne, sur Android). Cliquez sur *"Analyser le dossier"* quand vous voulez chercher les nouvelles photos, générer leurs tags et miniatures, et réparer celles qui auraient échoué. Le bouton "🔄 Reconnecter mes dossiers" réapparaît si le navigateur n'a pas gardé la permission d'une session à l'autre.
- Vous pouvez aussi **installer l'app** (icône "Ajouter à l'écran d'accueil" / "Installer l'application") pour un lancement en un tap, comme une app native.

En développement (rebuild à chaud) :

```bash
npm run watch    # dans un terminal : reconstruit le bundle à chaque changement
npm run serve    # dans un autre terminal : sert public/
```

## Structure du projet

```
public/            fichiers statiques servis tels quels (index.html, manifest, styles, sw.js, bundle.js généré)
src/
  db.js            wrapper IndexedDB (photos, événements, handles, réglages)
  storage.js       accès au dossier (File System Access API + repli OPFS), écriture des miniatures
  exif.js          extraction des métadonnées EXIF (date, GPS, appareil)
  geocode.js       géocodage inverse (Nominatim) pour le tag "lieu"
  detect.js        détection d'objets/animaux/personnes (TensorFlow.js + COCO-SSD)
  events.js        regroupement chronologique en "événements"
  thumbnail.js     génération de miniature (canvas)
  faces.js         détection de visages + reconnaissance/propagation des prénoms (@vladmandic/face-api)
  tagging.js       orchestration du pipeline d'import/tag complet + sidecars JSON
  share.js         partage (Web Share API) avec repli en téléchargement
  main.js          contrôleur de l'interface (galerie, recherche, modale, tags manuels)
```

## Limites connues (pistes d'amélioration)

- La reconnaissance nominative n'apprend un prénom qu'à partir d'une photo où **une seule** personne est détectée (sinon on ne sait pas à quel visage le prénom s'applique) ; sur une photo de groupe, taguez chaque prénom manuellement ou d'abord sur un portrait solo de la personne.
- Le géocodage inverse utilise l'API publique Nominatim (usage raisonnable, ~1 requête/s) ; pour un volume important, prévoir une clé d'un service de géocodage dédié.
- HEIC/HEIF peut ne pas se décoder dans tous les navigateurs (dépend du support natif de `createImageBitmap`/`<img>`).
- La détection IA (objets + visages) tourne sur l'appareil : le premier scan est plus lent le temps que les modèles se téléchargent (~12 Mo au total, mis en cache ensuite par le service worker).
- Si la génération d'une miniature échoue (mémoire insuffisante sur mobile pour une photo très lourde, format non décodable, etc.), la carte affiche "⚠ Miniature indisponible" et la régénération est automatiquement retentée au scan suivant ("Analyser le dossier") — les tags déjà saisis manuellement ne sont jamais perdus dans l'intervalle. Une erreur sur une photo n'interrompt jamais l'analyse des autres.
- Certains dossiers protégés par le système (ex. le dossier de l'appareil photo sur Android) peuvent refuser l'écriture — la création du sous-dossier `photos/miniatures`, ou l'écriture d'une miniature dedans — même avec la permission accordée. Sur certains navigateurs Android, ce refus ne déclenche ni erreur ni message : l'opération reste bloquée indéfiniment. Toute opération d'écriture sur le disque est donc bornée par un délai de sécurité (20 s) qui la transforme en erreur explicite si elle ne répond pas — utilisez alors le bouton optionnel *"Utiliser un autre emplacement pour les miniatures"* pour les stocker ailleurs (ex. la racine du stockage).
- La reconnaissance faciale reste probabiliste : vérifiez et corrigez les prénoms proposés automatiquement au besoin (les tags restent modifiables manuellement à tout moment).
