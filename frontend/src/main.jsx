import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { api, getAuthToken, setAuthToken } from './api.js';
import './styles.css';

const modules = [
  { id: 'dashboard', label: 'Dashboard' },
  { id: 'equipements', label: 'Equipements' },
  { id: 'pannes', label: 'Pannes' },
  { id: 'reparations', label: 'Reparations' },
  { id: 'stock', label: 'Stock' },
  { id: 'prets', label: 'Prets' },
  { id: 'utilisateurs', label: 'Utilisateurs' },
  { id: 'fournisseurs', label: 'Fournisseurs' },
  { id: 'rapports', label: 'Rapports' },
  { id: 'audit', label: 'Audit' },
];

function App() {
  const [active, setActive] = useState('dashboard');
  const [data, setData] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [session, setSession] = useState(() => {
    const stored = localStorage.getItem('pt17_session');
    return stored ? JSON.parse(stored) : null;
  });

  useEffect(() => {
    if (!getAuthToken()) {
      setLoading(false);
      return;
    }
    let ignore = false;
    async function load() {
      await loadData(ignore);
    }
    load();
    return () => {
      ignore = true;
    };
  }, [session]);

  async function loadData(ignore = false) {
      setLoading(true);
      setError('');
      try {
        const result = await Promise.allSettled([
          api.dashboard(),
          api.equipements(),
          api.pannes(),
          api.reparations(),
          api.pieces(),
          api.prets(),
          api.utilisateurs(),
          api.fournisseurs(),
          api.auditLogs(),
        ]);
        if (!ignore) {
          const value = (index, fallback) => result[index].status === 'fulfilled' ? result[index].value : fallback;
          setData({
            dashboard: value(0, {
              totalEquipements: 0,
              pannesEnCours: 0,
              stockCritique: 0,
              reparations: 0,
              pretsEnRetard: 0,
              tauxDisponibilite: 0,
            }),
            equipements: value(1, []),
            pannes: value(2, []),
            reparations: value(3, []),
            stock: value(4, []),
            prets: value(5, []),
            utilisateurs: value(6, []),
            fournisseurs: value(7, []),
            auditLogs: value(8, []),
          });
        }
      } catch (err) {
        if (!ignore) {
          setError(err.message);
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
  }

  async function runAction(action) {
    setError('');
    try {
      await action();
      await loadData(false);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleLogin(credentials) {
    const result = await api.login(credentials);
    setAuthToken(result.token);
    const nextSession = {
      login: result.login,
      role: result.role,
      userId: result.userId,
    };
    localStorage.setItem('pt17_session', JSON.stringify(nextSession));
    setSession(nextSession);
  }

  function logout() {
    setAuthToken(null);
    localStorage.removeItem('pt17_session');
    setSession(null);
    setData({});
  }

  if (!session) {
    return <LoginScreen onLogin={handleLogin} />;
  }

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <strong>PT17</strong>
          <span>Parc informatique</span>
        </div>
        <nav>
          {modules.map((module) => (
            <button
              key={module.id}
              className={active === module.id ? 'active' : ''}
              onClick={() => setActive(module.id)}
            >
              {module.label}
            </button>
          ))}
        </nav>
      </aside>

      <main>
        <header className="hero">
          <div className="session">
            <p>Application React + Spring Boot</p>
            <button onClick={logout}>Logout {session.login}</button>
          </div>
          <h1>{modules.find((module) => module.id === active)?.label}</h1>
        </header>

        {loading && <div className="panel">Chargement...</div>}
        {error && <div className="panel error">Backend indisponible: {error}</div>}
        {!loading && !error && <ModuleView active={active} data={data} onAction={runAction} />}
      </main>
    </div>
  );
}

function LoginScreen({ onLogin }) {
  const [login, setLogin] = useState('admin');
  const [motDePasse, setMotDePasse] = useState('admin');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setError('');
    setLoading(true);
    try {
      await onLogin({ login, motDePasse });
    } catch (err) {
      setError('Login ou mot de passe invalide');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="login-page">
      <form className="login-card" onSubmit={submit}>
        <span>PT17</span>
        <h1>Connexion</h1>
        <label>
          Login
          <input value={login} onChange={(event) => setLogin(event.target.value)} />
        </label>
        <label>
          Mot de passe
          <input type="password" value={motDePasse} onChange={(event) => setMotDePasse(event.target.value)} />
        </label>
        {error && <div className="form-error">{error}</div>}
        <button disabled={loading}>{loading ? 'Connexion...' : 'Se connecter'}</button>
      </form>
    </main>
  );
}

function ModuleView({ active, data, onAction }) {
  if (active === 'dashboard') {
    return <Dashboard data={data.dashboard} />;
  }
  if (active === 'equipements') {
    return <Equipements rows={data.equipements} onAction={onAction} />;
  }
  if (active === 'pannes') {
    return <Pannes rows={data.pannes} equipements={data.equipements} utilisateurs={data.utilisateurs} onAction={onAction} />;
  }
  if (active === 'reparations') {
    return <Reparations rows={data.reparations} pannes={data.pannes} pieces={data.stock} onAction={onAction} />;
  }
  if (active === 'stock') {
    return <Stock rows={data.stock} fournisseurs={data.fournisseurs} onAction={onAction} />;
  }
  if (active === 'prets') {
    return <Prets rows={data.prets} equipements={data.equipements} utilisateurs={data.utilisateurs} onAction={onAction} />;
  }
  if (active === 'utilisateurs') {
    return <Utilisateurs rows={data.utilisateurs} onAction={onAction} />;
  }
  if (active === 'fournisseurs') {
    return <Fournisseurs rows={data.fournisseurs} onAction={onAction} />;
  }
  if (active === 'rapports') {
    return <Rapports onAction={onAction} />;
  }
  return <Audit rows={data.auditLogs} onAction={onAction} />;
}

function Dashboard({ data }) {
  const cards = [
    ['Equipements', data.totalEquipements],
    ['Pannes en cours', data.pannesEnCours],
    ['Stock critique', data.stockCritique],
    ['Reparations', data.reparations],
    ['Prets en retard', data.pretsEnRetard],
    ['Disponibilite', `${data.tauxDisponibilite}%`],
    ['Cout reparations', `${data.coutTotalReparations ?? 0} DH`],
  ];

  return (
    <section className="stack">
      <section className="kpis">
        {cards.map(([label, value]) => (
          <article className="card" key={label}>
            <span>{label}</span>
            <strong>{value}</strong>
          </article>
        ))}
      </section>

      <section className="dashboard-grid">
        <BarChart title="Pannes par statut" data={objectToChartData(data.pannesParStatut)} />
        <BarChart title="Pannes par mois" data={objectToChartData(data.pannesParMois)} />
        <BarChart title="Reparations par technicien" data={objectToChartData(data.reparationsParTechnicien)} />
        <BarChart title="Top equipements defaillants" data={data.topEquipementsDefaillants ?? []} />
      </section>

      <section className="panel">
        <div className="section-title">
          <h2>Pieces en stock critique</h2>
          <span>{data.piecesStockCritique?.length ?? 0} alertes</span>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Reference</th><th>Designation</th><th>Stock</th><th>Seuil</th></tr>
            </thead>
            <tbody>
              {(data.piecesStockCritique ?? []).map((piece) => (
                <tr key={piece.reference}>
                  <td>{piece.reference}</td>
                  <td>{piece.designation}</td>
                  <td>{piece.quantiteStock}</td>
                  <td>{piece.seuilMinimum}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}

function objectToChartData(data = {}) {
  return Object.entries(data).map(([label, value]) => ({ label, value }));
}

function BarChart({ title, data = [] }) {
  const max = Math.max(1, ...data.map((item) => Number(item.value)));
  return (
    <section className="panel chart-card">
      <div className="section-title">
        <h2>{title}</h2>
        <span>{data.length} items</span>
      </div>
      <div className="bars">
        {data.length === 0 && <div className="empty-chart">Aucune donnee</div>}
        {data.map((item) => (
          <div className="bar-row" key={item.label}>
            <span>{item.label}</span>
            <div className="bar-track">
              <div className="bar-fill" style={{ width: `${(Number(item.value) / max) * 100}%` }} />
            </div>
            <strong>{item.value}</strong>
          </div>
        ))}
      </div>
    </section>
  );
}

function Equipements({ rows = [], onAction }) {
  const [form, setForm] = useState({ numSerie: '', type: '', marque: '', modele: '', statut: 'DISPONIBLE' });

  function submit(event) {
    event.preventDefault();
    onAction(async () => {
      await api.createEquipement(form);
      setForm({ numSerie: '', type: '', marque: '', modele: '', statut: 'DISPONIBLE' });
    });
  }

  return (
    <section className="stack">
      <form className="form-grid panel" onSubmit={submit}>
        <Field label="Numero serie" value={form.numSerie} onChange={(numSerie) => setForm({ ...form, numSerie })} required />
        <Field label="Type" value={form.type} onChange={(type) => setForm({ ...form, type })} required />
        <Field label="Marque" value={form.marque} onChange={(marque) => setForm({ ...form, marque })} />
        <Field label="Modele" value={form.modele} onChange={(modele) => setForm({ ...form, modele })} />
        <Select label="Statut" value={form.statut} onChange={(statut) => setForm({ ...form, statut })} options={['DISPONIBLE', 'AFFECTE', 'EN_PRET', 'EN_PANNE', 'REFORME']} />
        <button>Ajouter equipement</button>
      </form>
      <section className="panel">
        <div className="section-title">
          <h2>Inventaire</h2>
          <span>{rows.length} lignes</span>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Numero serie</th><th>Type</th><th>Marque</th><th>Modele</th><th>Statut</th><th>Photo</th><th>Upload</th></tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td>{row.numSerie}</td>
                  <td>{row.type}</td>
                  <td>{formatValue(row.marque)}</td>
                  <td>{formatValue(row.modele)}</td>
                  <td>{row.statut}</td>
                  <td><PhotoPreview id={row.id} photoPath={row.photoPath} loader={api.equipementPhotoUrl} /></td>
                  <td><FileUpload onSelect={(file) => onAction(() => api.uploadEquipementPhoto(row.id, file))} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}

function Pannes({ rows = [], equipements = [], utilisateurs = [], onAction }) {
  const [form, setForm] = useState({ equipementId: '', declarantId: '', description: '', urgence: 'MOYENNE' });
  const techniciens = utilisateurs.filter((user) => user.role === 'TECHNICIEN');

  function submit(event) {
    event.preventDefault();
    onAction(async () => {
      await api.createPanne({
        ...form,
        equipementId: Number(form.equipementId),
        declarantId: form.declarantId ? Number(form.declarantId) : null,
      });
      setForm({ equipementId: '', declarantId: '', description: '', urgence: 'MOYENNE' });
    });
  }

  return (
    <section className="stack">
      <form className="form-grid panel" onSubmit={submit}>
        <Select label="Equipement" value={form.equipementId} onChange={(equipementId) => setForm({ ...form, equipementId })} options={equipements.map((item) => [item.id, `${item.numSerie} - ${item.type}`])} required />
        <Select label="Declarant" value={form.declarantId} onChange={(declarantId) => setForm({ ...form, declarantId })} options={utilisateurs.map((item) => [item.id, `${item.login} (${item.role})`])} />
        <Select label="Urgence" value={form.urgence} onChange={(urgence) => setForm({ ...form, urgence })} options={['HAUTE', 'MOYENNE', 'FAIBLE']} />
        <Field label="Description" value={form.description} onChange={(description) => setForm({ ...form, description })} required />
        <button>Declarer panne</button>
      </form>
      <section className="panel">
        <div className="section-title">
          <h2>Workflow des pannes</h2>
          <span>{rows.length} lignes</span>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>ID</th><th>Description</th><th>Urgence</th><th>Statut</th><th>Technicien</th><th>Photo</th><th>Actions</th></tr>
            </thead>
            <tbody>
              {rows.map((panne) => (
                <tr key={panne.id}>
                  <td>{panne.id}</td>
                  <td>{panne.description}</td>
                  <td>{panne.urgence}</td>
                  <td>{panne.statut}</td>
                  <td>{formatValue(panne.technicien)}</td>
                  <td>
                    <div className="photo-cell">
                      <PhotoPreview id={panne.id} photoPath={panne.photoPath} loader={api.pannePhotoUrl} compact />
                      <FileUpload onSelect={(file) => onAction(() => api.uploadPannePhoto(panne.id, file))} />
                    </div>
                  </td>
                  <td className="row-actions">
                    {techniciens.map((tech) => (
                      <button key={tech.id} onClick={() => onAction(() => api.affecterPanne(panne.id, tech.id))}>Affecter {tech.login}</button>
                    ))}
                    {['EN_COURS', 'EN_ATTENTE_PIECE', 'REPAREE', 'CLOTUREE'].map((statut) => (
                      <button key={statut} onClick={() => onAction(() => api.changerStatutPanne(panne.id, statut))}>{statut}</button>
                    ))}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}

function Stock({ rows = [], fournisseurs = [], onAction }) {
  const [piece, setPiece] = useState({ reference: '', designation: '', quantiteStock: 0, seuilMinimum: 0, localisation: '', prixUnitaire: 0, fournisseur: null });
  const [mouvement, setMouvement] = useState({ pieceId: '', quantite: 1, typeMouvement: 'ENTREE', motif: 'Ajustement stock' });

  function submitPiece(event) {
    event.preventDefault();
    onAction(async () => {
      await api.createPiece(piece);
      setPiece({ reference: '', designation: '', quantiteStock: 0, seuilMinimum: 0, localisation: '', prixUnitaire: 0, fournisseur: null });
    });
  }

  function submitMouvement(event) {
    event.preventDefault();
    onAction(() => api.mouvementStock({ ...mouvement, pieceId: Number(mouvement.pieceId), quantite: Number(mouvement.quantite) }));
  }

  return (
    <section className="stack">
      <form className="form-grid panel" onSubmit={submitPiece}>
        <Field label="Reference" value={piece.reference} onChange={(reference) => setPiece({ ...piece, reference })} required />
        <Field label="Designation" value={piece.designation} onChange={(designation) => setPiece({ ...piece, designation })} required />
        <Field label="Quantite" type="number" value={piece.quantiteStock} onChange={(quantiteStock) => setPiece({ ...piece, quantiteStock: Number(quantiteStock) })} />
        <Field label="Seuil" type="number" value={piece.seuilMinimum} onChange={(seuilMinimum) => setPiece({ ...piece, seuilMinimum: Number(seuilMinimum) })} />
        <Field label="Localisation" value={piece.localisation} onChange={(localisation) => setPiece({ ...piece, localisation })} />
        <Field label="Prix" type="number" value={piece.prixUnitaire} onChange={(prixUnitaire) => setPiece({ ...piece, prixUnitaire: Number(prixUnitaire) })} />
        <Select label="Fournisseur" value={piece.fournisseur?.id ?? ''} onChange={(id) => setPiece({ ...piece, fournisseur: id ? { id: Number(id) } : null })} options={fournisseurs.map((item) => [item.id, item.nom])} />
        <button>Ajouter piece</button>
      </form>
      <form className="form-grid panel" onSubmit={submitMouvement}>
        <Select label="Piece" value={mouvement.pieceId} onChange={(pieceId) => setMouvement({ ...mouvement, pieceId })} options={rows.map((item) => [item.id, `${item.reference} - ${item.designation}`])} required />
        <Select label="Type" value={mouvement.typeMouvement} onChange={(typeMouvement) => setMouvement({ ...mouvement, typeMouvement })} options={['ENTREE', 'SORTIE', 'CONSOMMATION']} />
        <Field label="Quantite" type="number" value={mouvement.quantite} onChange={(quantite) => setMouvement({ ...mouvement, quantite })} required />
        <Field label="Motif" value={mouvement.motif} onChange={(motif) => setMouvement({ ...mouvement, motif })} />
        <button>Enregistrer mouvement</button>
      </form>
      <Table title="Pieces de rechange" rows={rows} columns={['reference', 'designation', 'quantiteStock', 'seuilMinimum', 'localisation']} />
    </section>
  );
}

function Reparations({ rows = [], pannes = [], pieces = [], onAction }) {
  const [form, setForm] = useState({ panneId: '', description: '', coutTotal: 0, pieceId: '', quantitePiece: 1 });

  function submit(event) {
    event.preventDefault();
    onAction(async () => {
      await api.createReparation({
        panneId: Number(form.panneId),
        description: form.description,
        coutTotal: Number(form.coutTotal),
        piecesConsommees: form.pieceId ? [{ pieceId: Number(form.pieceId), quantite: Number(form.quantitePiece) }] : [],
      });
      setForm({ panneId: '', description: '', coutTotal: 0, pieceId: '', quantitePiece: 1 });
    });
  }

  return (
    <section className="stack">
      <form className="form-grid panel" onSubmit={submit}>
        <Select label="Panne" value={form.panneId} onChange={(panneId) => setForm({ ...form, panneId })} options={pannes.map((item) => [item.id, `#${item.id} - ${item.description}`])} required />
        <Field label="Description" value={form.description} onChange={(description) => setForm({ ...form, description })} required />
        <Field label="Cout total" type="number" value={form.coutTotal} onChange={(coutTotal) => setForm({ ...form, coutTotal })} />
        <Select label="Piece utilisee" value={form.pieceId} onChange={(pieceId) => setForm({ ...form, pieceId })} options={pieces.map((item) => [item.id, `${item.reference} - ${item.designation} (${item.quantiteStock})`])} />
        <Field label="Quantite piece" type="number" value={form.quantitePiece} onChange={(quantitePiece) => setForm({ ...form, quantitePiece })} />
        <button>Creer reparation</button>
      </form>
      <section className="panel">
        <div className="section-title"><h2>Interventions</h2><span>{rows.length} lignes</span></div>
        <div className="table-wrap">
          <table>
            <thead><tr><th>ID</th><th>Description</th><th>Debut</th><th>Fin</th><th>Cout</th><th>Action</th></tr></thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td>{row.id}</td><td>{row.description}</td><td>{formatValue(row.dateDebut)}</td><td>{formatValue(row.dateFin)}</td><td>{formatValue(row.coutTotal)}</td>
                  <td><button onClick={() => onAction(() => api.cloturerReparation(row.id, { noteSatisfaction: 5 }))}>Cloturer</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}

function Prets({ rows = [], equipements = [], utilisateurs = [], onAction }) {
  const employes = utilisateurs.filter((user) => user.employe).map((user) => user.employe);
  const [form, setForm] = useState({ equipementId: '', employeId: '', dateRetourPrevue: '', motif: '' });

  function submit(event) {
    event.preventDefault();
    onAction(async () => {
      await api.createPret({ ...form, equipementId: Number(form.equipementId), employeId: Number(form.employeId) });
      setForm({ equipementId: '', employeId: '', dateRetourPrevue: '', motif: '' });
    });
  }

  return (
    <section className="stack">
      <form className="form-grid panel" onSubmit={submit}>
        <Select label="Equipement" value={form.equipementId} onChange={(equipementId) => setForm({ ...form, equipementId })} options={equipements.map((item) => [item.id, `${item.numSerie} - ${item.type}`])} required />
        <Select label="Employe" value={form.employeId} onChange={(employeId) => setForm({ ...form, employeId })} options={employes.map((item) => [item.id, `${item.nom} ${item.prenom}`])} required />
        <Field label="Retour prevu" type="date" value={form.dateRetourPrevue} onChange={(dateRetourPrevue) => setForm({ ...form, dateRetourPrevue })} required />
        <Field label="Motif" value={form.motif} onChange={(motif) => setForm({ ...form, motif })} required />
        <button>Valider pret</button>
      </form>
      <section className="panel">
        <div className="section-title"><h2>Prets</h2><span>{rows.length} lignes</span></div>
        <div className="table-wrap">
          <table>
            <thead><tr><th>ID</th><th>Depart</th><th>Retour prevu</th><th>Retour reel</th><th>Statut</th><th>Actions</th></tr></thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td>{row.id}</td><td>{formatValue(row.dateDepart)}</td><td>{formatValue(row.dateRetourPrevue)}</td><td>{formatValue(row.dateRetourReelle)}</td><td>{row.statut}</td>
                  <td className="row-actions">
                    {row.statut === 'EN_ATTENTE' && <button onClick={() => onAction(() => api.validerPret(row.id))}>Valider</button>}
                    {row.statut === 'EN_ATTENTE' && <button onClick={() => onAction(() => api.refuserPret(row.id))}>Refuser</button>}
                    {(row.statut === 'VALIDE' || row.statut === 'EN_RETARD') && <button onClick={() => onAction(() => api.cloturerPret(row.id))}>Retour</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}

function Utilisateurs({ rows = [], onAction }) {
  const [form, setForm] = useState({ login: '', motDePasse: '', email: '', role: 'EMPLOYE' });
  const [passwords, setPasswords] = useState({});

  function submit(event) {
    event.preventDefault();
    onAction(async () => {
      await api.createUtilisateur(form);
      setForm({ login: '', motDePasse: '', email: '', role: 'EMPLOYE' });
    });
  }

  return (
    <section className="stack">
      <form className="form-grid panel" onSubmit={submit}>
        <Field label="Login" value={form.login} onChange={(login) => setForm({ ...form, login })} required />
        <Field label="Mot de passe" type="password" value={form.motDePasse} onChange={(motDePasse) => setForm({ ...form, motDePasse })} required />
        <Field label="Email" type="email" value={form.email} onChange={(email) => setForm({ ...form, email })} />
        <Select label="Role" value={form.role} onChange={(role) => setForm({ ...form, role })} options={['ADMIN', 'TECHNICIEN', 'EMPLOYE', 'DIRECTEUR']} />
        <button>Creer utilisateur</button>
      </form>
      <section className="panel">
        <div className="section-title"><h2>Comptes</h2><span>{rows.length} lignes</span></div>
        <div className="table-wrap">
          <table>
            <thead><tr><th>Login</th><th>Email</th><th>Role</th><th>Actif</th><th>Echecs</th><th>Actions</th></tr></thead>
            <tbody>
              {rows.map((user) => (
                <tr key={user.id}>
                  <td>{user.login}</td>
                  <td>{formatValue(user.email)}</td>
                  <td>{user.role}</td>
                  <td>{formatValue(user.actif)}</td>
                  <td>{user.tentativesEchec}</td>
                  <td className="row-actions">
                    {user.actif
                      ? <button onClick={() => onAction(() => api.desactiverUtilisateur(user.id))}>Desactiver</button>
                      : <button onClick={() => onAction(() => api.reactiverUtilisateur(user.id))}>Reactiver</button>}
                    <input
                      className="inline-input"
                      type="password"
                      placeholder="Nouveau MDP"
                      value={passwords[user.id] ?? ''}
                      onChange={(event) => setPasswords({ ...passwords, [user.id]: event.target.value })}
                    />
                    <button
                      onClick={() => onAction(async () => {
                        await api.resetPasswordUtilisateur(user.id, passwords[user.id] || '123456');
                        setPasswords({ ...passwords, [user.id]: '' });
                      })}
                    >
                      Reset MDP
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}

function Fournisseurs({ rows = [], onAction }) {
  const [form, setForm] = useState({ nom: '', contact: '', email: '', specialite: '' });

  function submit(event) {
    event.preventDefault();
    onAction(async () => {
      await api.createFournisseur(form);
      setForm({ nom: '', contact: '', email: '', specialite: '' });
    });
  }

  return (
    <section className="stack">
      <form className="form-grid panel" onSubmit={submit}>
        <Field label="Nom" value={form.nom} onChange={(nom) => setForm({ ...form, nom })} required />
        <Field label="Contact" value={form.contact} onChange={(contact) => setForm({ ...form, contact })} />
        <Field label="Email" type="email" value={form.email} onChange={(email) => setForm({ ...form, email })} />
        <Field label="Specialite" value={form.specialite} onChange={(specialite) => setForm({ ...form, specialite })} />
        <button>Ajouter fournisseur</button>
      </form>
      <Table title="Fournisseurs" rows={rows} columns={['nom', 'contact', 'email', 'specialite']} />
    </section>
  );
}

function Audit({ rows = [], onAction }) {
  return (
    <section className="stack">
      <section className="panel">
        <div className="section-title">
          <h2>Notifications planifiees</h2>
          <span>Execution manuelle</span>
        </div>
        <p className="muted">Lance les rappels de prets, escalades de pannes et alertes stock sans attendre le scheduler.</p>
        <button onClick={() => onAction(() => api.runNotifications())}>Executer notifications</button>
      </section>
      <Table title="Journal d'audit" rows={rows} columns={['dateHeure', 'action', 'details', 'ipAdresse']} />
    </section>
  );
}

function Rapports({ onAction }) {
  const reports = [
    ['equipements', 'Inventaire equipements'],
    ['stock', 'Stock pieces'],
    ['pannes', 'Suivi des pannes'],
  ];

  return (
    <section className="panel">
      <div className="section-title">
        <h2>Exports CSV</h2>
        <span>Admin / Directeur</span>
      </div>
      <div className="report-actions">
        {reports.map(([name, label]) => (
          <button key={name} onClick={() => onAction(() => api.downloadReport(name))}>{label}</button>
        ))}
      </div>
    </section>
  );
}

function Field({ label, value, onChange, type = 'text', required = false }) {
  return (
    <label>
      {label}
      <input type={type} value={value ?? ''} required={required} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function Select({ label, value, onChange, options, required = false }) {
  return (
    <label>
      {label}
      <select value={value ?? ''} required={required} onChange={(event) => onChange(event.target.value)}>
        <option value="">-</option>
        {options.map((option) => {
          const value = Array.isArray(option) ? option[0] : option;
          const label = Array.isArray(option) ? option[1] : option;
          return <option key={value} value={value}>{label}</option>;
        })}
      </select>
    </label>
  );
}

function FileUpload({ onSelect }) {
  return (
    <label className="file-button">
      Photo
      <input
        type="file"
        accept="image/png,image/jpeg,image/webp"
        onChange={(event) => {
          const file = event.target.files?.[0];
          if (file) {
            onSelect(file);
            event.target.value = '';
          }
        }}
      />
    </label>
  );
}

function PhotoPreview({ id, photoPath, loader, compact = false }) {
  const [url, setUrl] = useState(null);

  useEffect(() => {
    if (!photoPath) {
      setUrl(null);
      return undefined;
    }

    let active = true;
    let objectUrl = null;
    loader(id).then((nextUrl) => {
      if (!active) {
        if (nextUrl) {
          URL.revokeObjectURL(nextUrl);
        }
        return;
      }
      objectUrl = nextUrl;
      setUrl(nextUrl);
    });

    return () => {
      active = false;
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [id, photoPath, loader]);

  if (!photoPath) {
    return <span className="muted">-</span>;
  }

  if (!url) {
    return <span className="muted">Chargement...</span>;
  }

  return <img className={compact ? 'photo-preview compact' : 'photo-preview'} src={url} alt="Photo" />;
}

function Table({ title, rows = [], columns }) {
  return (
    <section className="panel">
      <div className="section-title">
        <h2>{title}</h2>
        <span>{rows.length} lignes</span>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              {columns.map((column) => (
                <th key={column}>{column}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((row, index) => (
              <tr key={row.id ?? index}>
                {columns.map((column) => (
                  <td key={column}>{formatValue(row[column])}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function formatValue(value) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  if (typeof value === 'boolean') {
    return value ? 'Oui' : 'Non';
  }
  if (typeof value === 'object') {
    return value.nom ?? value.login ?? value.numSerie ?? JSON.stringify(value);
  }
  return value;
}

createRoot(document.getElementById('root')).render(<App />);
