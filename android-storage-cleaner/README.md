# Apexus Cleaner — Analyseur et nettoyeur de stockage Android

Application Android native (Kotlin, Jetpack Compose, Material 3) qui :

1. **mesure tout le stockage** du téléphone (mémoire interne, carte SD, clé USB OTG) et le répartit par catégorie ;
2. **classe les répertoires et fichiers du plus volumineux au plus petit** (liste, carte « treemap » et top 100 des fichiers) ;
3. **laisse l'utilisateur choisir** ce qu'il supprime (sélection à trois niveaux, aperçu, confirmation, corbeille restaurable) ;
4. **libère l'espace caché** : logs, traces, fichiers temporaires, caches, miniatures, résidus d'applications désinstallées, dossiers vides, APK oubliés, corbeilles, doublons, médias de messageries, gros fichiers anciens.

Le cahier des charges d'origine se trouve dans [`../prompts/android-storage-cleaner-prompt.md`](../prompts/android-storage-cleaner-prompt.md).

---

## Fonctionnalités

| Écran | Contenu |
|---|---|
| **Onboarding** | 3 pages de présentation puis un écran d'autorisations qui explique pourquoi chaque accès est demandé et ce qui ne fonctionne pas sans lui. |
| **Accueil** | Jauge animée (utilisé / libre / total) par volume, barre segmentée par catégorie (dont « Système et autres » calculé), carte « Espace récupérable », lancement de l'analyse. |
| **Analyse** | Progression en temps réel (dossier courant, nombre de fichiers, octets), annulable. L'analyse continue si l'on quitte l'écran. |
| **Répertoires** | Dossiers triés par taille / nombre de fichiers / date / nom, fil d'Ariane, barre de proportion, vue **treemap** (algorithme *squarified*), **top 100** filtrable par type et ancienneté, sélection manuelle et suppression. |
| **Nettoyage** | Catégories de fichiers inutiles avec badge de risque, sélection **catégorie → dossier → fichier** (cases à trois états), « Sélection recommandée », caches des applications (nettoyage système Android 11+ ou mode assisté), journaux système (mode root facultatif). |
| **Applications** | Taille APK / données / cache de chaque application, dernière utilisation, filtre « inutilisées depuis 30/90 jours », vider le cache (assisté), infos, désinstaller. |
| **Corbeille** | Restauration ou suppression définitive, purge automatique après la durée choisie (7 jours par défaut). |
| **Historique** | Analyses et nettoyages passés, espace libéré cumulé. |
| **Réglages** | Seuils « gros fichier » / « fichier ancien », corbeille, analyse planifiée (notification uniquement), fichiers cachés, exclusions, thème, mode root. |
| **À propos** | Tableau de ce qu'Android permet ou non de nettoyer selon la version. |

### Règles de détection

| Catégorie | Détection | Risque | Cochée par défaut |
|---|---|---|---|
| Logs | `.log`, `.log.N`, `.logcat`, fichiers texte dans `log/`, `logs/` | Faible | Oui |
| Traces | `.trace`, `.trc`, `.hprof`, `.dmp`, `.dump`, `bugreport-*`, dossiers `traces/`, `crash(es)/`, `tombstones/`, `anr/` | Faible | Oui |
| Temporaires | `.tmp`, `.temp`, `.partial`, `.crdownload`, `~*`, `.pending-*` (`.bak`/`.old` détectés mais non recommandés) ; ignorés s'ils ont été modifiés il y a moins d'une heure | Faible | Oui |
| Caches accessibles | Dossiers `cache`, `.cache`, `tmp`… du stockage partagé | Faible | Oui |
| Miniatures | Dossiers `.thumbnails` | Faible | Oui |
| Dossiers vides | Aucun fichier, récursivement (dossiers standards exclus) | Faible | Oui |
| APK | `.apk`, `.xapk`, `.apks`, `.apkm` — recommandés seulement si l'application est déjà installée | Faible | Oui si installée |
| Résidus d'apps | Dossiers nommés comme un paquet (`com.xxx.yyy`) non installé, à la racine ou dans `Android/data|obb|media` | Moyen | Non |
| Corbeilles | `.trashed-*` (corbeille MediaStore), `.Trash*`, `.recycle`… | Moyen | Non |
| Doublons | Même taille → empreinte partielle (début + fin) → SHA-256 complet ; l'original le plus ancien est conservé | Moyen | Non |
| Médias de messageries | Sous-dossiers de `WhatsApp/Media`, `Telegram`, `Signal`… | Élevé | Non |
| Gros fichiers anciens | ≥ 100 Mo et non modifiés depuis ≥ 6 mois (réglables) | Élevé | Non |

Un élément dont un dossier parent est déjà proposé n'est jamais compté deux fois.

### Sécurité

- **Chemins protégés** (`ProtectedPaths`) : racines des volumes, dossiers standards (`DCIM`, `Download`, `Documents`…), `DCIM/Camera`, `Android/`, `Android/data|obb|media` et les dossiers des applications **installées**, le dossier de l'application elle-même (sauf son cache), tout ce qui est hors des volumes de stockage.
- Aucune règle automatique ne pioche dans `DCIM/Camera` (sélection manuelle uniquement).
- Revérification juste avant chaque suppression : garde-fou, existence, taille et date inchangées depuis l'analyse.
- **Photos et vidéos : double confirmation.** Après la confirmation de l'application, Android 11+ affiche sa propre fenêtre (`MediaStore.createTrashRequest` ou `createDeleteRequest`). Avec la corbeille activée, les médias vont dans la **corbeille Android** (restaurables 30 jours depuis Galerie ou Fichiers) ; sinon ils sont supprimés définitivement. Si l'utilisateur refuse, ils sont conservés et le reste du lot est traité. Sur Android 8–10, où cette fenêtre n'existe pas, le dialogue de l'application indique le nombre de photos et vidéos concernées. **Les dossiers sont concernés aussi** : si un dossier sélectionné (ex. « WhatsApp Video ») contient des photos ou vidéos indexées, elles passent par la même fenêtre d'Android ; le reste de son contenu suit le traitement habituel (corbeille interne ou suppression définitive). Les fenêtres sont découpées par lots de 1 000 médias ; un dossier n'est traité que si toutes les fenêtres qui le concernent ont été acceptées.
- Aucune suppression sans action explicite ; l'analyse planifiée ne fait que notifier.
- **Corbeille interne** activée par défaut ; les logs, caches, temporaires, miniatures et dossiers vides sont toujours supprimés définitivement.
- **Aucun accès réseau** : la permission `INTERNET` est retirée du manifeste fusionné.

---

## Ce que l'application peut et ne peut pas nettoyer

| | Android 8 – 10 | Android 11 + |
|---|---|---|
| Fichiers du stockage partagé | ✅ | ✅ (avec « Accès à tous les fichiers ») |
| Logs et traces écrits par les apps dans le stockage partagé | ✅ | ✅ |
| `Android/data` et `Android/obb` | ✅ | ❌ inaccessibles (mesurés via `StorageStatsManager` quand l'accès aux données d'utilisation est accordé) |
| Cache des autres applications | 🟠 mode assisté (fiche de l'app dans les Paramètres) | 🟠 `StorageManager.ACTION_CLEAR_APP_CACHE` (confirmation système) ou mode assisté |
| Journaux système `/data/log`, `/data/tombstones`, `/data/anr`, `/data/system/dropbox` | ❌ sans root | ❌ sans root |
| Taille de chaque application | ✅ avec l'accès aux données d'utilisation | ✅ avec l'accès aux données d'utilisation |

**« Système et autres »** = capacité totale − espace libre − tout ce qui a été mesuré (fichiers + applications). Sur le volume principal, la capacité provient de `StorageStatsManager.getTotalBytes()`, qui inclut la partition système (valeur « commerciale » du téléphone).

Le **mode root** (réglages → Avancé) utilise `su` pour mesurer et vider les journaux système. Il est désactivé par défaut. Shizuku n'est pas intégré : l'interface `PrivilegedCleaner` permet d'ajouter cette implémentation.

---

## Architecture

```
app/src/main/java/com/apexus/storagelens/
├── domain/                  # Kotlin pur, sans dépendance Android (testé sur la JVM)
│   ├── model/               # FileNode, ScanResult, CleanupCandidate, catégories…
│   ├── scan/                # FileScanner (parcours itératif parallèle), collecteurs, TreeOps
│   ├── cleanup/             # Règles, CleanupAnalyzer, ProtectedPaths, DuplicateFinder
│   ├── selection/           # Sélection à trois niveaux, calcul de l'espace « Système »
│   └── treemap/             # Algorithme squarified
├── data/
│   ├── storage/             # Volumes (StorageManager, StatFs, StorageStatsManager), permissions
│   ├── apps/                # Tailles des applications, dernière utilisation
│   ├── scan/                # ScanManager : orchestration, progression, historique
│   ├── delete/              # DeletionManager, corbeille interne, synchronisation MediaStore
│   ├── privileged/          # Nettoyage root des journaux système
│   ├── db/                  # Room : corbeille, historique
│   └── prefs/               # DataStore : réglages
├── work/                    # WorkManager : analyse planifiée, purge de la corbeille
├── di/                      # Modules Hilt
└── ui/                      # Compose : thème, composants, navigation typée, écrans + ViewModels
```

- **MVVM** : chaque écran a son `ViewModel` Hilt exposant un `StateFlow`.
- L'analyse et les suppressions tournent dans une portée applicative (`@ApplicationScope`) : elles survivent à la navigation.
- Le moteur de scan ne conserve individuellement que les 200 plus gros fichiers par dossier (le reste est agrégé) pour maîtriser la mémoire, et tient à jour un top 100 global.
- Après une suppression, l'arbre est mis à jour sans relancer l'analyse (`TreeOps.remove`).

## Construire le projet

Prérequis : Android Studio (Ladybug ou plus récent) ou le SDK Android avec la plateforme 35, JDK 17+.

```bash
cd android-storage-cleaner
./gradlew assembleDebug          # APK : app/build/outputs/apk/debug/apexus-cleaner-debug.apk
./gradlew testDebugUnitTest      # tests unitaires (JUnit 5)
./gradlew connectedAndroidTest   # tests d'interface Compose (appareil ou émulateur)
```

**Sans installer Android Studio** : le workflow GitHub Actions `.github/workflows/android-apk.yml` exécute les tests et construit l'APK à chaque push touchant `android-storage-cleaner/` (ou manuellement via *Actions → Apexus Cleaner — APK Android → Run workflow*). L'APK est publié à chaque build dans la release **apexus-cleaner-latest**, avec un lien fixe : https://github.com/apexusbertrand/Test_claude/releases/download/apexus-cleaner-latest/apexus-cleaner.apk. Pour l'installer sur le téléphone, autorisez l'installation depuis des sources inconnues.

- `minSdk` 26 (Android 8.0), `targetSdk`/`compileSdk` 35.
- Langues : français (par défaut) et anglais.

## Tests

- `app/src/test` : moteur de scan (agrégation, liens symboliques, exclusions, annulation), chaque règle de nettoyage, déduplication, doublons, chemins protégés, sélection à trois états, espace « Système », mise à jour de l'arbre, treemap.
- `app/src/androidTest` : dialogue de confirmation (éléments à risque, confirmation explicite) et case à trois états d'une catégorie.

## Publication sur Google Play

- `MANAGE_EXTERNAL_STORAGE` est une permission restreinte : remplissez la déclaration « Accès à tous les fichiers » dans la Play Console en choisissant le cas d'usage **gestionnaire de fichiers / nettoyage de stockage**, avec une vidéo montrant la fonctionnalité.
- `QUERY_ALL_PACKAGES` doit aussi être déclarée (détection des résidus d'applications désinstallées, écran Applications).
- `PACKAGE_USAGE_STATS` est accordée par l'utilisateur dans les Paramètres ; expliquez-la dans la fiche et la politique de confidentialité.
- L'application n'a pas d'accès réseau et ne collecte aucune donnée : déclarez-le dans la section « Sécurité des données ».
