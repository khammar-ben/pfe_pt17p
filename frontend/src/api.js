const API_BASE = import.meta.env.VITE_API_BASE ?? '';

let authToken = localStorage.getItem('pt17_token');

export function setAuthToken(token) {
  authToken = token;
  if (token) {
    localStorage.setItem('pt17_token', token);
  } else {
    localStorage.removeItem('pt17_token');
  }
}

export function getAuthToken() {
  return authToken;
}

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
      ...options.headers,
    },
    ...options,
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `HTTP ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

async function download(path, filename) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
    },
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `HTTP ${response.status}`);
  }

  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

async function upload(path, file) {
  const formData = new FormData();
  formData.append('file', file);
  const response = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: {
      ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
    },
    body: formData,
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `HTTP ${response.status}`);
  }

  return response.json();
}

async function imageUrl(path) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
    },
  });

  if (!response.ok) {
    return null;
  }

  return URL.createObjectURL(await response.blob());
}

export const api = {
  dashboard: () => request('/api/dashboard'),
  equipements: () => request('/api/equipements'),
  pannes: () => request('/api/pannes'),
  pieces: () => request('/api/stock/pieces'),
  prets: () => request('/api/prets'),
  reparations: () => request('/api/reparations'),
  utilisateurs: () => request('/api/utilisateurs'),
  fournisseurs: () => request('/api/fournisseurs'),
  auditLogs: () => request('/api/audit-logs'),
  login: (payload) => request('/api/auth/login', { method: 'POST', body: JSON.stringify(payload) }),
  createEquipement: (payload) => request('/api/equipements', { method: 'POST', body: JSON.stringify(payload) }),
  uploadEquipementPhoto: (id, file) => upload(`/api/equipements/${id}/photo`, file),
  equipementPhotoUrl: (id) => imageUrl(`/api/equipements/${id}/photo`),
  createPanne: (payload) => request('/api/pannes', { method: 'POST', body: JSON.stringify(payload) }),
  uploadPannePhoto: (id, file) => upload(`/api/pannes/${id}/photo`, file),
  pannePhotoUrl: (id) => imageUrl(`/api/pannes/${id}/photo`),
  affecterPanne: (panneId, technicienId) => request(`/api/pannes/${panneId}/affecter/${technicienId}`, { method: 'PUT' }),
  changerStatutPanne: (panneId, statut) => request(`/api/pannes/${panneId}/statut/${statut}`, { method: 'PUT' }),
  createPiece: (payload) => request('/api/stock/pieces', { method: 'POST', body: JSON.stringify(payload) }),
  mouvementStock: (payload) => request('/api/stock/mouvements', { method: 'POST', body: JSON.stringify(payload) }),
  createReparation: (payload) => request('/api/reparations', { method: 'POST', body: JSON.stringify(payload) }),
  cloturerReparation: (id, payload) => request(`/api/reparations/${id}/cloturer`, { method: 'PUT', body: JSON.stringify(payload) }),
  createPret: (payload) => request('/api/prets', { method: 'POST', body: JSON.stringify(payload) }),
  validerPret: (id) => request(`/api/prets/${id}/valider`, { method: 'PUT' }),
  refuserPret: (id) => request(`/api/prets/${id}/refuser`, { method: 'PUT' }),
  cloturerPret: (id) => request(`/api/prets/${id}/cloturer`, { method: 'PUT' }),
  createUtilisateur: (payload) => request('/api/utilisateurs', { method: 'POST', body: JSON.stringify(payload) }),
  desactiverUtilisateur: (id) => request(`/api/utilisateurs/${id}/desactiver`, { method: 'PUT' }),
  reactiverUtilisateur: (id) => request(`/api/utilisateurs/${id}/reactiver`, { method: 'PUT' }),
  resetPasswordUtilisateur: (id, motDePasse) => request(`/api/utilisateurs/${id}/mot-de-passe`, { method: 'PUT', body: JSON.stringify({ motDePasse }) }),
  createFournisseur: (payload) => request('/api/fournisseurs', { method: 'POST', body: JSON.stringify(payload) }),
  runNotifications: () => request('/api/notifications/run/all', { method: 'POST' }),
  downloadReport: (name) => download(`/api/reports/${name}.csv`, `${name}.csv`),
};
