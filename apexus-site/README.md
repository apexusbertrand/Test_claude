# Apexus-BMT — Site vitrine

Site vitrine à page unique pour Apexus-BMT (Bertrand Marat), conseil en
transformation des processus métiers et en architecture des systèmes
d'information.

## Contenu

- `index.html` — la page complète (HTML/CSS/JS), autonome : aucune
  dépendance externe hors Google Fonts (police *Inter*). Logo, photo et
  icônes sont intégrés directement dans le fichier.

## Ouvrir en local

Il suffit d'ouvrir `index.html` dans un navigateur, aucun serveur requis.

## Héberger gratuitement avec GitHub Pages

1. Dans les paramètres de ce dépôt : **Settings → Pages**.
2. Sous « Build and deployment », choisir **Deploy from a branch**.
3. Sélectionner la branche `main` et le dossier `/ (root)`, puis **Save**.
4. Le site est publié en quelques minutes à une adresse du type
   `https://<votre-compte>.github.io/<nom-du-depot>/`.
5. Un nom de domaine personnalisé (ex. `apexus-bmt.fr`) peut ensuite être
   ajouté dans les mêmes paramètres, en pointant un enregistrement DNS
   CNAME (ou A) chez le registrar vers GitHub Pages.

## Mise à jour du contenu

Le fichier est un simple export statique : toute modification de
contenu implique de régénérer et remplacer `index.html`.
