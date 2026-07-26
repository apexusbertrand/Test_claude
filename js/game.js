/*
 * Quarto — moteur de jeu
 * -----------------------------------------------------------------------------
 * Chaque pièce est codée sur 4 bits, un bit par caractéristique :
 *   bit 0 (1)  : couleur   0 = claire   / 1 = foncée
 *   bit 1 (2)  : hauteur   0 = petite   / 1 = grande
 *   bit 2 (4)  : forme     0 = ronde    / 1 = carrée
 *   bit 3 (8)  : remplissage 0 = pleine / 1 = creuse
 * Il y a donc 16 pièces (0..15), toutes différentes.
 *
 * Le plateau est un tableau de 16 cases (index 0..15, ligne par ligne).
 * Une case vaut null si vide, sinon l'index de la pièce (0..15).
 */

const ATTRIBUTES = [
  { key: 'color', bit: 1, labels: ['claire', 'foncée'] },
  { key: 'height', bit: 2, labels: ['petite', 'grande'] },
  { key: 'shape', bit: 4, labels: ['ronde', 'carrée'] },
  { key: 'fill', bit: 8, labels: ['pleine', 'creuse'] },
];

// Les 10 lignes gagnantes standard : 4 lignes, 4 colonnes, 2 diagonales.
const LINES = (() => {
  const lines = [];
  for (let r = 0; r < 4; r++) lines.push([r * 4, r * 4 + 1, r * 4 + 2, r * 4 + 3]);
  for (let c = 0; c < 4; c++) lines.push([c, c + 4, c + 8, c + 12]);
  lines.push([0, 5, 10, 15]);
  lines.push([3, 6, 9, 12]);
  return lines;
})();

// Les 9 carrés 2×2 (variante optionnelle).
const SQUARES = (() => {
  const squares = [];
  for (let r = 0; r < 3; r++) {
    for (let c = 0; c < 3; c++) {
      const i = r * 4 + c;
      squares.push([i, i + 1, i + 4, i + 5]);
    }
  }
  return squares;
})();

/** Est-ce que 4 pièces partagent au moins une caractéristique ? */
function shareAttribute(a, b, c, d) {
  const andOnes = a & b & c & d;              // bit commun à 1 pour tous
  const andZeros = ~a & ~b & ~c & ~d & 0xf;   // bit commun à 0 pour tous
  return (andOnes | andZeros) !== 0;
}

/**
 * Renvoie la première combinaison gagnante trouvée sur le plateau,
 * ou null. `useSquares` active la variante des carrés 2×2.
 */
function findWinningLine(board, useSquares) {
  const groups = useSquares ? LINES.concat(SQUARES) : LINES;
  for (const line of groups) {
    const [i, j, k, l] = line;
    if (board[i] === null || board[j] === null || board[k] === null || board[l] === null) continue;
    if (shareAttribute(board[i], board[j], board[k], board[l])) return line;
  }
  return null;
}

class QuartoGame {
  /**
   * @param {object} opts
   *   mode: '2p' | 'ai'
   *   aiLevel: 'facile' | 'moyen' | 'difficile'
   *   aiPlayer: 1 | 2  (quel joueur est piloté par l'IA en mode 'ai')
   *   useSquares: boolean
   */
  constructor(opts = {}) {
    this.mode = opts.mode || '2p';
    this.aiLevel = opts.aiLevel || 'moyen';
    this.aiPlayer = opts.aiPlayer || 2;
    this.useSquares = !!opts.useSquares;
    this.reset();
  }

  reset() {
    this.board = new Array(16).fill(null);
    this.available = new Set([...Array(16).keys()]); // pièces encore disponibles
    this.currentPlayer = 1;      // le joueur qui doit agir
    this.phase = 'give';         // 'give' (choisir la pièce à donner) | 'place'
    this.pieceInHand = null;     // pièce à poser pendant la phase 'place'
    this.winner = null;          // null | 1 | 2 | 'draw'
    this.winningLine = null;
    this.lastPlaced = null;      // index de case du dernier coup (pour l'animation)
    this.history = [];
  }

  isAiTurn() {
    return this.mode === 'ai' && this.winner === null && this.currentPlayer === this.aiPlayer;
  }

  /** Le joueur courant donne une pièce à l'adversaire. Retour false si invalide. */
  givePiece(piece) {
    if (this.winner !== null || this.phase !== 'give') return false;
    if (!this.available.has(piece)) return false;
    this.pieceInHand = piece;
    this.available.delete(piece);
    this.phase = 'place';
    this.currentPlayer = this.currentPlayer === 1 ? 2 : 1; // l'adversaire va poser
    return true;
  }

  /** Le joueur courant pose la pièce en main sur `cell`. Retour false si invalide. */
  placePiece(cell) {
    if (this.winner !== null || this.phase !== 'place') return false;
    if (cell < 0 || cell > 15 || this.board[cell] !== null) return false;
    this.board[cell] = this.pieceInHand;
    this.lastPlaced = cell;
    this.pieceInHand = null;

    const line = findWinningLine(this.board, this.useSquares);
    if (line) {
      this.winner = this.currentPlayer;
      this.winningLine = line;
      return true;
    }
    if (this.available.size === 0) {
      this.winner = 'draw';
      return true;
    }
    this.phase = 'give'; // le même joueur choisit maintenant la pièce à donner
    return true;
  }
}

// Exposition (navigateur + éventuels tests Node).
if (typeof window !== 'undefined') {
  window.QuartoGame = QuartoGame;
  window.QuartoCore = { ATTRIBUTES, LINES, SQUARES, shareAttribute, findWinningLine };
}
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { QuartoGame, ATTRIBUTES, LINES, SQUARES, shareAttribute, findWinningLine };
}
