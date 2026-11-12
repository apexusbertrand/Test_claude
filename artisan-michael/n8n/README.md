# Flux n8n du site de Michael

Adresse de réception unique : **logenerj@artisanat1.fr**. Les demandes du formulaire et les conversations du chat arrivent toutes sur cette boîte (décision du 13/11/2026).

Rien n'est déployé. Le site fonctionne sans n8n tant que `CONFIG.formWebhook` et `CONFIG.chatWebhook` sont vides dans `js/site.js`.

## 1. Formulaire de devis → Gmail

Webhook POST (JSON), réponse `{"message": "..."}` affichée au client.

```json
{
  "source": "formulaire-site",
  "page": "/artisan-michael/contact.html",
  "prenom": "Julie", "nom": "Martin",
  "telephone": "06 …", "email": "julie@…",
  "commune": "Roubaix",
  "travaux": ["carrelage", "platrerie"],
  "surface": "8 m²",
  "delai": "1 à 3 mois",
  "description": "Refaire la salle de bain…",
  "consentement": true,
  "date": "2026-11-05T21:00:00.000Z"
}
```

Nœuds : Webhook → (filtre : `consentement` vrai, `telephone` non vide) → Gmail vers Michael (logenerj@artisanat1.fr) (objet : `Devis · {{commune}} · {{travaux}}`) → Gmail d'accusé de réception au client si `email` renseigné → Respond to Webhook. Penser à autoriser l'origine du site (CORS) sur le webhook.

## 2. Chat → agent IA

Webhook du nœud Chat Trigger (mode « Embedded chat »), URL à mettre dans `CONFIG.chatWebhook`. Le widget envoie `chatInput`, `sessionId` et `metadata` (page, langue, référent).

L'agent lit sa connaissance depuis le site publié, une seule source de vérité :

- `content/competences.json` : métiers, prestations, ce qu'il ne fait pas
- `content/faq.json` : réponses types (`fait` = oui / non / sur devis)

Nœuds : Chat Trigger → HTTP Request (les deux JSON, mis en cache) → AI Agent avec le prompt de `prompt-agent-chat.md` + mémoire de session → réponse.

### Envoi des conversations à Michael (logenerj@artisanat1.fr)

Un mail par message serait illisible. On envoie donc :

1. **Tout de suite**, quand le visiteur a un vrai projet ou laisse ses coordonnées : l'agent appelle l'outil `transmettre_a_michael` (sous-flux n8n → Gmail). Objet : `Chat site · {{commune ou "commune inconnue"}} · {{sujet}}`. Contenu : résumé du besoin en 3 lignes, coordonnées si données, puis la conversation complète.
2. **Chaque soir à 18 h**, un récapitulatif de toutes les conversations de la journée (même sans coordonnées), pour que Michael voie ce que les visiteurs demandent. Chaque échange est enregistré au fil de l'eau dans une Data table n8n `chat_logenerj` (`sessionId`, `date`, `role`, `texte`, `page`) ; un Schedule Trigger la lit, regroupe par `sessionId` et envoie un seul mail. Pas de mail si aucune conversation dans la journée.

Les deux mails partent du compte Gmail connecté à n8n, avec en « Répondre à » l'e-mail du visiteur quand il l'a donné.
