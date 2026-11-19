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
