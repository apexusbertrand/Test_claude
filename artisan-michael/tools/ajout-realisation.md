# Ajouter un chantier

1. Choisir un slug court, sans accent ni espace : `cuisine-credence-roubaix`.
2. Créer `assets/realisations/<slug>/` et y mettre les photos converties (1600 px max, WebP) :

```bash
python3 - <<'EOF'
from PIL import Image, ImageOps
import sys, pathlib
src = pathlib.Path('chemin/vers/photos')          # dossier des photos d'origine
dst = pathlib.Path('assets/realisations/<slug>')   # dossier du chantier
dst.mkdir(parents=True, exist_ok=True)
for i, f in enumerate(sorted(src.glob('*.jp*g')), 1):
    im = ImageOps.exif_transpose(Image.open(f)).convert('RGB')
    im.thumbnail((1600, 1600))
    im.save(dst / f'{i:02d}.webp', 'WEBP', quality=80, method=6)
EOF
```

3. Dans `content/realisations.json`, ajouter une entrée en tête de la liste `realisations` (la plus récente s'affiche en premier sur l'accueil) :

```json
{
  "slug": "cuisine-credence-roubaix",
  "titre": "Une crédence en zellige",
  "date": "2026-03",
  "date_affichee": "Mars 2026",
  "commune": "Roubaix",
  "metiers": ["carrelage"],
  "fictif": false,
  "duree": "2 jours",
  "resume": "Deux phrases sur le chantier.",
  "couverture": "03.webp",
  "avant": "01.webp",
  "apres": "03.webp",
  "video": null,
  "etapes": [
    { "titre": "Avant", "metier": null, "photo": "01.webp", "alt": "Description de la photo", "texte": "Ce qu'on voit, ce qu'on a fait." }
  ]
}
```

`metiers` prend `carrelage`, `platrerie`, `couverture`. `avant` et `apres` sont optionnels : s'ils sont tous les deux remplis, la page affiche le comparateur.

4. Vidéos : les mettre sur YouTube en « non répertorié » et noter l'identifiant dans `video` (affichage à brancher, voir le brief). Pas de MP4 de plus de 10 Mo dans le dépôt.
5. Vérifier en local (`python3 -m http.server`), puis commit et push.
