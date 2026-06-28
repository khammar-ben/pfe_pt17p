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
    const raw = await response.text();
    let message = raw;
    try {
      message = JSON.parse(raw).error ?? raw;
    } catch {
      // Keep plain-text API errors unchanged.
    }
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
  equipementHistorique: (id) => request(`/api/equipements/${id}/historique`),
  employes: () => request('/api/employes'),
  pannes: () => request('/api/pannes'),
  pieces: () => request('/api/stock/pieces'),
  prets: () => request('/api/prets'),
  reparations: () => request('/api/reparations'),
  utilisateurs: () => request('/api/utilisateurs'),
  fournisseurs: () => request('/api/fournisseurs'),
  auditLogs: () => request('/api/audit-logs'),
  appNotifications: () => request('/api/app-notifications'),
  chatbot: (message) => request('/api/chatbot', { method: 'POST', body: JSON.stringify({ message }) }),
  login: (payload) => request('/api/auth/login', { method: 'POST', body: JSON.stringify(payload) }),
  me: () => request('/api/auth/me'),
  createEquipement: (payload) => request('/api/equipements', { method: 'POST', body: JSON.stringify(payload) }),
  affecterEquipement: (payload) => request('/api/equipements/affecter', { method: 'PUT', body: JSON.stringify(payload) }),
  affecterPieceEquipement: (payload) => request('/api/equipements/affecter-piece', { method: 'PUT', body: JSON.stringify(payload) }),
  affecterPieceDirectement: (payload) => request('/api/equipements/affecter-piece-directe', { method: 'PUT', body: JSON.stringify(payload) }),
  affecterPack: (payload) => request('/api/equipements/affecter-pack', { method: 'PUT', body: JSON.stringify(payload) }),
  demanderEquipement: (payload) => request('/api/equipements/demandes', { method: 'POST', body: JSON.stringify(payload) }),
  uploadEquipementPhoto: (id, file) => upload(`/api/equipements/${id}/photo`, file),
  equipementPhotoUrl: (id) => imageUrl(`/api/equipements/${id}/photo`),
  createPanne: (payload) => request('/api/pannes', { method: 'POST', body: JSON.stringify(payload) }),
  typesEquipementPanne: () => request('/api/pannes/types-equipement'),
  piecesStockParPrefixe: (prefix) => request(`/api/pannes/pieces-stock?prefix=${encodeURIComponent(prefix)}`),
  uploadPannePhoto: (id, file) => upload(`/api/pannes/${id}/photo`, file),
  pannePhotoUrl: (id) => imageUrl(`/api/pannes/${id}/photo`),
  affecterPanne: (panneId, technicienId) => request(`/api/pannes/${panneId}/affecter/${technicienId}`, { method: 'PUT' }),
  publierPanne: (panneId) => request(`/api/pannes/${panneId}/publier`, { method: 'PUT' }),
  claimPanne: (panneId) => request(`/api/pannes/${panneId}/claim`, { method: 'PUT' }),
  changerStatutPanne: (panneId, statut) => request(`/api/pannes/${panneId}/statut/${statut}`, { method: 'PUT' }),
  createPiece: (payload) => request('/api/stock/pieces', { method: 'POST', body: JSON.stringify(payload) }),
  mouvementStock: (payload) => request('/api/stock/mouvements', { method: 'POST', body: JSON.stringify(payload) }),
  createReparation: (payload) => request('/api/reparations', { method: 'POST', body: JSON.stringify(payload) }),
  cloturerReparation: (id, payload) => request(`/api/reparations/${id}/cloturer`, { method: 'PUT', body: JSON.stringify(payload) }),
  executerReparation: (id, payload) => request(`/api/reparations/${id}/executer`, { method: 'PUT', body: JSON.stringify(payload) }),
  createPret: (payload) => request('/api/prets', { method: 'POST', body: JSON.stringify(payload) }),
  validerPret: (id) => request(`/api/prets/${id}/valider`, { method: 'PUT' }),
  refuserPret: (id) => request(`/api/prets/${id}/refuser`, { method: 'PUT' }),
  cloturerPret: (id) => request(`/api/prets/${id}/cloturer`, { method: 'PUT' }),
  prolongerPret: (id, dateRetourPrevue) => request(`/api/prets/${id}/prolonger`, { method: 'PUT', body: JSON.stringify({ dateRetourPrevue }) }),
  createUtilisateur: (payload) => request('/api/utilisateurs', { method: 'POST', body: JSON.stringify(payload) }),
  desactiverUtilisateur: (id) => request(`/api/utilisateurs/${id}/desactiver`, { method: 'PUT' }),
  reactiverUtilisateur: (id) => request(`/api/utilisateurs/${id}/reactiver`, { method: 'PUT' }),
  resetPasswordUtilisateur: (id) => request(`/api/utilisateurs/${id}/mot-de-passe`, { method: 'PUT' }),
  createFournisseur: (payload) => request('/api/fournisseurs', { method: 'POST', body: JSON.stringify(payload) }),
  runNotifications: () => request('/api/notifications/run/all', { method: 'POST' }),
  marquerNotificationLue: (id) => request(`/api/app-notifications/${id}/lue`, { method: 'PUT' }),
  affecterNotificationEquipement: (id) => request(`/api/app-notifications/${id}/affecter`, { method: 'PUT' }),
  terminerNotificationEquipement: (id) => request(`/api/app-notifications/${id}/done`, { method: 'PUT' }),
  downloadReport: (name, format = 'csv') => download(`/api/reports/${name}.${format}`, `${name}.${format}`),
  downloadReparationReport: (id) => download(`/api/reparations/${id}/rapport.pdf`, `reparation-${id}.pdf`),
};
