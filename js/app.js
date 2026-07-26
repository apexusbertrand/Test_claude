/*
 * Quarto — contrôleur d'interface.
 * Relie le moteur (QuartoGame), l'IA (QuartoAI) et le rendu (QuartoPieces)
 * aux éléments du DOM.
 */

(function () {
  const { pieceSVG, pieceLabel } = window.QuartoPieces;

  // --- Références DOM --------------------------------------------------------
  const el = (id) => document.getElementById(id);
  const boardEl = el('board');
  const trayEl = el('tray');
  const handPieceEl = el('handPiece');
  const handTitleEl = el('handTitle');
  const handSubEl = el('handSub');
  const handWrapEl = el('hand');
  const statusEl = el('status');
  const statusMainEl = el('statusMain');
  const statusSubEl = el('statusSub');
  const trayCountEl = el('trayCount');
  const trayTitleEl = el('trayTitle');

  // --- Réglages choisis dans la modale --------------------------------------
  const settings = { mode: '2p', level: 'moyen', first: 1, squares: false };
  let game = null;
  let busy = false;      // vrai pendant que l'IA « réfléchit »
  let hintCell = null;   // indice actif

  // --- Construction du plateau ----------------------------------------------
  const cells = [];
  for (let i = 0; i < 16; i++) {
    const c = document.createElement('div');
    c.className = 'cell';
    c.dataset.index = i;
    c.addEventListener('click', () => onCellClick(i));
    boardEl.appendChild(c);
    cells.push(c);
  }

  const trayCells = [];
  for (let p = 0; p < 16; p++) {
    const t = document.createElement('div');
    t.className = 'tpiece';
    t.dataset.piece = p;
    t.innerHTML = pieceSVG(p);
    t.title = pieceLabel(p);
    t.addEventListener('click', () => onTrayClick(p));
    trayEl.appendChild(t);
    trayCells.push(t);
  }

  // --- Noms des joueurs ------------------------------------------------------
  function playerName(n) {
    if (game.mode === 'ai') return n === game.aiPlayer ? 'L\'IA' : 'Toi';
    return 'Joueur ' + n;
  }

  // --- Rendu -----------------------------------------------------------------
  function render() {
    // Plateau
    for (let i = 0; i < 16; i++) {
      const cell = cells[i];
      const piece = game.board[i];
      const wasEmpty = !cell.firstChild;
      if (piece === null) {
        cell.innerHTML = '';
      } else if (cell.dataset.piece !== String(piece)) {
        cell.innerHTML = `<div class="piece">${pieceSVG(piece)}</div>`;
        cell.dataset.piece = String(piece);
        if (i === game.lastPlaced) cell.querySelector('.piece').classList.add('placed');
      }
      if (piece === null) cell.dataset.piece = '';

      const canPlace = !busy && game.winner === null && game.phase === 'place'
        && piece === null && !game.isAiTurn();
      cell.classList.toggle('playable', canPlace);
      cell.classList.toggle('win', false);
      cell.classList.toggle('hint', hintCell === i);
    }

    // Ligne gagnante
    if (game.winningLine) {
      for (const i of game.winningLine) cells[i].classList.add('win');
    }

    // Réserve
    const canGive = !busy && game.winner === null && game.phase === 'give' && !game.isAiTurn();
    trayEl.classList.toggle('selectable', canGive);
    let count = 0;
    for (let p = 0; p < 16; p++) {
      const used = !game.available.has(p) || game.pieceInHand === p;
      trayCells[p].classList.toggle('used', used);
      trayCells[p].classList.toggle('hint', false);
      if (!used) count++;
    }
    trayCountEl.textContent = count;

    // Pièce en main
    if (game.pieceInHand !== null) {
      handPieceEl.className = 'hand-piece';
      handPieceEl.innerHTML = pieceSVG(game.pieceInHand);
      handTitleEl.textContent = 'Pièce à poser';
      handSubEl.textContent = pieceLabel(game.pieceInHand);
      handWrapEl.classList.toggle('pulse', game.phase === 'place' && !game.isAiTurn() && !busy);
    } else {
      handPieceEl.className = 'hand-piece empty';
      handPieceEl.innerHTML = '';
      handTitleEl.textContent = 'Pièce à poser';
      handSubEl.textContent = 'Aucune pièce en main';
      handWrapEl.classList.remove('pulse');
    }

    updateStatus();
  }

  function updateStatus() {
    statusEl.classList.toggle('p2', game.currentPlayer === 2);
    if (game.winner === 'draw') {
      statusMainEl.textContent = 'Match nul';
      statusSubEl.textContent = 'Toutes les pièces sont posées';
      return;
    }
    if (game.winner !== null) {
      statusMainEl.textContent = playerName(game.winner) + ' gagne !';
      statusSubEl.textContent = 'Quarto complété';
      return;
    }
    const who = playerName(game.currentPlayer);
    if (busy && game.isAiTurn()) {
      statusMainEl.textContent = 'L\'IA réfléchit…';
      statusSubEl.textContent = game.phase === 'place' ? 'Elle place sa pièce' : 'Elle choisit une pièce';
      return;
    }
    if (game.phase === 'give') {
      statusMainEl.textContent = who + ' : donne une pièce';
      statusSubEl.textContent = 'Choisis dans la réserve la pièce que l\'adversaire devra poser';
      trayTitleEl.textContent = 'Choisis une pièce à donner';
    } else {
      statusMainEl.textContent = who + ' : place la pièce';
      statusSubEl.textContent = 'Pose la pièce reçue sur une case libre';
      trayTitleEl.textContent = 'Pièces disponibles';
    }
  }

  // --- Interactions ----------------------------------------------------------
  function onCellClick(i) {
    if (busy || game.winner !== null) return;
    if (game.phase !== 'place' || game.isAiTurn()) return;
    if (game.board[i] !== null) return;
    hintCell = null;
    game.placePiece(i);
    render();
    if (game.winner !== null) return onGameEnd();
    scheduleAI();
  }

  function onTrayClick(p) {
    if (busy || game.winner !== null) return;
    if (game.phase !== 'give' || game.isAiTurn()) return;
    if (!game.available.has(p)) return;
    hintCell = null;
    game.givePiece(p);
    render();
    scheduleAI();
  }

  // --- Tour de l'IA (enchaîne place puis don si besoin) ----------------------
  function scheduleAI() {
    if (!game.isAiTurn() || game.winner !== null) return;
    busy = true;
    render();
    const level = game.aiLevel;
    const delay = level === 'facile' ? 350 : level === 'moyen' ? 520 : 650;
    setTimeout(() => {
      if (game.winner !== null || !game.isAiTurn()) { busy = false; render(); return; }
      try {
        if (game.phase === 'place') {
          const cell = window.QuartoAI.choosePlacement(game);
          game.placePiece(cell);
        } else {
          const gift = window.QuartoAI.chooseGift(game);
          game.givePiece(gift);
        }
      } catch (err) {
        console.error('Erreur IA', err);
      }
      if (game.winner !== null) { busy = false; render(); return onGameEnd(); }
      if (game.isAiTurn()) {
        scheduleAI(); // encore à l'IA (elle vient de poser, elle doit donner)
      } else {
        busy = false;
        render();
      }
    }, delay);
  }

  // --- Indice ----------------------------------------------------------------
  function showHint() {
    if (busy || game.winner !== null || game.isAiTurn()) return;
    const core = window.QuartoCore;
    if (game.phase === 'place') {
      // Peut-on gagner avec la pièce en main ?
      for (let c = 0; c < 16; c++) {
        if (game.board[c] !== null) continue;
        game.board[c] = game.pieceInHand;
        const win = core.findWinningLine(game.board, game.useSquares) !== null;
        game.board[c] = null;
        if (win) { hintCell = c; render(); flashStatus('Une case gagnante existe ! 🎯'); return; }
      }
      flashStatus('Aucune victoire immédiate — pose sans créer de menace.');
    } else {
      // Mettre en évidence les pièces « sûres » à donner.
      const safe = [];
      for (const p of game.available) {
        let danger = false;
        for (let c = 0; c < 16 && !danger; c++) {
          if (game.board[c] !== null) continue;
          game.board[c] = p;
          if (core.findWinningLine(game.board, game.useSquares)) danger = true;
          game.board[c] = null;
        }
        if (!danger) safe.push(p);
      }
      if (safe.length === 0) {
        flashStatus('⚠️ Toutes les pièces restantes peuvent faire gagner l\'adversaire.');
      } else {
        for (const p of safe) trayCells[p].classList.add('hint');
        flashStatus(safe.length + ' pièce(s) sûre(s) surlignée(s) en vert.');
      }
    }
  }

  let flashTimer = null;
  function flashStatus(msg) {
    statusSubEl.textContent = msg;
    clearTimeout(flashTimer);
    flashTimer = setTimeout(() => updateStatus(), 3200);
  }

  // --- Cycle de partie -------------------------------------------------------
  function startGame() {
    game = new window.QuartoGame({
      mode: settings.mode,
      aiLevel: settings.level,
      aiPlayer: settings.mode === 'ai' ? (settings.first === 1 ? 2 : 1) : 2,
      useSquares: settings.squares,
    });
    // En mode IA, le premier joueur peut être l'IA.
    game.currentPlayer = settings.first;
    game.phase = 'give';
    busy = false;
    hintCell = null;
    closeOverlay(setupOverlay);
    closeOverlay(endOverlay);
    render();
    scheduleAI();
  }

  function onGameEnd() {
    render();
    setTimeout(() => {
      const icon = el('endIcon');
      const title = el('endTitle');
      const sub = el('endSub');
      if (game.winner === 'draw') {
        icon.textContent = '🤝';
        title.textContent = 'Match nul';
        sub.textContent = 'Personne n\'a réussi à aligner un Quarto.';
      } else {
        const won = game.winner;
        const isHumanWin = game.mode !== 'ai' || won !== game.aiPlayer;
        icon.textContent = isHumanWin ? '🏆' : '🤖';
        title.innerHTML = `<span class="confetti-name">${playerName(won)}</span> gagne !`;
        sub.textContent = game.mode === 'ai'
          ? (isHumanWin ? 'Bien joué, tu as battu l\'IA.' : 'L\'IA a complété un Quarto. À la revanche !')
          : 'Quarto ! Un alignement a été complété.';
      }
      openOverlay(endOverlay);
    }, 650);
  }

  // --- Modales ---------------------------------------------------------------
  const setupOverlay = el('setupOverlay');
  const endOverlay = el('endOverlay');
  function openOverlay(o) { o.classList.add('show'); }
  function closeOverlay(o) { o.classList.remove('show'); }

  function syncSetupUI() {
    document.querySelectorAll('#modeSeg button').forEach((b) =>
      b.classList.toggle('active', b.dataset.mode === settings.mode));
    document.querySelectorAll('#levelSeg button').forEach((b) =>
      b.classList.toggle('active', b.dataset.level === settings.level));
    document.querySelectorAll('#firstSeg button').forEach((b) =>
      b.classList.toggle('active', b.dataset.first === String(settings.first)));
    el('squaresToggle').checked = settings.squares;
    const aiOnly = settings.mode === 'ai';
    el('levelField').classList.toggle('hidden', !aiOnly);
    el('firstField').classList.toggle('hidden', !aiOnly);
  }

  // --- Écouteurs d'événements ------------------------------------------------
  el('menuBtn').addEventListener('click', () => { syncSetupUI(); openOverlay(setupOverlay); });
  el('newBtn').addEventListener('click', () => { syncSetupUI(); openOverlay(setupOverlay); });
  el('cancelBtn').addEventListener('click', () => closeOverlay(setupOverlay));
  el('startBtn').addEventListener('click', startGame);
  el('hintBtn').addEventListener('click', showHint);
  el('endReplayBtn').addEventListener('click', startGame);
  el('endMenuBtn').addEventListener('click', () => { closeOverlay(endOverlay); syncSetupUI(); openOverlay(setupOverlay); });

  document.querySelectorAll('#modeSeg button').forEach((b) =>
    b.addEventListener('click', () => { settings.mode = b.dataset.mode; syncSetupUI(); }));
  document.querySelectorAll('#levelSeg button').forEach((b) =>
    b.addEventListener('click', () => { settings.level = b.dataset.level; syncSetupUI(); }));
  document.querySelectorAll('#firstSeg button').forEach((b) =>
    b.addEventListener('click', () => { settings.first = Number(b.dataset.first); syncSetupUI(); }));
  el('squaresToggle').addEventListener('change', (e) => { settings.squares = e.target.checked; });

  // --- Démarrage : partie 2 joueurs par défaut, menu ouvert -----------------
  game = new window.QuartoGame({ mode: '2p' });
  render();
  syncSetupUI();
  openOverlay(setupOverlay);

  // --- Service worker (mode hors-ligne / installation) -----------------------
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('sw.js').catch(() => {});
    });
  }
})();
