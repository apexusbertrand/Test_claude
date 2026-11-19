"""Prépare les images du site à partir des fichiers d'origine.

- image d'accueil : WebP et JPEG en plusieurs largeurs ;
- logo : WebP/PNG, et icônes (favicon, apple-touch) tirées du monogramme ;
- textures de démonstration des 2 granulats d'exemple (générées, raccordables).

Usage : python3 tools/images.py (depuis nataliia-resine/site)
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT.parent / "assets" / "maquette"
OUT = ROOT / "static" / "img"
OUT.mkdir(parents=True, exist_ok=True)


def hero():
    im = Image.open(SRC / "hero-terrasse.png").convert("RGB")
    for w in (640, 1024, 1672):
        h = round(im.height * w / im.width)
        r = im.resize((w, h), Image.LANCZOS)
        r.save(OUT / f"accueil-{w}.webp", "WEBP", quality=80, method=6)
        r.save(OUT / f"accueil-{w}.jpg", "JPEG", quality=82, optimize=True, progressive=True)
    # Image de partage (Open Graph) 1200 x 630
    og = im.resize((1200, round(im.height * 1200 / im.width)), Image.LANCZOS)
    top = (og.height - 630) // 2
    og.crop((0, top, 1200, top + 630)).save(OUT / "partage.jpg", "JPEG", quality=82, optimize=True)


def logo():
    im = Image.open(SRC / "logo-nataliia-resine.jpg").convert("RGB")
    for w in (320, 640):
        h = round(im.height * w / im.width)
        r = im.resize((w, h), Image.LANCZOS)
        r.save(OUT / f"logo-{w}.webp", "WEBP", quality=88, method=6)
        r.save(OUT / f"logo-{w}.jpg", "JPEG", quality=88, optimize=True)
    # Icône : monogramme NS centré, recadré en carré sur fond noir
    # Monogramme sur toute la hauteur du logo (640 px), centré
    side = im.height
    cx = im.width // 2
    sq = im.crop((cx - side // 2, 0, cx + side // 2, side))
    for s, name in ((32, "favicon-32.png"), (180, "apple-touch-icon.png"), (192, "icone-192.png"), (512, "icone-512.png")):
        sq.resize((s, s), Image.LANCZOS).save(OUT / name, "PNG", optimize=True)


def granulat(name, palette, seed, size=900, grain=(9, 17)):
    """Texture raccordable : petits galets arrondis serrés, avec ombre et reflet."""
    rnd = random.Random(seed)
    scale = 2
    S = size * scale
    base = tuple(max(0, c - 70) for c in palette[0]) if sum(palette[0]) > 300 else tuple(min(255, c + 25) for c in palette[0])
    img = Image.new("RGB", (S, S), base)
    shadow = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(img)
    ds = ImageDraw.Draw(shadow)
    n = int((S * S) / ((sum(grain) / 2 * scale) ** 2) * 1.35)
    stones = []
    for _ in range(n):
        r = rnd.uniform(*grain) * scale / 2
        stones.append((rnd.uniform(0, S), rnd.uniform(0, S), r * rnd.uniform(0.75, 1.0), r, rnd.uniform(0, math.pi)))
    offsets = [(ox, oy) for ox in (-S, 0, S) for oy in (-S, 0, S)]
    for x, y, rx, ry, a in stones:
        col = rnd.choice(palette)
        jitter = rnd.randint(-14, 14)
        col = tuple(min(255, max(0, c + jitter)) for c in col)
        for ox, oy in offsets:
            X, Y = x + ox, y + oy
            if X < -ry * 2 or X > S + ry * 2 or Y < -ry * 2 or Y > S + ry * 2:
                continue
            pts = []
            for k in range(18):
                t = 2 * math.pi * k / 18
                px, py = rx * math.cos(t), ry * math.sin(t)
                pts.append((X + px * math.cos(a) - py * math.sin(a), Y + px * math.sin(a) + py * math.cos(a)))
            ds.polygon([(p[0] + 2 * scale, p[1] + 3 * scale) for p in pts], fill=120)
            d.polygon(pts, fill=col)
            hl = tuple(min(255, c + 45) for c in col)
            d.ellipse((X - rx * 0.45, Y - ry * 0.55, X - rx * 0.05, Y - ry * 0.2), fill=hl)
    shadow = shadow.filter(ImageFilter.GaussianBlur(3 * scale))
    dark = Image.new("RGB", (S, S), (0, 0, 0))
    img = Image.composite(dark, img, shadow.point(lambda v: v // 3))
    img = img.filter(ImageFilter.GaussianBlur(0.6 * scale)).resize((size, size), Image.LANCZOS)
    img.save(OUT / f"{name}.webp", "WEBP", quality=85, method=6)
    img.save(OUT / f"{name}.jpg", "JPEG", quality=85, optimize=True)


if __name__ == "__main__":
    hero()
    logo()
    granulat("exemple-granulat-clair", [(236, 226, 206), (222, 206, 178), (245, 240, 230), (205, 188, 160), (190, 172, 145)], seed=7)
    granulat("exemple-granulat-fonce", [(62, 60, 58), (48, 46, 44), (82, 79, 75), (70, 67, 64), (104, 100, 95), (200, 196, 188)], seed=11)
    for p in sorted(OUT.iterdir()):
        print(f"{p.name:32} {p.stat().st_size // 1024:>5} Ko")
