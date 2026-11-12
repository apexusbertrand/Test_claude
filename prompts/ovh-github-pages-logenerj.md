# Prompt Claude for Chrome : publier le site LOGE&NERJ sur logenerj.fr (OVH + GitHub Pages)

À coller dans Claude for Chrome, avec un onglet déjà connecté à OVH (espace client) et un onglet connecté à GitHub (compte apexusbertrand).

```text
Tu vas configurer la publication du site vitrine LOGE&NERJ sur le domaine logenerj.fr, hébergé par GitHub Pages, avec un DNS chez OVH. Je suis connecté à OVH (espace client) et à GitHub (compte apexusbertrand) dans ce navigateur.

Règles :
- Ne rien acheter, ne rien payer, ne rien commander. Si une étape demande un paiement ou une commande, arrête-toi et dis-le moi.
- Ne touche qu'au domaine logenerj.fr. Ne modifie jamais les enregistrements MX, les TXT de type SPF/DKIM/DMARC, ni les serveurs DNS (NS).
- Avant toute modification dans OVH, relève l'état actuel complet de la zone DNS (type, sous-domaine, cible, TTL) et montre-le moi.
- Demande-moi confirmation avant chaque suppression.
- Si une double authentification ou un code est demandé, arrête-toi et demande-le moi.
- À la fin, donne-moi un compte rendu : ce qui a été fait, ce qui reste, les valeurs saisies.

ÉTAPE 1 — Vérifier le domaine chez OVH
1. Dans l'espace client OVH, va dans Web Cloud > Noms de domaine.
2. Vérifie que logenerj.fr apparaît dans la liste. S'il n'y est pas, arrête tout et dis-le moi (le domaine n'est pas encore acheté ou est sur un autre compte).
3. Ouvre logenerj.fr > onglet « Zone DNS ». Relève et affiche-moi tous les enregistrements existants.
4. Ouvre l'onglet « Redirection » (ou « Redirections ») : s'il existe une redirection web sur logenerj.fr ou www.logenerj.fr, signale-la, et supprime-la après mon accord (elle crée des enregistrements A/CNAME qui entrent en conflit avec GitHub Pages).

ÉTAPE 2 — Créer le dépôt GitHub du site
1. Sur GitHub, crée un nouveau dépôt public nommé « logenerj-site » sur le compte apexusbertrand.
2. Description : « Site vitrine LOGE&NERJ – rénovation de l'habitat à Somain ».
3. Ne coche rien : pas de README, pas de .gitignore, pas de licence (le dépôt doit rester vide, les fichiers y seront poussés ensuite).
4. Donne-moi l'URL du dépôt.

ÉTAPE 3 — Vérifier le domaine sur GitHub (protection contre la prise de contrôle du domaine)
1. Va dans les paramètres du compte GitHub (photo de profil > Settings) > Pages > « Add a domain ».
2. Saisis logenerj.fr et valide.
3. GitHub affiche un enregistrement TXT à créer, du type :
   nom : _github-pages-challenge-apexusbertrand.logenerj.fr
   valeur : une suite de caractères
   Note ces deux valeurs et laisse cet onglet ouvert (le bouton « Verify » servira à l'étape 5).

ÉTAPE 4 — Configurer la zone DNS chez OVH (logenerj.fr > Zone DNS)
1. Supprime, après mon accord, uniquement :
   - les enregistrements A et AAAA dont le sous-domaine est vide (logenerj.fr) ;
   - les enregistrements A, AAAA ou CNAME dont le sous-domaine est « www ».
   (Ce sont en général les enregistrements de la page parking d'OVH.)
2. Ajoute ces enregistrements, TTL par défaut :
   - A, sous-domaine vide, cible 185.199.108.153
   - A, sous-domaine vide, cible 185.199.109.153
   - A, sous-domaine vide, cible 185.199.110.153
   - A, sous-domaine vide, cible 185.199.111.153
   - AAAA, sous-domaine vide, cible 2606:50c0:8000::153
   - AAAA, sous-domaine vide, cible 2606:50c0:8001::153
   - AAAA, sous-domaine vide, cible 2606:50c0:8002::153
   - AAAA, sous-domaine vide, cible 2606:50c0:8003::153
   - CNAME, sous-domaine « www », cible « apexusbertrand.github.io. » (avec le point final)
   - TXT, sous-domaine « _github-pages-challenge-apexusbertrand », valeur relevée à l'étape 3
3. Affiche-moi la zone DNS finale.

ÉTAPE 5 — Valider le domaine sur GitHub
1. Reviens sur l'onglet GitHub de l'étape 3 et clique sur « Verify ». La propagation DNS peut prendre de quelques minutes à quelques heures : si la vérification échoue, réessaie une fois après 10 minutes, puis arrête-toi et dis-le moi.

ÉTAPE 6 — Point d'arrêt
Arrête-toi ici et dis-moi : « DNS prêt, dépôt créé. Demande à Claude Code de pousser le site dans apexusbertrand/logenerj-site. » Ne configure pas encore GitHub Pages dans le dépôt : il est vide.

ÉTAPE 7 — À faire seulement quand je te le redemande, une fois le site poussé
1. Dans le dépôt apexusbertrand/logenerj-site : Settings > Pages.
2. Source : « Deploy from a branch », branche « main », dossier « / (root) », puis Save.
3. Custom domain : www.logenerj.fr, puis Save. Attends que la vérification DNS passe au vert.
4. Coche « Enforce HTTPS » dès que la case est disponible (le certificat peut mettre jusqu'à 24 h). Si elle n'est pas encore disponible, dis-le moi.
5. Ouvre https://www.logenerj.fr et https://logenerj.fr et dis-moi ce qui s'affiche.
```

## Ce que Claude Code fait entre l'étape 6 et l'étape 7

Pousser le contenu de `artisan-michael/` à la racine de `apexusbertrand/logenerj-site` (branche `main`), avec un fichier `CNAME` contenant `www.logenerj.fr`. Le dépôt doit être ajouté à la session Claude Code pour pouvoir y écrire.

## À trancher avant la vraie mise en ligne

- Le domaine logenerj.fr doit être acheté (sa disponibilité n'a pas pu être vérifiée depuis la session).
- Le site est encore une maquette : `noindex`, `robots.txt` bloquant, bandeau « maquette », téléphone et adresse fictifs. Pour une simple validation par Michael c'est voulu ; pour un lancement public, il faut les retirer.
