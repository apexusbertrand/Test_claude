# PhotoTag

Application web (PWA) de tri et de tag automatique de photos, utilisable sur PC et sur smartphone, sans serveur à maintenir : tout tourne dans le navigateur.

## Fonctionnalités

1. **Choisir un dossier** de photos sur votre appareil : l'app le parcourt, analyse chaque image et lui attribue automatiquement des tags (lieu, personne, animal, événement).
2. **Miniatures automatiques** : à chaque nouvelle photo détectée, une miniature est générée et enregistrée dans `photos/miniatures/<événement>/`, classée par événement et par ordre chronologique.
3. **Recherche par tag** : une barre de recherche + des filtres par catégorie (Lieu / Personne / Animal / Événement) affichent les miniatures correspondantes. Cliquer sur une photo tente d'ouvrir l'original — possible uniquement si le fichier original est accessible depuis l'appareil en cours (même disque/support), sinon un message l'indique.
4. **Page principale** : grille de miniatures avec leurs tags, ajout/suppression manuelle de tags, et bouton **Partager** (API Web Share : envoie l'original si disponible, sinon la miniature).

Catégories de tags : `lieu`, `personne`, `animal`, `evenement`, `autre`.

## Comment ça marche (choix techniques)

- **Aucun serveur applicatif.** L'app est une PWA statique. L'accès au dossier de photos se fait via la **File System Access API** du navigateur (Chrome/Edge, desktop et Android) : c'est elle qui garantit qu'on ne peut ouvrir l'original que si le fichier est réellement sur l'appareil utilisé — exactement le comportement demandé au point 3.
- **Tags automatiques, 100 % local (hors géocodage)** :
  - *Événement* : les photos sont regroupées chronologiquement (nouvel événement dès 6h sans photo, ou au-delà de 3 jours) à partir de la date EXIF.
  - *Lieu* : si la photo contient des coordonnées GPS EXIF, une requête de géocodage inverse (OpenStreetMap/Nominatim, gratuit, sans clé) donne la ville/le pays. Nécessite une connexion internet ; sans réseau, ce tag est simplement omis.
  - *Personne / Animal* : détection d'objets locale, dans le navigateur, via TensorFlow.js (modèle COCO-SSD léger). Détecte les personnes et les animaux courants (chien, chat, oiseau, cheval, etc.). C'est une détection de présence, pas une reconnaissance faciale nominative — pour associer un nom à une personne, ajoutez-le manuellement (le tag est ensuite mémorisé sur la photo).
- **Stockage des métadonnées** (tags, index photo, handles de fichiers) dans IndexedDB, en local sur l'appareil.
- **Compatibilité navigateurs** : la File System Access API n'est pas supportée par Safari/iOS ni Firefox. Sur ces navigateurs, l'app bascule automatiquement en mode **import** : vous sélectionnez un dossier via le sélecteur système classique, les photos sont copiées dans un espace de stockage privé du navigateur (OPFS), puis tout le reste (miniatures, tags, recherche, partage) fonctionne à l'identique. Seule l'ouverture "de l'original sur un autre support" n'a alors de sens que sur l'appareil qui a fait l'import.

## Installation et lancement

```bash
npm install
npm run dev     # build + sert le dossier public/ sur http://localhost:5173
```

- **Sur PC** : ouvrez `http://localhost:5173` dans Chrome ou Edge.
- **Sur smartphone (même réseau Wi-Fi que le PC)** : remplacez `localhost` par l'adresse IP locale du PC, ex. `http://192.168.1.20:5173`, dans Chrome Android. (La File System Access API et le Service Worker exigent un contexte sécurisé ; `http://<ip-locale>` fonctionne pour le développement, mais pour un usage durable — notamment sur smartphone hors réseau local — préférez un hébergement HTTPS statique, ex. GitHub Pages/Netlify/Vercel : `npm run build` puis déployez le contenu de `public/`.)
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
  tagging.js       orchestration du pipeline d'import/tag complet
  share.js         partage (Web Share API) avec repli en téléchargement
  main.js          contrôleur de l'interface (galerie, recherche, modale, tags manuels)
```

## Limites connues (pistes d'amélioration)

- Pas de reconnaissance faciale nominative automatique (RGPD-friendly par défaut) : les tags "Personne" sont manuels au-delà du comptage de présence.
- Le géocodage inverse utilise l'API publique Nominatim (usage raisonnable, ~1 requête/s) ; pour un volume important, prévoir une clé d'un service de géocodage dédié.
- HEIC/HEIF peut ne pas se décoder dans tous les navigateurs (dépend du support natif de `createImageBitmap`/`<img>`).
- La détection IA tourne sur l'appareil : le premier scan est plus lent le temps que le modèle se télécharge (~6 Mo, mis en cache ensuite par le service worker).
