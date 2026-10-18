// Remplace le service worker de l'ancienne app PhotoTag, publiée à la même adresse.
// Il vide les caches, se désinstalle et recharge les onglets ouverts. Le site n'en utilise pas.
self.addEventListener('install', () => self.skipWaiting());

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(keys.map((k) => caches.delete(k)));
    await self.registration.unregister();
    const clients = await self.clients.matchAll({ type: 'window' });
    clients.forEach((c) => c.navigate(c.url));
  })());
});
