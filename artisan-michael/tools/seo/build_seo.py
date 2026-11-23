#!/usr/bin/env python3
"""Référencement du site LOGE&NERJ.

À relancer après toute modification de content/competences.json, content/faq.json
ou content/realisations.json (depuis artisan-michael/ : python3 tools/seo/build_seo.py).

Ce script :
  1. génère une page statique par métier (pages « service » ciblées sur une recherche locale) ;
  2. réécrit, sur chaque page, le titre, la description, l'URL canonique et les balises de partage ;
  3. réécrit les données structurées (JSON-LD) de l'accueil et des pages métier ;
  4. met à jour sitemap.xml et la liste statique des métiers de savoir-faire.html.
Les zones gérées sont encadrées par des commentaires <!-- seo:... --> : ne pas les modifier à la main.
"""
import datetime
import html
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SITE = 'https://www.logenerj.fr/'
NOM = 'LOGE&NERJ'
VILLE = 'Somain'
TEL = '07 49 94 52 79'
TEL_INTL = '+33749945279'
EMAIL = 'logenerj@artisanat1.fr'
FACEBOOK = 'https://www.facebook.com/p/Logenerj-61591852021732/'
OG_IMAGE = SITE + 'assets/brand/og-image.jpg'
AUJOURDHUI = datetime.date.today().isoformat()
ENTREPRISE_ID = SITE + '#entreprise'


def lire(chemin):
    with open(os.path.join(ROOT, chemin), encoding='utf-8') as f:
        return f.read()


def ecrire(chemin, contenu):
    with open(os.path.join(ROOT, chemin), 'w', encoding='utf-8') as f:
        f.write(contenu)


def e(texte):
    return html.escape(str(texte), quote=True)


def sans_balises(texte):
    return re.sub(r'<[^>]+>', '', texte).replace('→', '').strip()


competences = json.loads(lire('content/competences.json'))
faq = json.loads(lire('content/faq.json'))['faq']
realisations = json.loads(lire('content/realisations.json'))

# --- Pages métier --------------------------------------------------------------------------------
# Titre (≤ 65 caractères environ), description (≤ 160), H1, et réalisations liées.
SERVICES = {
    'carrelage': {
        'slug': 'renovation-salle-de-bain-carrelage-somain.html',
        'titre': 'Rénovation de salle de bain et carrelage à Somain (59) · LOGE&NERJ',
        'h1': 'Rénovation de salle de bain et carrelage à Somain',
        'description': 'Salle de bain clé en main, douche à l’italienne, carrelage et faïence à Somain et alentours (Nord). Entreprise familiale LOGE&NERJ, devis gratuit.',
        'liens': [('realisations.html', 'Vidéo : une salle de bain refaite de A à Z'), ('realisations.html', 'Vidéo : rénovation d’un WC')],
    },
    'platrerie': {
        'slug': 'isolation-placo-combles-somain.html',
        'titre': 'Isolation, placo et aménagement de combles à Somain · LOGE&NERJ',
        'h1': 'Isolation, placo et aménagement de combles à Somain',
        'description': 'Isolation des murs et des combles, cloisons, doublages et faux plafonds à Somain (Nord). Une maison mieux isolée qui consomme moins. Devis gratuit.',
        'liens': [('chantier.html?c=combles-briques', 'Chantier : des combles en brique devenus une chambre')],
    },
    'couverture': {
        'slug': 'toiture-fenetre-de-toit-somain.html',
        'titre': 'Toiture, tuiles et fenêtres de toit à Somain (59) · LOGE&NERJ',
        'h1': 'Toiture et fenêtres de toit à Somain',
        'description': 'Pose de fenêtres de toit, réparation de tuiles, recherche de fuites et isolation de toiture à Somain et alentours (Nord). Devis gratuit LOGE&NERJ.',
        'liens': [('chantier.html?c=combles-briques', 'Chantier : ouverture du toit et pose de fenêtres de toit')],
    },
    'menuiserie': {
        'slug': 'fenetres-volets-portails-clotures-somain.html',
        'titre': 'Fenêtres, volets roulants, portails et clôtures à Somain · LOGE&NERJ',
        'h1': 'Fenêtres, volets roulants, portails et clôtures à Somain',
        'description': 'Pose et remplacement de fenêtres, volets roulants, portails et clôtures rigides à Somain (Nord), dépannage de fenêtres et de volets. Devis gratuit.',
        'liens': [],
    },
    'peinture': {
        'slug': 'peinture-interieure-somain.html',
        'titre': 'Peinture intérieure, murs et plafonds à Somain (59) · LOGE&NERJ',
        'h1': 'Peinture intérieure à Somain',
        'description': 'Préparation des supports, rebouchage et peinture des murs, plafonds et boiseries à Somain et alentours (Nord). Entreprise familiale LOGE&NERJ.',
        'liens': [],
    },
    'depannage': {
        'slug': 'depannage-electricite-plomberie-somain.html',
        'titre': 'Dépannage électricité, plomberie et fenêtres à Somain · LOGE&NERJ',
        'h1': 'Dépannage électricité, plomberie et fenêtres à Somain',
        'description': 'Panne électrique, fuite, volet ou fenêtre bloqués à Somain (Nord) : LOGE&NERJ dépanne et fait vos travaux d’électricité et de plomberie. 07 49 94 52 79.',
        'liens': [('realisations.html', 'Vidéo : électricité et plomberie refaites dans une salle de bain')],
    },
    'chauffage': {
        'slug': 'pompe-a-chaleur-somain.html',
        'titre': 'Installation de pompe à chaleur à Somain (59) · LOGE&NERJ',
        'h1': 'Installation de pompe à chaleur à Somain',
        'description': 'Installation de pompe à chaleur et conseil isolation + chauffage à Somain et alentours (Nord). Rénovation énergétique par LOGE&NERJ, devis gratuit.',
        'liens': [],
    },
}
GENERAUX = ['Le devis est payant ?', 'Vous intervenez où ?', 'Quels sont vos délais ?']

# --- Titres et descriptions des pages existantes -------------------------------------------------
PAGES = {
    'index.html': ('Rénovation de l’habitat à Somain (59) · LOGE&NERJ',
                   'LOGE&NERJ, entreprise familiale de rénovation à Somain (Nord) : salle de bain, isolation, toiture, fenêtres, plomberie, électricité, pompe à chaleur.', ''),
    'savoir-faire.html': ('Nos métiers de la rénovation à Somain (59) · LOGE&NERJ',
                          'Tous les métiers de la rénovation à Somain (Nord) : salle de bain, placo, isolation, toiture, fenêtres, peinture, électricité, plomberie, pompe à chaleur.', 'savoir-faire.html'),
    'realisations.html': ('Chantiers de rénovation en photos et vidéos à Somain · LOGE&NERJ',
                          'Les chantiers de LOGE&NERJ en photos et vidéos : combles aménagés, salle de bain refaite de A à Z, WC rénové. Rénovation de l’habitat à Somain (Nord).', 'realisations.html'),
    'chantier.html': ('Chantier de rénovation étape par étape · LOGE&NERJ (Somain)',
                      'Un chantier de rénovation de LOGE&NERJ raconté étape par étape, en photos : de l’état initial à la pièce livrée. Somain (Nord).', 'chantier.html?c=combles-briques'),
    'qui-suis-je.html': ('Entreprise familiale de rénovation à Somain · LOGE&NERJ',
                         'LOGE&NERJ, société familiale de rénovation de l’habitat à Somain (Nord) : tous corps d’état, un seul interlocuteur du devis à la fin des travaux.', 'qui-suis-je.html'),
    'contact.html': ('Devis gratuit et dépannage à Somain (59) · Contact LOGE&NERJ',
                     'Demandez un devis gratuit ou un dépannage à LOGE&NERJ, Somain (Nord) : 07 49 94 52 79, logenerj@artisanat1.fr, ou le formulaire avec vos photos.', 'contact.html'),
    'mentions-legales.html': ('Mentions légales et confidentialité · LOGE&NERJ',
                              'Mentions légales, hébergement et politique de confidentialité du site de LOGE&NERJ, entreprise de rénovation à Somain (Nord).', 'mentions-legales.html'),
}


def bloc_tete(titre, description, chemin):
    url = SITE + chemin
    return ('<!-- seo:tete -->\n'
            f'<link rel="canonical" href="{e(url)}">\n'
            '<meta property="og:type" content="website">\n'
            f'<meta property="og:site_name" content="{e(NOM)}">\n'
            '<meta property="og:locale" content="fr_FR">\n'
            f'<meta property="og:title" content="{e(titre)}">\n'
            f'<meta property="og:description" content="{e(description)}">\n'
            f'<meta property="og:url" content="{e(url)}">\n'
            f'<meta property="og:image" content="{e(OG_IMAGE)}">\n'
            '<meta name="twitter:card" content="summary_large_image">\n'
            '<!-- /seo:tete -->')


def maj_tete(contenu, titre, description, chemin):
    contenu = re.sub(r'<title>.*?</title>', f'<title>{e(titre)}</title>', contenu, count=1, flags=re.S)
    contenu = re.sub(r'<meta name="description" content="[^"]*">', f'<meta name="description" content="{e(description)}">', contenu, count=1)
    contenu = re.sub(r'\n?<!-- seo:tete -->.*?<!-- /seo:tete -->', '', contenu, flags=re.S)
    # anciennes balises posées à la main
    contenu = re.sub(r'\n<meta property="og:[^"]+" content="[^"]*">', '', contenu)
    contenu = re.sub(r'\n<link rel="canonical" href="[^"]*">', '', contenu)
    return re.sub(r'(<meta name="description" content="[^"]*">)', lambda m: m.group(1) + '\n' + bloc_tete(titre, description, chemin), contenu, count=1)


def jsonld(obj):
    return '<script type="application/ld+json">\n' + json.dumps(obj, ensure_ascii=False, indent=1) + '\n</script>'


def maj_jsonld(contenu, objets):
    bloc = '<!-- seo:jsonld -->\n' + '\n'.join(jsonld(o) for o in objets) + '\n<!-- /seo:jsonld -->'
    contenu = re.sub(r'\n?<!-- seo:jsonld -->.*?<!-- /seo:jsonld -->', '', contenu, flags=re.S)
    contenu = re.sub(r'\n?<script type="application/ld\+json">.*?</script>', '', contenu, flags=re.S)
    return contenu.replace('</head>', bloc + '\n</head>', 1)


metiers = {m['id']: m for m in competences['metiers']}
adresse = {'@type': 'PostalAddress', 'addressLocality': VILLE, 'postalCode': '59490', 'addressRegion': 'Hauts-de-France', 'addressCountry': 'FR'}
zone = [{'@type': 'City', 'name': VILLE}, {'@type': 'AdministrativeArea', 'name': 'Nord (59)'}]
entreprise = {
    '@context': 'https://schema.org',
    '@type': ['HomeAndConstructionBusiness', 'GeneralContractor'],
    '@id': ENTREPRISE_ID,
    'name': NOM, 'legalName': NOM, 'url': SITE,
    'logo': SITE + 'assets/brand/logo-officiel.webp', 'image': OG_IMAGE,
    'description': 'Entreprise familiale de rénovation de l’habitat tous corps d’état à Somain (Nord) : salle de bain, carrelage, placo, isolation, toiture, fenêtres, volets, portails, peinture, électricité, plomberie, pompe à chaleur et dépannage.',
    'slogan': 'Rénover, isoler, économiser',
    'telephone': TEL_INTL, 'email': EMAIL,
    'taxID': '99052688100018', 'vatID': 'FR11990526881', 'foundingDate': '2025-08-18',
    'address': adresse, 'areaServed': zone, 'sameAs': [FACEBOOK],
    'knowsAbout': ['Rénovation de salle de bain', 'Carrelage', 'Plâtrerie', 'Isolation', 'Aménagement de combles', 'Toiture', 'Fenêtres de toit', 'Menuiserie extérieure', 'Volets roulants', 'Portails', 'Clôtures', 'Peinture', 'Électricité', 'Plomberie', 'Pompe à chaleur', 'Dépannage'],
    'hasOfferCatalog': {'@type': 'OfferCatalog', 'name': 'Travaux de rénovation', 'itemListElement': [
        {'@type': 'Offer', 'itemOffered': {'@type': 'Service', 'name': SERVICES[i]['h1'], 'url': SITE + SERVICES[i]['slug']}} for i in SERVICES if i in metiers]},
}
site_web = {'@context': 'https://schema.org', '@type': 'WebSite', 'name': NOM, 'url': SITE, 'inLanguage': 'fr-FR', 'publisher': {'@id': ENTREPRISE_ID}}

ICO_FB = '<svg viewBox="0 0 24 24" aria-hidden="true" class="fb-ico"><path fill="currentColor" d="M22 12a10 10 0 1 0-11.56 9.88v-6.99H7.9V12h2.54V9.8c0-2.5 1.49-3.89 3.78-3.89 1.09 0 2.24.2 2.24.2v2.46h-1.26c-1.24 0-1.63.77-1.63 1.56V12h2.78l-.44 2.89h-2.34v6.99A10 10 0 0 0 22 12Z"/></svg>'
gabarit = lire('mentions-legales.html')
tete_gabarit = gabarit[:gabarit.index('<main>')]

for mid, s in SERVICES.items():
    m = metiers.get(mid)
    if not m:
        continue
    questions = [f for f in faq if f.get('metier') == mid and f.get('fait') != 'non']
    if mid == 'depannage':
        questions = [f for f in faq if f.get('metier') == 'depannage']
    questions += [f for f in faq if f['question'] in GENERAUX]
    vus, qs = set(), []
    for f in questions:
        if f['question'] not in vus:
            vus.add(f['question']); qs.append(f)
    autres = ''.join(f'<li><a href="{SERVICES[i]["slug"]}">{e(SERVICES[i]["h1"].replace(" à Somain", ""))}</a></li>' for i in SERVICES if i != mid and i in metiers)
    liens = ''.join(f'<li><a href="{e(u)}">{e(t)}</a></li>' for u, t in s['liens'])
    corps = f'''<main>
  <section class="page-hero"><div class="wrap">
    <nav class="crumbs" aria-label="Fil d’Ariane"><a href="index.html">Accueil</a> › <a href="savoir-faire.html">Savoir-faire</a> › <span>{e(m['nom'])}</span></nav>
    <h1>{e(s['h1'])}</h1>
    <p>{e(m['accroche'])} {e(m['texte'])}</p>
    <div class="hero-actions" style="margin-top:22px"><a class="btn sun" href="contact.html">Demander un devis gratuit <span class="arr">→</span></a><a class="btn ghost" href="tel:{TEL_INTL}">Appeler le {TEL}</a></div>
  </div></section>
  <section class="sec" style="padding-top:32px"><div class="wrap svc">
    <div class="svc-main">
      <div class="svc-tex tex-{e(m['texture'])}" aria-hidden="true"></div>
      <h2>Ce que LOGE&amp;NERJ fait pour vous</h2>
      <ul class="presta">{''.join(f'<li>{e(p)}</li>' for p in m['prestations'])}</ul>
      {('<div class="chips">' + ''.join(f'<span class="chip">{e(x)}</span>' for x in m['materiaux']) + '</div>') if m['materiaux'] else ''}
      {('<h2>Nos réalisations</h2><ul class="svc-links">' + liens + '</ul>') if liens else ''}
      <h2>Une seule entreprise pour tout le chantier</h2>
      <p>LOGE&amp;NERJ est une entreprise familiale de rénovation de l’habitat installée à {VILLE}, dans le Nord. Elle intervient à {VILLE} et dans les communes alentour, sur un seul poste ou sur une rénovation complète : salle de bain, isolation, toiture, fenêtres, peinture, électricité, plomberie, pompe à chaleur. Vous avez un seul interlocuteur, du devis à la fin des travaux.</p>
      <h2>Questions fréquentes</h2>
      <div class="faq">{''.join(f'<details><summary>{e(f["question"])}</summary><p>{f["reponse"]}</p></details>' for f in qs)}</div>
    </div>
    <aside class="svc-aside">
      <div class="card-info">
        <span class="mono">Devis gratuit</span>
        <a class="big" href="tel:{TEL_INTL}">{TEL}</a>
        <span class="mono">E-mail</span><a class="big" href="mailto:{EMAIL}" style="font-size:1.2rem">{EMAIL}</a>
        <span class="mono">Zone</span><p>{VILLE} et ses alentours, Nord (59)</p>
        <a class="btn sun" href="contact.html">Formulaire de devis <span class="arr">→</span></a>
      </div>
      <a class="btn fb-btn" href="{FACEBOOK}" target="_blank" rel="noopener" style="margin-top:14px">{ICO_FB} Nos chantiers sur Facebook</a>
      <h2 class="svc-autres-t">Nos autres métiers</h2>
      <ul class="svc-links">{autres}</ul>
    </aside>
  </div></section>
</main>
</body>
</html>
'''
    page = maj_tete(tete_gabarit, s['titre'], s['description'], s['slug']) + corps
    service = {'@context': 'https://schema.org', '@type': 'Service', 'name': s['h1'], 'serviceType': m['nom'],
               'description': s['description'], 'url': SITE + s['slug'], 'provider': {'@id': ENTREPRISE_ID, '@type': 'HomeAndConstructionBusiness', 'name': NOM, 'telephone': TEL_INTL, 'address': adresse},
               'areaServed': zone}
    fil = {'@context': 'https://schema.org', '@type': 'BreadcrumbList', 'itemListElement': [
        {'@type': 'ListItem', 'position': 1, 'name': 'Accueil', 'item': SITE},
        {'@type': 'ListItem', 'position': 2, 'name': 'Savoir-faire', 'item': SITE + 'savoir-faire.html'},
        {'@type': 'ListItem', 'position': 3, 'name': m['nom'], 'item': SITE + s['slug']}]}
    questions_ld = {'@context': 'https://schema.org', '@type': 'FAQPage', 'mainEntity': [
        {'@type': 'Question', 'name': f['question'], 'acceptedAnswer': {'@type': 'Answer', 'text': sans_balises(f['reponse'])}} for f in qs]}
    ecrire(s['slug'], maj_jsonld(page, [service, fil, questions_ld]))

# --- Pages existantes -----------------------------------------------------------------------------
for fichier, (titre, description, chemin) in PAGES.items():
    contenu = maj_tete(lire(fichier), titre, description, chemin)
    if fichier == 'index.html':
        contenu = maj_jsonld(contenu, [entreprise, site_web])
    if fichier == 'savoir-faire.html':
        liste = ('<!-- seo:metiers -->\n  <nav class="svc-index" aria-label="Nos métiers"><div class="wrap"><ul>'
                 + ''.join(f'<li><a href="{SERVICES[i]["slug"]}">{e(metiers[i]["nom"])}</a></li>' for i in SERVICES if i in metiers)
                 + '</ul></div></nav>\n  <!-- /seo:metiers -->')
        contenu = re.sub(r'\n?\s*<!-- seo:metiers -->.*?<!-- /seo:metiers -->', '', contenu, flags=re.S)
        contenu = contenu.replace('  <div id="metiers"></div>', liste + '\n  <div id="metiers"></div>', 1)
    ecrire(fichier, contenu)

# --- Plan du site ---------------------------------------------------------------------------------
urls = [('', '1.0'), ('savoir-faire.html', '0.9')] + [(SERVICES[i]['slug'], '0.9') for i in SERVICES if i in metiers] + [
    ('realisations.html', '0.8'), ('chantier.html?c=combles-briques', '0.6'), ('qui-suis-je.html', '0.6'), ('contact.html', '0.8'), ('mentions-legales.html', '0.2')]
plan = '<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n' + ''.join(
    f'  <url><loc>{e(SITE + u)}</loc><lastmod>{AUJOURDHUI}</lastmod><priority>{p}</priority></url>\n' for u, p in urls) + '</urlset>\n'
ecrire('sitemap.xml', plan)
print('Pages métier :', ', '.join(SERVICES[i]['slug'] for i in SERVICES if i in metiers))
