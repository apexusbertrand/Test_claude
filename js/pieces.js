/*
 * Quarto — rendu SVG des pièces.
 * Chaque pièce (0..15) est dessinée selon ses 4 caractéristiques :
 *   couleur (claire/foncée), hauteur (petite/grande),
 *   forme (ronde/carrée), remplissage (pleine/creuse).
 */

(function () {
  function pieceSVG(piece) {
    const dark = (piece & 1) !== 0;    // couleur
    const tall = (piece & 2) !== 0;    // hauteur
    const square = (piece & 4) !== 0;  // forme
    const hollow = (piece & 8) !== 0;  // remplissage

    // La hauteur est représentée par la taille de la pièce.
    const size = tall ? 78 : 56;
    const off = (100 - size) / 2;
    const fill = dark ? 'var(--piece-dark)' : 'var(--piece-light)';
    const edge = dark ? 'var(--piece-dark-edge)' : 'var(--piece-light-edge)';
    const holeFill = dark ? '#1c1730' : '#bda574';

    let body, hole;
    if (square) {
      const r = 10;
      body = `<rect x="${off}" y="${off}" width="${size}" height="${size}" rx="${r}" ry="${r}"
                fill="${fill}" stroke="${edge}" stroke-width="3"/>`;
      const hs = size * 0.42;
      const hoff = (100 - hs) / 2;
      hole = `<rect x="${hoff}" y="${hoff}" width="${hs}" height="${hs}" rx="5" ry="5" fill="${holeFill}"/>`;
    } else {
      const rad = size / 2;
      body = `<circle cx="50" cy="50" r="${rad}" fill="${fill}" stroke="${edge}" stroke-width="3"/>`;
      const hr = size * 0.21;
      hole = `<circle cx="50" cy="50" r="${hr}" fill="${holeFill}"/>`;
    }

    return `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <defs>
        <radialGradient id="sh${piece}" cx="35%" cy="28%" r="75%">
          <stop offset="0%" stop-color="rgba(255,255,255,.35)"/>
          <stop offset="55%" stop-color="rgba(255,255,255,0)"/>
        </radialGradient>
      </defs>
      ${body}
      ${square
        ? `<rect x="${off}" y="${off}" width="${size}" height="${size}" rx="10" ry="10" fill="url(#sh${piece})"/>`
        : `<circle cx="50" cy="50" r="${size / 2}" fill="url(#sh${piece})"/>`}
      ${hollow ? hole : ''}
    </svg>`;
  }

  /** Libellé texte accessible d'une pièce. */
  function pieceLabel(piece) {
    const parts = [
      (piece & 4) ? 'carrée' : 'ronde',
      (piece & 1) ? 'foncée' : 'claire',
      (piece & 2) ? 'grande' : 'petite',
      (piece & 8) ? 'creuse' : 'pleine',
    ];
    return parts.join(', ');
  }

  window.QuartoPieces = { pieceSVG, pieceLabel };
})();
