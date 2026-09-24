# Quarto 🎲

Application web **mobile-first** (PWA installable, jouable hors-ligne) du jeu de
stratégie **Quarto**. Jouable à **2 joueurs** en local ou **contre une IA**
(3 niveaux).

## Règles du jeu

- Plateau **4×4**, **16 pièces** toutes différentes par 4 caractéristiques
  binaires : **couleur** (claire/foncée), **hauteur** (petite/grande),
  **forme** (ronde/carrée), **remplissage** (pleine/creuse).
- But : aligner **4 pièces partageant au moins une caractéristique commune**
  sur une **ligne**, une **colonne** ou une **diagonale**.
- Sel du jeu : **c'est l'adversaire qui choisit la pièce que vous devez poser.**
  À votre tour, vous posez la pièce reçue puis choisissez celle que l'adversaire
  devra poser.
- Variante optionnelle : un **carré 2×2** de pièces partageant une
  caractéristique gagne également.

## Fonctionnalités

- 🎮 Modes **2 joueurs** (local) et **1 joueur contre l'IA**
- 🤖 IA à 3 niveaux : *facile*, *moyen*, *difficile* (recherche minimax
  alpha-bêta en fin de partie)
- 💡 Bouton **Indice** (coup gagnant / pièces sûres à donner)
- 📱 Design moderne, sombre, pensé pour le mobile ; **installable** (PWA) et
  utilisable **hors-ligne**
- 🎨 Variante carrés 2×2 activable

## Lancer en local

Aucune dépendance ni build. Il suffit de servir le dossier :

```bash
# avec Python
python3 -m http.server 8000
# puis ouvrir http://localhost:8000
```

Sur mobile : ouvrir l'URL dans le navigateur puis **« Ajouter à l'écran
d'accueil »** pour l'installer comme une application.

## Structure

```
index.html              Structure de l'interface
css/styles.css          Thème et mise en page (mobile-first)
js/game.js              Moteur : pièces, plateau, règles, détection de victoire
js/ai.js                IA : heuristiques + minimax alpha-bêta
js/pieces.js            Rendu SVG des pièces
js/app.js               Contrôleur d'interface
manifest.webmanifest    Manifeste PWA
sw.js                   Service worker (cache hors-ligne)
icons/                  Icônes de l'application
```

## Suite envisagée

Annulation de coup, statistiques, thème clair, mode en ligne, packaging natif
(Capacitor) pour publication sur les stores.
