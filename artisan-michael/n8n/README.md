# Flux n8n du site de Michael

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

Nœuds : Webhook → (filtre : `consentement` vrai, `telephone` non vide) → Gmail vers Michael (objet : `Devis · {{commune}} · {{travaux}}`) → Gmail d'accusé de réception au client si `email` renseigné → Respond to Webhook. Penser à autoriser l'origine du site (CORS) sur le webhook.

## 2. Chat → agent IA

Webhook du nœud Chat Trigger (mode « Embedded chat »), URL à mettre dans `CONFIG.chatWebhook`. Le widget envoie `chatInput`, `sessionId` et `metadata` (page, langue, référent).

L'agent lit sa connaissance depuis le site publié, une seule source de vérité :

- `content/competences.json` : métiers, prestations, ce qu'il ne fait pas
- `content/faq.json` : réponses types (`fait` = oui / non / sur devis)

Nœuds : Chat Trigger → HTTP Request (les deux JSON, mis en cache) → AI Agent avec le prompt de `prompt-agent-chat.md` + mémoire de session → réponse. Plus tard : une table n8n ou un document pour la base de connaissances enrichie, et un outil « transmettre à Michael » qui envoie un mail quand un visiteur laisse ses coordonnées dans le chat.
