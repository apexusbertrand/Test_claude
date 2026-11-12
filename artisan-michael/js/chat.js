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
      teaser();
      fermeture();
    });
  }

  // Croix de fermeture dans l'en-tête du chat + bouton rond qui devient une croix quand le chat est ouvert.
  // Le widget n8n se ferme en recliquant sur son bouton : la croix ne fait que ça.
  function fermeture() {
    const racine = document.getElementById('n8n-chat');
    const attendre = (essais) => {
      const toggle = racine && racine.querySelector('.chat-window-toggle');
      if (!toggle) { if (essais > 0) setTimeout(() => attendre(essais - 1), 300); return; }
      toggle.setAttribute('aria-label', 'Ouvrir le chat');
      const maj = () => {
        const fen = racine.querySelector('.chat-window');
        const ouvert = !!(fen && fen.offsetParent !== null && getComputedStyle(fen).display !== 'none');
        toggle.classList.toggle('is-open', ouvert);
        toggle.setAttribute('aria-label', ouvert ? 'Fermer le chat' : 'Ouvrir le chat');
        const entete = racine.querySelector('.chat-header');
        if (ouvert && entete && !entete.querySelector('.chat-close')) {
          entete.insertAdjacentHTML('beforeend', '<button type="button" class="chat-close" aria-label="Fermer le chat">×</button>');
          entete.querySelector('.chat-close').addEventListener('click', () => toggle.click());
        }
      };
      new MutationObserver(maj).observe(racine, { childList: true, subtree: true, attributes: true, attributeFilter: ['style', 'class'] });
      toggle.addEventListener('click', () => setTimeout(maj, 50));
      document.addEventListener('keydown', e => { if (e.key === 'Escape' && toggle.classList.contains('is-open')) toggle.click(); });
      maj();
    };
    attendre(20);
  }

  // Bulle bien visible au-dessus du bouton du chat, pour signaler qu'un assistant répond aux questions.
  // Elle disparaît dès que le chat est ouvert ou si le visiteur la ferme (pour la durée de la visite).
  function teaser() {
    let ferme = false;
    try { ferme = sessionStorage.getItem('logenerj-teaser') === 'ferme'; } catch (e) {}
    const attendre = (essais) => {
      const toggle = document.querySelector('#n8n-chat .chat-window-toggle');
      if (!toggle) { if (essais > 0) setTimeout(() => attendre(essais - 1), 300); return; }
      toggle.classList.add('chat-pulse');
      if (ferme) return;
      document.body.insertAdjacentHTML('beforeend', `
        <div class="chat-teaser" role="dialog" aria-label="Assistant en ligne">
          <button type="button" class="chat-teaser-open">
            <span class="ct-kicker"><span class="ct-dot" aria-hidden="true"></span>Assistant en ligne</span>
            <strong>Une question ? Posez-la ici</strong>
            <span class="ct-txt">Travaux, zone, dépannage, devis : réponse immédiate, 7 j / 7.</span>
          </button>
          <button type="button" class="chat-teaser-x" aria-label="Masquer ce message">×</button>
        </div>`);
      const bulle = document.querySelector('.chat-teaser');
      const cacher = (memoriser) => {
        bulle.classList.remove('show');
        setTimeout(() => bulle.remove(), 300);
        toggle.classList.remove('chat-pulse');
        if (memoriser) { try { sessionStorage.setItem('logenerj-teaser', 'ferme'); } catch (e) {} }
      };
      setTimeout(() => bulle.classList.add('show'), 1200);
      // la bulle s'efface quand le bas du pied de page est visible, pour ne pas cacher les mentions
      const bas = document.querySelector('.ftr-legal');
      if (bas && 'IntersectionObserver' in window) new IntersectionObserver(es => es.forEach(e => bulle.classList.toggle('ct-hidden', e.isIntersecting))).observe(bas);
      bulle.querySelector('.chat-teaser-open').addEventListener('click', () => { toggle.click(); cacher(true); });
      bulle.querySelector('.chat-teaser-x').addEventListener('click', () => cacher(true));
      toggle.addEventListener('click', () => cacher(true));
    };
    attendre(20);
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
