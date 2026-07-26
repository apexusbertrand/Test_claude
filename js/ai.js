/*
 * Quarto — intelligence artificielle
 * -----------------------------------------------------------------------------
 * Une décision de l'IA se fait en deux temps, comme pour un humain :
 *   1. PLACER la pièce reçue (phase 'place')
 *   2. DONNER une pièce à l'adversaire (phase 'give')
 *
 * Trois niveaux :
 *   - facile     : joue un coup gagnant s'il existe, sinon assez aléatoire
 *   - moyen      : gagne si possible, ne donne jamais une pièce gagnante si évitable
 *   - difficile  : recherche minimax (alpha-bêta) en fin de partie, heuristique avant
 */

(function () {
  const { findWinningLine } = window.QuartoCore;

  const emptyCells = (board) => {
    const cells = [];
    for (let i = 0; i < 16; i++) if (board[i] === null) cells.push(i);
    return cells;
  };

  /** Existe-t-il une case où poser `piece` donne la victoire ? -> index ou -1 */
  function winningCellFor(board, piece, useSquares) {
    for (const cell of emptyCells(board)) {
      board[cell] = piece;
      const win = findWinningLine(board, useSquares) !== null;
      board[cell] = null;
      if (win) return cell;
    }
    return -1;
  }

  /** Une pièce est « sûre » à donner si l'adversaire ne peut pas gagner avec. */
  function safePiecesToGive(board, available, useSquares) {
    const safe = [];
    for (const piece of available) {
      if (winningCellFor(board, piece, useSquares) === -1) safe.push(piece);
    }
    return safe;
  }

  const randOf = (arr) => arr[Math.floor(Math.random() * arr.length)];

  // --- Placement -------------------------------------------------------------

  function choosePlacement(game) {
    const { board, pieceInHand, useSquares, aiLevel } = game;

    // Toujours prendre une victoire immédiate.
    const winCell = winningCellFor(board, pieceInHand, useSquares);
    if (winCell !== -1) return winCell;

    const cells = emptyCells(board);
    if (aiLevel === 'facile') return randOf(cells);

    if (aiLevel === 'difficile' && cells.length <= 6) {
      const best = searchBestMove(game);
      if (best && best.cell !== undefined && best.cell !== null) return best.cell;
    }

    // Heuristique (moyen, ou difficile en début de partie) :
    // préférer une case qui laisse ensuite le plus de pièces « sûres » à donner.
    let bestCells = [];
    let bestScore = -Infinity;
    for (const cell of cells) {
      board[cell] = pieceInHand;
      const remaining = [...game.available];
      const safe = safePiecesToGive(board, remaining, useSquares).length;
      board[cell] = null;
      const score = safe; // plus il reste d'options sûres, mieux c'est
      if (score > bestScore) { bestScore = score; bestCells = [cell]; }
      else if (score === bestScore) bestCells.push(cell);
    }
    return randOf(bestCells);
  }

  // --- Don d'une pièce -------------------------------------------------------

  function chooseGift(game) {
    const { board, useSquares, aiLevel } = game;
    const available = [...game.available];

    if (aiLevel === 'facile') return randOf(available);

    const safe = safePiecesToGive(board, available, useSquares);

    if (aiLevel === 'difficile' && emptyCells(board).length <= 6) {
      const best = searchBestMove(game);
      if (best && best.gift !== undefined && best.gift !== null) return best.gift;
    }

    if (safe.length > 0) return randOf(safe);
    return randOf(available); // aucune pièce sûre : on est probablement perdant
  }

  // --- Recherche minimax (négamax + alpha-bêta) ------------------------------
  // État évalué du point de vue du joueur qui doit AGIR.
  // Score : +1 si le joueur courant finit par gagner, -1 s'il perd, 0 nul.

  function cloneState(game) {
    return {
      board: game.board.slice(),
      available: new Set(game.available),
      phase: game.phase,
      pieceInHand: game.pieceInHand,
      current: game.currentPlayer,
      useSquares: game.useSquares,
    };
  }

  function negamax(state, alpha, beta) {
    // La phase 'place' : le joueur courant pose la pièce en main.
    if (state.phase === 'place') {
      let best = -Infinity;
      const cells = emptyCells(state.board);
      // Ordonner : essayer d'abord une victoire immédiate.
      for (const cell of cells) {
        state.board[cell] = state.pieceInHand;
        if (findWinningLine(state.board, state.useSquares)) {
          state.board[cell] = null;
          return { score: 1 }; // victoire immédiate pour le joueur courant
        }
        state.board[cell] = null;
      }
      if (state.available.size === 0) return { score: 0 }; // plateau plein -> nul

      let bestMove = null;
      for (const cell of cells) {
        state.board[cell] = state.pieceInHand;
        const saved = state.pieceInHand;
        state.pieceInHand = null;
        state.phase = 'give';
        const res = negamax(state, alpha, beta);
        const score = res.score; // même joueur agit encore -> pas de négation
        state.phase = 'place';
        state.pieceInHand = saved;
        state.board[cell] = null;
        if (score > best) { best = score; bestMove = { cell }; }
        alpha = Math.max(alpha, best);
        if (alpha >= beta) break;
      }
      return { score: best, ...bestMove };
    }

    // La phase 'give' : le joueur courant donne une pièce, l'adversaire agit.
    // On itère sur une COPIE : muter le Set pendant un for...of le ferait
    // re-visiter indéfiniment les mêmes pièces.
    let best = -Infinity;
    let bestMove = null;
    const pieces = [...state.available];
    for (const piece of pieces) {
      state.available.delete(piece);
      state.pieceInHand = piece;
      state.phase = 'place';
      const savedCurrent = state.current;
      state.current = state.current === 1 ? 2 : 1;
      const res = negamax(state, -beta, -alpha);
      const score = -res.score; // c'est l'adversaire qui joue -> négation
      state.current = savedCurrent;
      state.phase = 'give';
      state.pieceInHand = null;
      state.available.add(piece);
      if (score > best) { best = score; bestMove = { gift: piece }; }
      alpha = Math.max(alpha, best);
      if (alpha >= beta) break;
    }
    if (bestMove === null) return { score: 0 };
    return { score: best, ...bestMove };
  }

  function searchBestMove(game) {
    const state = cloneState(game);
    return negamax(state, -Infinity, Infinity);
  }

  window.QuartoAI = { choosePlacement, chooseGift };
})();
