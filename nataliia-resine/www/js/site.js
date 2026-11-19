// Nataliia Résine : comportements communs à toutes les pages.

// Menu
(() => {
  const btn = document.querySelector('.menu-toggle');
  const menu = document.getElementById('site-menu');
  if (!btn || !menu) return;
  const close = () => { btn.setAttribute('aria-expanded', 'false'); menu.hidden = true; };
  btn.addEventListener('click', () => {
    const open = btn.getAttribute('aria-expanded') !== 'true';
    btn.setAttribute('aria-expanded', String(open));
    menu.hidden = !open;
    if (open) menu.querySelector('a')?.focus();
  });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && !menu.hidden) { close(); btn.focus(); } });
  document.addEventListener('click', (e) => { if (!menu.hidden && !menu.contains(e.target) && !btn.contains(e.target)) close(); });
})();

// Curseur avant / après
document.querySelectorAll('.compare').forEach((box) => {
  const input = box.querySelector('input');
  const set = (v) => box.style.setProperty('--pos', v + '%');
  input.addEventListener('input', () => set(input.value));
  set(input.value);
});

// Filtres des réalisations
(() => {
  const bar = document.querySelector('.filters');
  if (!bar) return;
  const items = document.querySelectorAll('[data-type]');
  bar.addEventListener('click', (e) => {
    const b = e.target.closest('.filter');
    if (!b) return;
    bar.querySelectorAll('.filter').forEach((x) => x.setAttribute('aria-pressed', String(x === b)));
    const t = b.dataset.filter;
    items.forEach((it) => { it.hidden = t !== 'tout' && it.dataset.type !== t; });
  });
})();

// Page « Créez votre projet » (version provisoire, avant le simulateur du lot 3)
(() => {
  const form = document.getElementById('project-form');
  if (!form) return;
  const input = document.getElementById('photo');
  const zone = document.getElementById('upload-zone');
  const preview = document.getElementById('photo-preview');
  const chosen = document.getElementById('chosen');
  let photoName = '';
  let granulat = '';

  input.addEventListener('change', () => {
    const file = input.files && input.files[0];
    if (!file) return;
    photoName = file.name;
    preview.src = URL.createObjectURL(file);
    zone.classList.add('has-photo');
  });

  const buttons = form.querySelectorAll('.swatch');
  const pick = (b) => {
    buttons.forEach((x) => x.setAttribute('aria-pressed', String(x === b)));
    granulat = b.dataset.name;
    chosen.textContent = granulat;
  };
  buttons.forEach((b) => b.addEventListener('click', () => pick(b)));
  const wanted = new URLSearchParams(location.search).get('granulat');
  const initial = [...buttons].find((b) => b.dataset.id === wanted) || buttons[0];
  if (initial) pick(initial);

  form.addEventListener('submit', (e) => {
    e.preventDefault();
    const v = (id) => (document.getElementById(id)?.value || '').trim();
    const lines = [
      'Bonjour Nataliia,',
      '',
      'Voici mon idée de projet.',
      'Nom : ' + (v('nom') || 'non précisé'),
      'Téléphone : ' + (v('tel') || 'non précisé'),
      'Ville : ' + (v('ville') || 'non précisée'),
      'Type de surface : ' + (v('type') || 'non précisé'),
      'Surface approximative : ' + (v('surface') ? v('surface') + ' m²' : 'non précisée'),
      'Granulat choisi : ' + (granulat || 'non choisi'),
      'Photo : ' + (photoName ? photoName + ' (je la joins à ce message)' : 'je joindrai une photo'),
      'Détails : ' + (v('message') || 'non précisés'),
      '',
      'Merci.'
    ];
    const to = form.dataset.email;
    location.href = 'mailto:' + to + '?subject=' + encodeURIComponent('Mon projet - Nataliia Résine') + '&body=' + encodeURIComponent(lines.join('\n'));
  });
})();
