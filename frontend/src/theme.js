export function initTheme() {
  let theme = null;
  try { theme = localStorage.getItem('bb_theme'); } catch { /* storage unavailable */ }
  if (!theme) {
    theme = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
  document.documentElement.setAttribute('data-theme', theme);
  return theme;
}

export function setTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  try { localStorage.setItem('bb_theme', theme); } catch { /* storage unavailable */ }
}

export function currentTheme() {
  return document.documentElement.getAttribute('data-theme') || 'light';
}
