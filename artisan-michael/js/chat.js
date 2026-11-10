/* Chat du site.
   - CONFIG.chatWebhook vide : mode démo, réponses tirées de content/faq.json par mots-clés (aucun envoi).
   - CONFIG.chatWebhook renseigné : widget @n8n/chat, comme sur apexus-site (version figée). */
(function () {
  const N8N_CHAT_VERSION = '1.42.6';

  function n8nChat() {
    const css = document.createElement('link');
    css.rel = 'stylesheet';
    css.href = `https://cdn.jsdelivr.net/npm/@n8n/chat@${N8N_CHAT_VERSION}/dist/style.css`;
    document.head.appendChild(css);
    import(`https://cdn.jsdelivr.net/npm/@n8n/chat@${N8N_CHAT_VERSION}/dist/chat.bundle.es.js`).then(({ createChat }) => {
      createChat({
        webhookUrl: CONFIG.chatWebhook,
        mode: 'window',
        showWelcomeScreen: false,
        defaultLanguage: 'en',
        metadata: { page: location.pathname, langue: navigator.language, referent: document.referrer || 'direct' },
        initialMessages: [`Bonjour, je suis l’assistant de ${CONFIG.prenom}. Posez-moi vos questions sur ses travaux de carrelage, plâtrerie ou couverture.`],
        i18n: { en: { title: CONFIG.nom, subtitle: 'Ce que je fais, ce que je ne fais pas', footer: '', getStarted: 'Nouvelle conversation', inputPlaceholder: 'Votre question…', closeButtonTooltip: 'Fermer' } }
      });
    });
  }

  function demoChat() {
    let faq = [];
    getJSON('content/faq.json').then(d => { faq = d.faq; }).catch(() => {});

    document.body.insertAdjacentHTML('beforeend', `
      <button class="chat-fab" aria-controls="chat" aria-expanded="false">${LOGO}<span class="lbl">Une question ?</span><span class="dot" aria-hidden="true"></span></button>
      <section class="chat" id="chat" aria-label="Discuter avec l’assistant">
        <div class="chat-hd">${LOGO}<div><strong>L’assistant de ${CONFIG.prenom}</strong><span class="mono">Ce que je fais · ce que je ne fais pas</span></div><button class="chat-x" aria-label="Fermer le chat">×</button></div>
        <div class="chat-log" aria-live="polite"></div>
        <div class="quick">
          <button>Vous faites la couverture ?</button><button>Vous intervenez où ?</button><button>Le devis est payant ?</button><button>Vous faites l’électricité ?</button>
        </div>
        <form class="chat-in"><input type="text" placeholder="Votre question…" aria-label="Votre question" autocomplete="off"><button type="submit">Envoyer</button></form>
        <div class="chat-demo">Démo maquette · réponses automatiques, rien n’est envoyé</div>
      </section>`);

    const fab = document.querySelector('.chat-fab'), box = document.getElementById('chat');
    const log = box.querySelector('.chat-log'), form = box.querySelector('.chat-in'), input = form.querySelector('input');
    let greeted = false;

    const add = (html, who) => {
      const m = document.createElement('div');
      m.className = 'msg ' + who; m.innerHTML = html;
      log.appendChild(m); log.scrollTop = log.scrollHeight; return m;
    };
    const norm = s => s.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');

    function answer(q) {
      const nq = norm(q);
      let best = null, score = 0;
      faq.forEach(f => {
        const s = f.mots_cles.reduce((n, k) => n + (nq.includes(norm(k)) ? k.length : 0), 0);
        if (s > score) { score = s; best = f; }
      });
      if (best) return best.reponse + (best.fait !== 'non' ? ' <a href="contact.html">Demander un devis →</a>' : '');
      return `Bonne question, je n’ai pas encore la réponse. Le plus simple : appelez ${CONFIG.prenom} au <a href="tel:${CONFIG.telephone.replace(/\s/g, '')}">${CONFIG.telephone}</a> ou passez par le <a href="contact.html">formulaire</a>.`;
    }

    function ask(q) {
      if (!q.trim()) return;
      add(q.replace(/</g, '&lt;'), 'me');
      const t = add('écrit…', 'bot typing');
      setTimeout(() => { t.remove(); add(answer(q), 'bot'); }, 650);
    }

    function toggle(open) {
      box.classList.toggle('open', open);
      fab.style.display = open ? 'none' : '';
      fab.setAttribute('aria-expanded', open);
      if (open && !greeted) { greeted = true; add(`Bonjour, je suis l’assistant de ${CONFIG.prenom}. Carrelage, plâtrerie, un peu de couverture : demandez-moi ce qu’il fait, et ce qu’il ne fait pas.`, 'bot'); }
      if (open) input.focus();
    }

    fab.addEventListener('click', () => toggle(true));
    box.querySelector('.chat-x').addEventListener('click', () => toggle(false));
    box.querySelector('.quick').addEventListener('click', e => { if (e.target.tagName === 'BUTTON') ask(e.target.textContent); });
    form.addEventListener('submit', e => { e.preventDefault(); ask(input.value); input.value = ''; });
  }

  document.addEventListener('DOMContentLoaded', () => { CONFIG.chatWebhook ? n8nChat() : demoChat(); });
})();
