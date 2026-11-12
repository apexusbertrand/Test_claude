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
        initialMessages: [`Bonjour, je suis l’assistant de LOGE&NERJ. Rénovation tous corps d’état et dépannage autour de Somain. Posez-moi votre question, et laissez votre prénom, votre téléphone et votre commune : ${CONFIG.prenom} vous rappelle pour un devis gratuit.`],
        i18n: { en: { title: CONFIG.nom, subtitle: 'Rénovation tous corps d’état et dépannage', footer: '', getStarted: 'Nouvelle conversation', inputPlaceholder: 'Votre question…', closeButtonTooltip: 'Fermer' } }
      });
    });
  }

  function demoChat() {
    let faq = [];
    getJSON('content/faq.json').then(d => { faq = d.faq; }).catch(() => {});

    document.body.insertAdjacentHTML('beforeend', `
      <button class="chat-fab" aria-controls="chat" aria-expanded="false">${LOGO}<span class="lbl">Une question ?</span><span class="dot" aria-hidden="true"></span></button>
      <section class="chat" id="chat" aria-label="Discuter avec l’assistant">
        <div class="chat-hd">${LOGO}<div><strong>L’assistant de ${CONFIG.prenom}</strong><span class="mono">Tous corps d’état · dépannage</span></div><button class="chat-x" aria-label="Fermer le chat">×</button></div>
        <div class="chat-log" aria-live="polite"></div>
        <div class="quick">
          <button>Vous faites quoi exactement ?</button><button>Un dépannage plomberie ?</button><button>Vous posez des portails ?</button><button>Vous faites la toiture ?</button><button>Vous intervenez où ?</button>
        </div>
        <form class="chat-in"><input type="text" placeholder="Votre question…" aria-label="Votre question" autocomplete="off"><button type="submit">Envoyer</button></form>
        <div class="chat-demo">Réponses automatiques · pour une demande, utilisez le formulaire de contact</div>
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
        // un mot-clé compte s'il commence un mot de la question (« vous » ne déclenche plus « où ») ;
        // « priorite » permet à une question (le prix par exemple) de passer devant un métier cité
        const s = (f.priorite || 1) * f.mots_cles.reduce((n, k) => {
          const nk = norm(k).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
          return n + (new RegExp('(^|[^a-z0-9])' + nk).test(nq) ? k.length : 0);
        }, 0);
        if (s > score) { score = s; best = f; }
      });
      const telLink = CONFIG.telephone ? `<a href="tel:${CONFIG.telephone.replace(/\s/g, '')}">Appeler ${CONFIG.prenom} →</a>` : '<a href="contact.html">Être rappelé →</a>';
      if (best) return best.reponse + ' ' + (best.metier === 'depannage' ? telLink : best.fait !== 'non' ? '<a href="contact.html">Demander un devis →</a>' : '');
      return `Bonne question, je n’ai pas encore la réponse. Le plus simple : passez par le <a href="contact.html">formulaire</a> ou écrivez à <a href="mailto:${CONFIG.email}">${CONFIG.email}</a>.`;
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
      if (open && !greeted) { greeted = true; add(`Bonjour, je suis l’assistant de ${CONFIG.prenom}. Rénovation tous corps d’état (salle de bain, isolation, toiture, fenêtres, peinture…) et dépannage électricité, plomberie et fenêtres : posez-moi votre question.`, 'bot'); }
      if (open) input.focus();
    }

    fab.addEventListener('click', () => toggle(true));
    box.querySelector('.chat-x').addEventListener('click', () => toggle(false));
    box.querySelector('.quick').addEventListener('click', e => { if (e.target.tagName === 'BUTTON') ask(e.target.textContent); });
    form.addEventListener('submit', e => { e.preventDefault(); ask(input.value); input.value = ''; });
  }

  document.addEventListener('DOMContentLoaded', () => { CONFIG.chatWebhook ? n8nChat() : demoChat(); });
})();
