# Prompt : application Android « Analyseur & Nettoyeur de stockage »

> Copiez tout ce qui suit la ligne `---` dans votre assistant IA de développement (Claude Code, Android Studio Gemini, Cursor…).
> Le prompt est autoportant : il décrit le besoin, les contraintes Android réelles, l'architecture, les écrans, les règles de sécurité et les livrables attendus.

---

## 1. Rôle et objectif

Tu es un développeur Android senior expert en Kotlin, Jetpack Compose et en gestion du stockage Android (Scoped Storage, MediaStore, SAF, StorageStatsManager).

Crée une application Android native complète, prête à compiler, nommée **« StorageLens »** (nom modifiable), qui :

1. **Identifie automatiquement tout l'espace de stockage** du téléphone (stockage interne + carte SD + clés USB OTG si présentes) : capacité totale, espace utilisé, espace libre, et répartition par catégorie.
2. **Analyse tous les répertoires et fichiers** accessibles et les **classe du plus volumineux au plus petit**, avec navigation dans l'arborescence.
3. **Laisse l'utilisateur sélectionner** précisément ce qu'il veut supprimer (fichiers, dossiers, catégories), avec confirmation et possibilité d'annulation.
4. **Libère l'espace « caché »** : fichiers de logs, traces, caches, fichiers temporaires, miniatures, résidus d'applications désinstallées, dossiers vides, APK oubliés, corbeille, etc. — tout ce qui fait paraître le téléphone plein sans raison visible.

L'application doit être **honnête** : elle affiche clairement ce qu'elle peut nettoyer elle-même, ce qui nécessite une action de l'utilisateur dans les Paramètres, et ce qu'Android interdit d'atteindre sans root.

## 2. Stack technique imposée

- **Langage** : Kotlin 2.x, 100 % Kotlin, aucun Java.
- **UI** : Jetpack Compose + Material 3 (Material You, couleurs dynamiques, thème clair/sombre).
- **Architecture** : MVVM + Clean Architecture (couches `ui` / `domain` / `data`), un seul module `app` suffit mais avec packages bien séparés.
- **Injection de dépendances** : Hilt.
- **Asynchrone** : Coroutines + Flow (`StateFlow` pour l'état UI).
- **Persistance** : Room (historique des scans, corbeille interne, règles d'exclusion), DataStore Preferences (réglages).
- **Tâches de fond** : WorkManager (scan planifié optionnel, purge de la corbeille interne).
- **Navigation** : Navigation Compose avec routes typées.
- **Build** : Gradle Kotlin DSL + Version Catalog (`libs.versions.toml`).
- **minSdk 26**, **targetSdk / compileSdk 35** (ou la dernière version stable).
- **Langue** : interface en **français** par défaut (`values/strings.xml`), avec `values-en/strings.xml` en anglais. Aucune chaîne en dur dans le code.

## 3. Contraintes Android à respecter impérativement

Gère correctement chaque version d'Android :

| Sujet | Comportement attendu |
|---|---|
| **Accès complet aux fichiers** | Sur Android 11+ (API 30+), demander `MANAGE_EXTERNAL_STORAGE` via l'intent `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`, avec un écran d'explication préalable. Sur Android 8–10, utiliser `READ/WRITE_EXTERNAL_STORAGE` (+ `requestLegacyExternalStorage="true"` pour Android 10). |
| **Taille des applications et de leurs caches** | Utiliser `StorageStatsManager.queryStatsForPackage()` (taille APK, données, cache par app). Nécessite l'accès « Données d'utilisation » (`PACKAGE_USAGE_STATS`, via `Settings.ACTION_USAGE_ACCESS_SETTINGS`). |
| **Vider le cache des autres applications** | Impossible directement depuis Android 6+. Implémenter : (a) sur Android 11+, l'intent `StorageManager.ACTION_CLEAR_APP_CACHE` (vide le cache de toutes les apps après confirmation système, nécessite l'accès complet aux fichiers) ; (b) un **mode assisté** qui ouvre `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` app par app, triées par taille de cache décroissante, avec retour automatique dans l'app et recalcul du gain. |
| **Dossiers `Android/data` et `Android/obb`** | Inaccessibles sur Android 11+ (même via SAF sur Android 13+). Les mesurer via `StorageStatsManager` quand c'est possible et l'expliquer à l'utilisateur. Sur Android ≤ 10, les scanner normalement. |
| **Fichiers médias** | Pour supprimer des photos/vidéos/audios indexés, utiliser `MediaStore.createDeleteRequest()` (Android 11+) ou `createTrashRequest()` pour la corbeille système, afin que la galerie reste cohérente. Toujours appeler `MediaScannerConnection.scanFile()` après une suppression par chemin. |
| **Espace « Système »** | Calculer `Système & autres = total − (somme de toutes les catégories mesurées) − libre` et l'afficher explicitement, avec une explication (OS, partitions, zones non accessibles). |
| **Logs système, tombstones, ANR, dropbox** (`/data/log`, `/data/tombstones`, `/data/anr`, `/data/system/dropbox`) | Non accessibles sans root. Afficher la catégorie comme « Non accessible » avec explication. Prévoir une **interface optionnelle** `PrivilegedCleaner` avec deux implémentations facultatives : **Shizuku** (si installé et autorisé) et **root** (`su`), désactivées par défaut et clairement marquées « avancé ». |
| **Google Play** | Documenter dans le README que `MANAGE_EXTERNAL_STORAGE` exige une justification Play Console (catégorie « gestionnaire de fichiers / nettoyage »). |

## 4. Fonctionnalités détaillées

### 4.1 Tableau de bord (écran d'accueil)

- Jauge circulaire animée : utilisé / libre / total, pour chaque volume (`StorageManager.getStorageVolumes()`, `StatFs`).
- Barre segmentée par catégorie : Applications, Images, Vidéos, Audio, Documents, Téléchargements, Archives, APK, Caches & fichiers temporaires, Logs & traces, Autres fichiers, Système.
- Carte « **Espace récupérable** » : total estimé en un coup d'œil, avec bouton « Nettoyer ».
- Bouton « Lancer l'analyse complète » + date du dernier scan.

### 4.2 Moteur de scan

- Parcours **itératif** (pile, pas de récursion) de toute l'arborescence accessible, sur `Dispatchers.IO`, parallélisé par sous-dossier racine (`Semaphore` pour limiter à ~4 coroutines).
- Ne jamais suivre les liens symboliques (`Files.isSymbolicLink` / `Os.lstat`) pour éviter boucles et doubles comptages.
- Construire un arbre `FileNode(path, name, isDir, size, fileCount, lastModified, children)` où la taille d'un dossier = somme récursive.
- Progression en temps réel via `Flow<ScanProgress>` : dossier courant, nombre de fichiers, octets comptés, pourcentage estimé (basé sur l'espace utilisé connu).
- Annulable à tout moment, reprise non nécessaire.
- Performance cible : scanner 100 000 fichiers en moins de 30 s sur un appareil milieu de gamme ; mémoire maîtrisée (ne garder en détail que les N plus gros fichiers par dossier, agréger le reste).
- Mettre en cache le résultat du dernier scan dans Room pour un affichage instantané au prochain lancement.

### 4.3 Explorateur « Répertoires volumineux »

- Liste des dossiers triés par **taille décroissante**, avec pour chacun : nom, chemin, taille, % du total, nombre de fichiers, barre de proportion.
- Navigation dans les sous-dossiers (fil d'Ariane cliquable, retour arrière).
- Tri alternatif : taille, nombre de fichiers, date de modification, nom.
- Vue alternative **Treemap** (rectangles proportionnels, algorithme squarified) dessinée en `Canvas` Compose, cliquable pour zoomer.
- Onglet « **Top 100 des plus gros fichiers** » tous dossiers confondus, filtrable par type et par ancienneté (> 30 j, > 90 j, > 1 an).

### 4.4 Détection de l'espace caché et des fichiers inutiles

Crée un moteur de règles extensible (`CleanupRule` : id, libellé, description, niveau de risque, `suspend fun find(): List<Candidate>`). Règles à implémenter :

| Catégorie | Détection | Risque | Coché par défaut |
|---|---|---|---|
| Fichiers de logs | Extensions `.log`, `.log.1`…, `.logcat`, dossiers `log/`, `logs/`, `Log/` | Faible | Oui |
| Fichiers de traces | `.trace`, `.trc`, `.hprof`, `.dmp`, `.dump`, dossiers `traces/`, `crash/`, `crashes/`, `tombstones/` accessibles | Faible | Oui |
| Fichiers temporaires | `.tmp`, `.temp`, `.bak`, `.old`, `~*`, `.partial`, `.crdownload`, `.download` inachevés | Faible | Oui |
| Caches accessibles | Dossiers `cache/`, `.cache/`, `Cache/` hors `Android/data` ; cache propre à l'app | Faible | Oui |
| Miniatures | `DCIM/.thumbnails`, `Pictures/.thumbnails` (régénérées automatiquement) | Faible | Oui |
| Résidus d'apps désinstallées | Dossiers portant un nom de package (`com.xxx.yyy`) dont l'app n'est plus installée (`PackageManager`) | Moyen | Non |
| Dossiers vides | Dossiers sans aucun fichier (récursivement) | Faible | Oui |
| Fichiers APK | `.apk`, `.xapk`, `.apks` dans Téléchargements et ailleurs, en indiquant si l'app est déjà installée | Faible | Oui si déjà installée |
| Corbeilles | Corbeille MediaStore (`IS_TRASHED`), dossiers `.Trash`, `.trashed-*`, `.recycle` | Moyen | Non |
| Médias de messageries | WhatsApp / Telegram / Signal : dossiers `Media/*` , `.Statuses`, `Sent`, par sous-type (images, vidéos, notes vocales) | Élevé | Non |
| Doublons | Même taille → hash partiel (premiers + derniers 64 Ko) → hash SHA-256 complet ; proposer de garder l'original le plus ancien | Moyen | Non |
| Gros fichiers anciens | > 100 Mo et non modifiés depuis > 6 mois (seuils réglables) | Élevé | Non |
| Caches des applications | Via `StorageStatsManager` + nettoyage système/assisté (cf. §3) | Faible | Oui |

Chaque catégorie affiche : taille totale, nombre d'éléments, niveau de risque (badge couleur), explication en langage simple, et liste détaillée dépliable.

### 4.5 Sélection et suppression

- Cases à cocher à trois niveaux : catégorie → dossier → fichier (état indéterminé géré).
- Actions : « Tout sélectionner », « Sélection recommandée (risque faible) », « Tout désélectionner ».
- Barre inférieure persistante : « X éléments sélectionnés — Y Go à libérer » + bouton « Supprimer ».
- Aperçu avant suppression : miniatures pour images/vidéos (Coil), ouverture du fichier avec l'app par défaut (`FileProvider` + `ACTION_VIEW`).
- **Boîte de dialogue de confirmation** récapitulant le nombre d'éléments, la taille et les éléments à risque moyen/élevé listés à part.
- **Corbeille interne** (option activée par défaut) : déplacement dans un dossier privé de l'app avec métadonnées en Room ; restauration possible pendant 7 jours (durée réglable), purge automatique par WorkManager. Option « Suppression définitive immédiate » pour les logs/caches/temporaires (qui ne passent pas par la corbeille).
- Suppression exécutée en arrière-plan avec progression, résistante aux erreurs (un échec n'arrête pas le lot) ; rapport final : espace réellement libéré (mesuré via `StatFs` avant/après), éléments en échec avec la raison.
- Snackbar « Annuler » juste après la suppression quand la corbeille interne est utilisée.

### 4.6 Sécurité — règles non négociables

- **Liste de chemins protégés** jamais proposés à la suppression : racine du stockage, `DCIM/Camera` (sauf sélection manuelle explicite), `Android/`, `Documents/` entiers, dossiers système, fichiers en cours d'écriture, dossier de l'app elle-même (sauf son cache).
- Aucune suppression sans action explicite de l'utilisateur. Aucune suppression automatique en arrière-plan, même planifiée (le scan planifié ne fait que **notifier** l'espace récupérable).
- Vérifier juste avant la suppression que le fichier existe toujours et n'a pas changé (taille + date) depuis le scan.
- Liste d'exclusions personnalisée par l'utilisateur (dossiers/extensions à ignorer).
- Aucune collecte de données, aucun accès réseau (pas de permission `INTERNET`), aucune publicité.

### 4.7 Écran Applications

- Liste des applications installées triées par taille totale (APK + données + cache), avec icône, nom, version, date de dernière utilisation (`UsageStatsManager`).
- Filtre « Non utilisées depuis 30/90 jours ».
- Actions : vider le cache (mode assisté), ouvrir la fiche système, désinstaller (`ACTION_DELETE` / `Intent.ACTION_UNINSTALL_PACKAGE`).

### 4.8 Réglages

- Seuils « gros fichiers » et « fichiers anciens ».
- Corbeille interne : activer/désactiver, durée de rétention.
- Scan planifié : désactivé / hebdomadaire / mensuel, avec notification si espace récupérable > seuil.
- Afficher les fichiers cachés (dossiers commençant par `.`).
- Mode avancé : Shizuku / root (désactivé par défaut, avec avertissement).
- Thème : système / clair / sombre.

## 5. Écrans et navigation

1. **Onboarding** (première ouverture) : 3 pages expliquant l'app, puis écran de permissions avec explication de *pourquoi* chaque accès est nécessaire et ce qui ne marchera pas sans.
2. **Tableau de bord**.
3. **Analyse en cours** (progression animée, annulable).
4. **Répertoires** (liste + treemap + top fichiers).
5. **Nettoyage** (catégories de fichiers inutiles et espace caché).
6. **Applications**.
7. **Corbeille interne**.
8. **Historique** (scans et nettoyages passés, espace libéré cumulé).
9. **Réglages** + **À propos** (explication des limites Android).

Barre de navigation inférieure : Accueil · Répertoires · Nettoyage · Applications. Le reste via menu.

## 6. Qualité et UX

- Tailles formatées avec `Formatter.formatShortFileSize` (unités localisées).
- États vides, états de chargement (skeletons) et états d'erreur soignés sur chaque écran.
- Accessibilité : `contentDescription` partout, contraste AA, tailles de police dynamiques, navigation TalkBack.
- Listes performantes (`LazyColumn` avec `key`), pas de blocage du thread principal (StrictMode actif en debug).
- Gestion de la rotation et de la mort de processus (`SavedStateHandle`).
- Gestion des cas limites : carte SD retirée pendant le scan, fichier supprimé entre scan et suppression, permission révoquée, espace disque plein, noms de fichiers avec caractères spéciaux, chemins très longs.

## 7. Tests

- **Tests unitaires** (JUnit 5 + MockK + Turbine) : moteur de scan sur un système de fichiers temporaire, agrégation des tailles, chaque `CleanupRule`, détection de doublons, liste des chemins protégés, calcul de l'espace « Système ».
- **Tests d'UI** (Compose UI Test) : sélection à trois niveaux, confirmation de suppression, restauration depuis la corbeille.
- Couverture visée : ≥ 80 % sur la couche `domain`.

## 8. Livrables attendus

1. Le projet Android Studio complet et compilable (`./gradlew assembleDebug` doit passer), avec l'arborescence de tous les fichiers.
2. `AndroidManifest.xml` complet avec toutes les permissions, justifiées en commentaire.
3. Le code source de **tous** les fichiers (pas de `// TODO` ni de pseudo-code), en commençant par : `libs.versions.toml`, `build.gradle.kts`, manifeste, puis couche `domain`, `data`, `ui`.
4. Les tests.
5. Un `README.md` en français : fonctionnalités, captures prévues, instructions de build, explication des permissions, **tableau clair de ce que l'app peut / ne peut pas nettoyer selon la version d'Android**, notes pour la publication Google Play.

Procède par étapes : présente d'abord l'arborescence du projet et l'architecture, puis génère les fichiers un par un. Si une fonctionnalité est techniquement impossible sur une version d'Android, dis-le explicitement et propose la meilleure alternative plutôt que de l'omettre silencieusement.
