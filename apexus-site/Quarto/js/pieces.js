/*
 * Quarto — rendu SVG des pièces, style « 3D bois ».
 * Chaque pièce (0..15) est vue légèrement de biais et possède une VRAIE
 * épaisseur (corps extrudé) :
 *   - couleur (bit 0)      : bois clair / bois foncé (noyer)
 *   - hauteur (bit 1)      : grande = corps haut / petite = corps bas
 *                            (même diamètre : seule la hauteur change)
 *   - forme (bit 2)        : ronde (cylindre) / carrée (bloc)
 *   - remplissage (bit 3)  : pleine / creuse (trou sur le dessus)
 */

(function () {
  // Palettes bois.
  const WOOD = {
    light: {
      topA: '#f3e2ba', topB: '#e4c88c',
      sideA: '#d9bc80', sideB: '#bd9d5c',
      edge: '#9c7f4c',
      hole: '#b18d52', holeDark: '#8a6a39',
    },
    dark: {
      topA: '#9c6a3d', topB: '#7d4d25',
      sideA: '#603a1a', sideB: '#472810',
      edge: '#301c0b',
      hole: '#301c0d', holeDark: '#1c1007',
    },
  };

  function pieceSVG(piece) {
    const dark = (piece & 1) !== 0;
    const tall = (piece & 2) !== 0;
    const square = (piece & 4) !== 0;
    const hollow = (piece & 8) !== 0;
    const w = dark ? WOOD.dark : WOOD.light;

    // Géométrie (viewBox 100x100). Même largeur pour toutes les pièces ;
    // seule la hauteur du corps distingue grande / petite.
    const cx = 50;
    const rx = 27;             // demi-largeur (identique grande/petite)
    const capRy = 10;          // demi-épaisseur du dessus (perspective)
    // Fort contraste de hauteur : petites très plates, grandes bien hautes.
    const bodyH = tall ? 42 : 7;
    const total = 2 * capRy + bodyH;
    const figTop = (100 - total) / 2;
    const topY = figTop + capRy;   // centre du dessus
    const botY = topY + bodyH;     // centre de la base

    const id = 'p' + piece;
    const gradients = `
      <linearGradient id="side${id}" x1="0" y1="0" x2="1" y2="0">
        <stop offset="0" stop-color="${w.sideB}"/>
        <stop offset="0.5" stop-color="${w.sideA}"/>
        <stop offset="1" stop-color="${w.sideB}"/>
      </linearGradient>
      <linearGradient id="top${id}" x1="0" y1="0" x2="0.2" y2="1">
        <stop offset="0" stop-color="${w.topA}"/>
        <stop offset="1" stop-color="${w.topB}"/>
      </linearGradient>
      <radialGradient id="holeg${id}" cx="50%" cy="42%" r="60%">
        <stop offset="0" stop-color="${w.holeDark}"/>
        <stop offset="1" stop-color="${w.hole}"/>
      </radialGradient>`;

    // Ombre portée au sol (ancrage 3D).
    const shadow = `<ellipse cx="${cx}" cy="${botY + capRy * 0.7}" rx="${rx * 0.9}" ry="${capRy * 0.5}" fill="rgba(0,0,0,0.30)"/>`;

    let body;
    if (square) {
      // Bloc : face avant (corps) + dessus foreshortené.
      body = `
        <rect x="${cx - rx}" y="${topY}" width="${rx * 2}" height="${bodyH}" rx="6" ry="6"
              fill="url(#side${id})" stroke="${w.edge}" stroke-width="1.2"/>
        <rect x="${cx - rx}" y="${topY - capRy}" width="${rx * 2}" height="${capRy * 2}" rx="6" ry="6"
              fill="url(#top${id})" stroke="${w.edge}" stroke-width="1.2"/>`;
      if (hollow) {
        const hw = rx * 0.34, hh = capRy * 0.72;
        body += `<rect x="${cx - hw}" y="${topY - hh}" width="${hw * 2}" height="${hh * 2}" rx="3" ry="3" fill="url(#holeg${id})" stroke="${w.holeDark}" stroke-width="0.8"/>`;
      }
    } else {
      // Cylindre : base + corps ombré + dessus elliptique.
      const outline = `M ${cx - rx} ${topY} L ${cx - rx} ${botY} A ${rx} ${capRy} 0 0 0 ${cx + rx} ${botY} L ${cx + rx} ${topY}`;
      body = `
        <ellipse cx="${cx}" cy="${botY}" rx="${rx}" ry="${capRy}" fill="${w.sideB}"/>
        <rect x="${cx - rx}" y="${topY}" width="${rx * 2}" height="${bodyH}" fill="url(#side${id})"/>
        <path d="${outline}" fill="none" stroke="${w.edge}" stroke-width="1.2"/>
        <ellipse cx="${cx}" cy="${topY}" rx="${rx}" ry="${capRy}" fill="url(#top${id})" stroke="${w.edge}" stroke-width="1.2"/>`;
      if (hollow) {
        body += `<ellipse cx="${cx}" cy="${topY}" rx="${rx * 0.42}" ry="${capRy * 0.42}" fill="url(#holeg${id})" stroke="${w.holeDark}" stroke-width="0.8"/>`;
      }
    }

    return `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <defs>${gradients}</defs>
      ${shadow}
      ${body}
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
