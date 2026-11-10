// Fabrique la vidéo de l'animation du logo à partir de animation.html.
// Usage (depuis artisan-michael/) :
//   python3 -m http.server 8765 &
//   node tools/logo-animation/render.cjs <dossier-images-temporaire> [fps] [--keyframes]
//   ffmpeg -framerate 30 -i <dossier>/f%04d.png -c:v libx264 -pix_fmt yuv420p -crf 24 -preset slow -movflags +faststart assets/brand/logo-animation.mp4
const { chromium } = require('playwright');
const fs = require('fs');
(async () => {
  const dir = process.argv[2], fps = +(process.argv[3] || 30), keys = process.argv.includes('--keyframes');
  fs.mkdirSync(dir, { recursive: true });
  const b = await chromium.launch();
  const p = await b.newPage({ viewport: { width: 1080, height: 1080 }, deviceScaleFactor: 1 });
  await p.goto('http://localhost:8765/tools/logo-animation/animation.html?render', { waitUntil: 'networkidle' });
  await p.evaluate(() => window.ready);
  const dur = await p.evaluate(() => window.DUR);
  const times = keys ? [0.4, 1.4, 2.3, 2.9, 3.6, 4.3, 4.9, 5.4, 5.9, 6.5, 7.0, 7.6, 8.0, 8.6, 9.3, 10.8]
                     : Array.from({ length: Math.round(dur * fps) + 1 }, (_, i) => i / fps);
  const c = await p.$('canvas');
  for (let i = 0; i < times.length; i++) {
    await p.evaluate(t => window.render(t), times[i]);
    await c.screenshot({ path: `${dir}/f${String(i).padStart(4, '0')}.png` });
  }
  console.log(times.length, 'images');
  await b.close();
})();
