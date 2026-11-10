const BASE = import.meta.env.VITE_API_BASE_URL || '';

export function getToken() {
  try { return localStorage.getItem('bb_token'); } catch { return null; }
}
export function getStoredUser() {
  try { return JSON.parse(localStorage.getItem('bb_user')); } catch { return null; }
}
export function storeSession(token, user) {
  try {
    localStorage.setItem('bb_token', token);
    localStorage.setItem('bb_user', JSON.stringify(user));
  } catch { /* storage unavailable */ }
}
export function clearSession() {
  try {
    localStorage.removeItem('bb_token');
    localStorage.removeItem('bb_user');
  } catch { /* storage unavailable */ }
}

export async function api(path, { method = 'GET', body } = {}) {
  const headers = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  let data = null;
  const text = await res.text();
  if (text) {
    try { data = JSON.parse(text); } catch { data = null; }
  }
  if (!res.ok) {
    if (res.status === 401 && token) {
      clearSession();
      window.dispatchEvent(new Event('bb-logout'));
    }
    throw new Error((data && (data.message || data.error)) || `Request failed (${res.status})`);
  }
  return data;
}

/** Downloads a protected file (for example a CSV report) using the signed-in user's token. */
export async function downloadFile(path, filename) {
  const token = getToken();
  const res = await fetch(`${BASE}${path}`, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  if (!res.ok) throw new Error(`Download failed (${res.status})`);
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}
