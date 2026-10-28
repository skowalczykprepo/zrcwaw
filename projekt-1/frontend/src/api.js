// Cienka warstwa nad fetch - wszystkie wywolania backendu w jednym miejscu.
export const API_BASE_URL = (window.APP_CONFIG && window.APP_CONFIG.API_BASE_URL) || 'http://localhost:8080';

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, options);
  if (!response.ok) {
    const text = await response.text().catch(() => '');
    throw new Error(`${response.status} ${response.statusText} ${text}`.trim());
  }
  return response.status === 204 ? null : response.json();
}

const json = (body) => ({
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body)
});

export const api = {
  // GET /api/messages - wszystkie wiadomosci
  getAllMessages: () => request('/api/messages'),

  // GET /api/messages/new?after=... - tylko nowsze niz podany znacznik czasu
  getNewMessages: (after) => request(`/api/messages/new?after=${encodeURIComponent(after)}`),

  // POST /api/messages - nowa wiadomosc
  sendMessage: (username, content, fileId) => request('/api/messages', json({ username, content, fileId })),

  // POST /api/messages/{id}/like - polubienie
  likeMessage: (id) => request(`/api/messages/${id}/like`, { method: 'POST' }),

  // POST /api/files - wyslanie pliku (multipart)
  uploadFile: (file) => {
    const form = new FormData();
    form.append('file', file);
    return request('/api/files', { method: 'POST', body: form });
  },

  // GET /api/files/{id} - adres do pobrania pliku
  fileUrl: (id) => `${API_BASE_URL}/api/files/${id}`
};
