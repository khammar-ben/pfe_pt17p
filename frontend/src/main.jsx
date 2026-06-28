import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { api, getAuthToken, setAuthToken } from './api.js';
import './styles.css';

const modules = [
  { id: 'dashboard', label: 'Dashboard', icon: '▦', description: "Vue globale de l'activite du parc", roles: ['ADMIN', 'DIRECTEUR'] },
  { id: 'equipements', label: 'Equipements', icon: '□', description: 'Inventaire, packs et affectations', roles: ['ADMIN', 'TECHNICIEN', 'EMPLOYE'] },
  { id: 'pannes', label: 'Pannes', icon: '!', description: 'Declaration et suivi des incidents', roles: ['ADMIN', 'TECHNICIEN', 'EMPLOYE'] },
  { id: 'reparations', label: 'Reparations', icon: '◇', description: 'Planification et interventions techniques', roles: ['ADMIN', 'TECHNICIEN', 'EMPLOYE'] },
  { id: 'stock', label: 'Stock', icon: '▤', description: 'Materiel et pieces de rechange', roles: ['ADMIN', 'TECHNICIEN'] },
  { id: 'notifications', label: 'Notifications', icon: '○', description: 'Alertes et taches a traiter', roles: ['ADMIN', 'TECHNICIEN', 'EMPLOYE'] },
  { id: 'prets', label: 'Prets', icon: '↗', description: 'Demandes et retours de materiel', roles: ['ADMIN', 'EMPLOYE'] },
  { id: 'utilisateurs', label: 'Utilisateurs', icon: '◎', description: 'Comptes et acces collaborateurs', roles: ['ADMIN'] },
  { id: 'fournisseurs', label: 'Fournisseurs', icon: '⌂', description: 'Partenaires et contacts', roles: ['ADMIN'] },
  { id: 'rapports', label: 'Rapports', icon: '▥', description: 'Exports et indicateurs de gestion', roles: ['ADMIN', 'DIRECTEUR'] },
  { id: 'audit', label: 'Audit', icon: '✓', description: 'Journal et controles systeme', roles: ['ADMIN'] },
];

function App() {
  const [active, setActive] = useState('dashboard');
  const [data, setData] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [quickView, setQuickView] = useState(null);
  const [session, setSession] = useState(() => {
    const stored = localStorage.getItem('pt17_session');
    return stored ? JSON.parse(stored) : null;
  });
  const visibleModules = modules.filter((module) => module.roles.includes(session?.role));
  const activeModule = visibleModules.find((module) => module.id === active);
  const unreadNotifications = data.notifications?.filter((item) => !item.lu).length ?? 0;

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

  useEffect(() => {
    if (!session) return undefined;
    const timer = setInterval(async () => {
      try {
        const [notifications, pannes] = await Promise.all([
          api.appNotifications(),
          api.pannes(),
        ]);
        setData((current) => ({ ...current, notifications, pannes }));
      } catch {
        // The regular page error handling remains responsible for connection errors.
      }
    }, 10000);
    return () => clearInterval(timer);
  }, [session]);

  useEffect(() => {
    if (session && visibleModules.length > 0 && !visibleModules.some((module) => module.id === active)) {
      setActive(visibleModules[0].id);
    }
  }, [active, session, visibleModules]);

  async function loadData(ignore = false) {
      setLoading(true);
      setError('');
      try {
        const result = await Promise.allSettled([
          api.dashboard(),
          api.equipements(),
          api.employes(),
          api.pannes(),
          api.reparations(),
          api.pieces(),
          api.prets(),
          api.utilisateurs(),
          api.fournisseurs(),
          api.auditLogs(),
          api.appNotifications(),
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
            employes: value(2, []),
            pannes: value(3, []),
            reparations: value(4, []),
            stock: value(5, []),
            prets: value(6, []),
            utilisateurs: value(7, []),
            fournisseurs: value(8, []),
            auditLogs: value(9, []),
            notifications: value(10, []),
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
      const result = await action();
      await loadData(false);
      return result;
    } catch (err) {
      setError(err.message);
    }
  }

  function openResult(result) {
    if (result.kind === 'equipement') {
      setActive('equipements');
      setQuickView(result.item);
      return;
    }
    setActive(result.module);
  }

  async function handleLogin(credentials) {
    const result = await api.login(credentials);
    setAuthToken(result.token);
    const nextSession = {
      login: result.login,
      role: result.role,
      userId: result.userId,
      employeId: result.employeId,
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
          <div className="brand-mark">P</div>
          <div>
            <strong>PT17</strong>
            <span>IT Asset Management</span>
          </div>
        </div>
        <span className="nav-label">Espace de travail</span>
        <nav>
          {visibleModules.map((module) => (
            <button
              key={module.id}
              className={active === module.id ? 'active' : ''}
              onClick={() => setActive(module.id)}
            >
              <span className="nav-icon">{module.icon}</span>
              <span className="nav-text">{module.label}</span>
              {module.id === 'notifications' && unreadNotifications > 0
                ? <span className="nav-badge">{unreadNotifications}</span>
                : null}
            </button>
          ))}
        </nav>
        <div className="sidebar-profile">
          <div className="profile-avatar">{session.login?.slice(0, 1).toUpperCase()}</div>
          <div>
            <strong>{session.login}</strong>
            <span>{session.role}</span>
          </div>
        </div>
      </aside>

      <main className="app-main">
        <header className="hero">
          <div className="page-heading">
            <p>PT17 / {activeModule?.label}</p>
            <h1>{activeModule?.label}</h1>
            <span>{activeModule?.description}</span>
          </div>
          <div className="session">
            <GlobalSearch data={data} onOpen={openResult} />
            <NotificationBell
              rows={data.notifications ?? []}
              onOpenAll={() => setActive('notifications')}
              onAction={runAction}
              session={session}
            />
            <button className="logout-button" onClick={logout}>Se deconnecter</button>
          </div>
        </header>

        {loading && <div className="panel loading-state"><span /> Chargement des donnees...</div>}
        {error && <div className="panel error">Action impossible : {error}</div>}
        <div className="page-content">
          {!loading && (
            <ModuleView
              active={active}
              data={data}
              onAction={runAction}
              session={session}
              quickView={quickView}
              onCloseQuickView={() => setQuickView(null)}
            />
          )}
        </div>
      </main>
      <ChatbotAssistant session={session} />
    </div>
  );
}

function LoginScreen({ onLogin }) {
  const [login, setLogin] = useState('');
  const [motDePasse, setMotDePasse] = useState('');
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
      <section className="login-visual">
        <div className="login-brand">
          <div className="brand-mark large">P</div>
          <span>PT17</span>
        </div>
        <div>
          <span className="eyebrow">Gestion du parc informatique</span>
          <h1>Un espace unique pour piloter votre IT.</h1>
          <p>Equipements, incidents, reparations et stock reunis dans une plateforme simple et securisee.</p>
        </div>
        <div className="login-features">
          <span>Inventaire centralise</span>
          <span>Suivi en temps reel</span>
          <span>Workflows par role</span>
        </div>
      </section>
      <form className="login-card" onSubmit={submit}>
        <div className="login-card-heading">
          <span>Acces securise</span>
          <h2>Bienvenue</h2>
          <p>Connectez-vous pour acceder a votre espace.</p>
        </div>
        <label>
          Identifiant
          <input autoComplete="username" placeholder="Votre identifiant" value={login} onChange={(event) => setLogin(event.target.value)} />
        </label>
        <label>
          Mot de passe
          <input autoComplete="current-password" placeholder="Votre mot de passe" type="password" value={motDePasse} onChange={(event) => setMotDePasse(event.target.value)} />
        </label>
        {error && <div className="form-error">{error}</div>}
        <button disabled={loading}>{loading ? 'Connexion...' : 'Se connecter'}</button>
        <small>Plateforme interne PT17 · Acces reserve</small>
      </form>
    </main>
  );
}

function GlobalSearch({ data = {}, onOpen }) {
  const [query, setQuery] = useState('');
  const needle = normalizeSearch(query);
  const results = needle.length < 2 ? [] : [
    ...(data.equipements ?? []).map((item) => ({
      kind: 'equipement',
      module: 'equipements',
      title: item.numSerie,
      subtitle: `${item.type ?? 'Equipement'} - ${item.marque ?? ''} ${item.modele ?? ''}`.trim(),
      meta: item.statut,
      item,
      haystack: [item.numSerie, item.type, item.marque, item.modele, item.employe?.nom, item.employe?.prenom].join(' '),
    })),
    ...(data.employes ?? []).map((item) => ({
      kind: 'employe',
      module: 'utilisateurs',
      title: `${item.prenom} ${item.nom}`,
      subtitle: `${item.service?.nom ?? 'Sans departement'} - ${item.poste ?? 'Poste non renseigne'}`,
      meta: item.email,
      haystack: [item.nom, item.prenom, item.email, item.poste, item.service?.nom].join(' '),
    })),
    ...(data.pannes ?? []).map((item) => ({
      kind: 'panne',
      module: 'pannes',
      title: `Panne #${item.id}`,
      subtitle: item.description,
      meta: item.statut,
      haystack: [item.id, item.description, item.urgence, item.statut, item.equipement?.numSerie].join(' '),
    })),
    ...(data.stock ?? []).map((item) => ({
      kind: 'piece',
      module: 'stock',
      title: item.reference,
      subtitle: item.designation,
      meta: `Stock ${item.quantiteStock}`,
      haystack: [item.reference, item.designation, item.localisation, item.usage].join(' '),
    })),
    ...(data.fournisseurs ?? []).map((item) => ({
      kind: 'fournisseur',
      module: 'fournisseurs',
      title: item.nom,
      subtitle: item.email ?? item.telephone ?? 'Fournisseur',
      meta: 'Partenaire',
      haystack: [item.nom, item.email, item.telephone].join(' '),
    })),
  ].filter((item) => normalizeSearch(item.haystack).includes(needle)).slice(0, 7);

  function open(result) {
    onOpen(result);
    setQuery('');
  }

  return (
    <div className="global-search">
      <input
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        placeholder="Recherche globale..."
      />
      {query && (
        <div className="global-results">
          {results.map((result, index) => (
            <button key={`${result.kind}-${index}`} type="button" onClick={() => open(result)}>
              <span>{result.kind}</span>
              <strong>{result.title}</strong>
              <small>{result.subtitle}</small>
              {result.meta && <em>{result.meta}</em>}
            </button>
          ))}
          {results.length === 0 && <div className="search-empty">Aucun resultat trouve.</div>}
        </div>
      )}
    </div>
  );
}

function NotificationBell({ rows = [], onOpenAll, onAction, session }) {
  const [open, setOpen] = useState(false);
  const unread = rows.filter((item) => !item.lu).length;
  const recent = [...rows].sort((left, right) => new Date(right.dateCreation || 0) - new Date(left.dateCreation || 0)).slice(0, 5);

  return (
    <div className="notif-bell">
      <button className="notification-shortcut" type="button" onClick={() => setOpen(!open)}>
        <span>!</span>
        {unread > 0 ? unread : 0}
      </button>
      {open && (
        <div className="notif-dropdown">
          <div className="notif-head">
            <strong>Notifications</strong>
            <span>{unread} non lue(s)</span>
          </div>
          <div className="notif-list">
            {recent.map((notification) => (
              <article className={notification.lu ? '' : 'unread'} key={notification.id}>
                <div>
                  <strong>{notification.titre}</strong>
                  <p>{notification.message}</p>
                  <StatusBadge value={notification.statut} />
                </div>
                <div className="notif-actions">
                  {session?.role !== 'TECHNICIEN' && !notification.lu && (
                    <button type="button" onClick={() => onAction(() => api.marquerNotificationLue(notification.id))}>lu</button>
                  )}
                  {session?.role === 'TECHNICIEN'
                    && notification.type === 'EQUIPEMENT_AFFECTE'
                    && notification.statut === 'NOUVELLE' && (
                      <button type="button" onClick={() => onAction(() => api.affecterNotificationEquipement(notification.id))}>traiter</button>
                  )}
                  {session?.role === 'TECHNICIEN'
                    && notification.type === 'EQUIPEMENT_AFFECTE'
                    && notification.statut === 'EN_COURS' && (
                      <button type="button" onClick={() => onAction(() => api.terminerNotificationEquipement(notification.id))}>done</button>
                  )}
                </div>
              </article>
            ))}
            {recent.length === 0 && <EmptyState title="Aucune notification" text="Les nouvelles alertes apparaitront ici." compact />}
          </div>
          <button className="notif-all" type="button" onClick={() => { setOpen(false); onOpenAll(); }}>Voir toutes les notifications</button>
        </div>
      )}
    </div>
  );
}

function ChatbotAssistant({ session }) {
  const quickPrompts = chatbotPrompts(session?.role);
  const [open, setOpen] = useState(false);
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState([
    {
      from: 'bot',
      title: 'PT17 Assistant',
      answer: chatbotIntro(session?.role),
      bullets: ['Choisis une question rapide ou ecris ton besoin.'],
    },
  ]);

  async function ask(text) {
    const clean = text.trim();
    if (!clean || loading) return;
    setMessage('');
    setMessages((current) => [...current, { from: 'user', answer: clean }]);
    setLoading(true);
    try {
      const reply = await api.chatbot(clean);
      setMessages((current) => [...current, { from: 'bot', ...reply }]);
    } catch (err) {
      setMessages((current) => [...current, {
        from: 'bot',
        title: 'Assistant indisponible',
        answer: err.message || 'Impossible de contacter le chatbot.',
        bullets: ['Verifie que le backend est lance puis reessaie.'],
      }]);
    } finally {
      setLoading(false);
    }
  }

  function submit(event) {
    event.preventDefault();
    ask(message);
  }

  return (
    <div className={`chatbot ${open ? 'open' : ''}`}>
      {open && (
        <section className="chatbot-panel">
          <div className="chatbot-head">
            <div>
              <span>Assistant intelligent</span>
              <strong>PT17 Copilot</strong>
            </div>
            <button type="button" onClick={() => setOpen(false)}>×</button>
          </div>
          <div className="chatbot-context">
            Connecte: <strong>{session?.login}</strong> · Role: <strong>{session?.role}</strong>
          </div>
          <div className="chatbot-quick">
            {quickPrompts.map((prompt) => (
              <button type="button" key={prompt} onClick={() => ask(prompt)} disabled={loading}>
                {prompt}
              </button>
            ))}
          </div>
          <div className="chatbot-messages">
            {messages.map((item, index) => (
              <article className={item.from === 'user' ? 'chat-message user' : 'chat-message bot'} key={`${item.from}-${index}`}>
                {item.title && <strong>{item.title}</strong>}
                <p>{item.answer}</p>
                {item.bullets?.length > 0 && (
                  <ul>
                    {item.bullets.map((bullet, bulletIndex) => (
                      <li key={`${index}-${bulletIndex}`}>{bullet}</li>
                    ))}
                  </ul>
                )}
              </article>
            ))}
            {loading && <article className="chat-message bot typing">Analyse en cours...</article>}
          </div>
          <form className="chatbot-form" onSubmit={submit}>
            <input
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              placeholder="Sowel bdarija wla francais... ex: chno khasni ndir lyom?"
            />
            <button type="submit" disabled={loading || !message.trim()}>Envoyer</button>
          </form>
        </section>
      )}
      <button className="chatbot-toggle" type="button" onClick={() => setOpen(!open)}>
        {open ? '−' : 'AI'}
      </button>
    </div>
  );
}

function chatbotPrompts(role) {
  if (role === 'TECHNICIEN') {
    return ['mes actions aujourd hui', 'pannes a claim', 'stock critique technique', 'pieces pour reparation'];
  }
  if (role === 'EMPLOYE') {
    return ['mon materiel', 'mes pannes', 'comment declarer une panne', 'etat de mon equipement'];
  }
  if (role === 'DIRECTEUR') {
    return ['resume executive', 'risques du parc', 'cout maintenance', 'stock critique'];
  }
  return ['actions aujourd hui', 'stock critique', 'resume dashboard', 'pack developpeur'];
}

function chatbotIntro(role) {
  if (role === 'TECHNICIEN') {
    return 'Salam, hdar m3aya bdarija wla francais. N9der n3awnk f pannes, claims et pieces.';
  }
  if (role === 'EMPLOYE') {
    return 'Salam, qder tswlni bdarija wla francais 3la materiel dyalk, pannes, w support.';
  }
  if (role === 'DIRECTEUR') {
    return 'Salam, qder tswl b ay tariqa 3la KPIs, risques, couts et decisions.';
  }
  return 'Salam, hdar m3aya bdarija wla francais. N9der n analyse parc, stock, pannes et packs.';
}

function StatusBadge({ value }) {
  const raw = String(value ?? '-');
  const label = raw.replaceAll('_', ' ');
  const tone = statusTone(raw);
  return <span className={`status-badge ${tone}`}>{label}</span>;
}

function statusTone(value) {
  const raw = String(value ?? '').toUpperCase();
  if (['AFFECTE', 'AFFECTEE', 'DONE', 'REPAREE', 'CLOTURE', 'CLOTUREE', 'TERMINEE', 'VALIDE', 'DISPONIBLE'].includes(raw)) return 'success';
  if (['EN_PANNE', 'HAUTE', 'REFUSE', 'REFUSEE', 'REFORME', 'EN_RETARD'].includes(raw)) return 'danger';
  if (['DECLAREE', 'EN_ATTENTE', 'EN_ATTENTE_PIECE', 'NOUVELLE', 'PENDING', 'MOYENNE'].includes(raw)) return 'warning';
  if (['A_AFFECTER', 'EN_COURS', 'EN_PRET', 'EN_ATTENTE_AFFECTATION', 'FAIBLE'].includes(raw)) return 'info';
  return 'neutral';
}

function EmptyState({ title, text, action, compact = false }) {
  return (
    <div className={`empty-state ${compact ? 'compact' : ''}`}>
      <span>∅</span>
      <strong>{title}</strong>
      <p>{text}</p>
      {action}
    </div>
  );
}

function ModuleView({ active, data, onAction, session, quickView, onCloseQuickView }) {
  if (active === 'dashboard') {
    return <DashboardPro data={data.dashboard} session={session} />;
  }
  if (active === 'equipements') {
    return <Equipements rows={data.equipements} employes={data.employes} pieces={data.stock} onAction={onAction} session={session} quickView={quickView} onCloseQuickView={onCloseQuickView} />;
  }
  if (active === 'pannes') {
    return <Pannes rows={data.pannes} equipements={data.equipements} utilisateurs={data.utilisateurs} onAction={onAction} session={session} />;
  }
  if (active === 'reparations') {
    return <Reparations rows={data.reparations} pannes={data.pannes} pieces={data.stock} utilisateurs={data.utilisateurs} onAction={onAction} session={session} />;
  }
  if (active === 'stock') {
    return <Stock rows={data.stock} fournisseurs={data.fournisseurs} onAction={onAction} session={session} />;
  }
  if (active === 'notifications') {
    return <Notifications rows={data.notifications} onAction={onAction} session={session} />;
  }
  if (active === 'prets') {
    return <Prets rows={data.prets} equipements={data.equipements} utilisateurs={data.utilisateurs} onAction={onAction} session={session} />;
  }
  if (active === 'utilisateurs') {
    return <Utilisateurs rows={data.utilisateurs} employes={data.employes} onAction={onAction} />;
  }
  if (active === 'fournisseurs') {
    return <Fournisseurs rows={data.fournisseurs} onAction={onAction} />;
  }
  if (active === 'rapports') {
    return <Rapports onAction={onAction} />;
  }
  return <Audit rows={data.auditLogs} onAction={onAction} />;
}

function Dashboard({ data, session }) {
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
    <section className="stack dashboard-page">
      <div className="dashboard-welcome">
        <div>
          <span className="eyebrow">Centre de controle</span>
          <h2>Bonjour, {session?.login} <span>👋</span></h2>
          <p>Voici la situation actuelle de votre parc informatique.</p>
        </div>
        <div className="live-indicator"><i /> Donnees en temps reel</div>
      </div>
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

function DashboardPro({ data, session }) {
  const monthlyPannes = objectToChartData(data.pannesParMois);
  const statusPannes = objectToChartData(data.pannesParStatut);
  const techReparations = objectToChartData(data.reparationsParTechnicien);
  const topEquipements = normalizeChartData(data.topEquipementsDefaillants);
  const criticalPieces = data.piecesStockCritique ?? [];
  const availability = Number(data.tauxDisponibilite ?? 0);
  const openPannes = Number(data.pannesEnCours ?? 0);
  const stockCritical = Number(data.stockCritique ?? 0);
  const lateLoans = Number(data.pretsEnRetard ?? 0);
  const healthTone = availability >= 90 ? 'good' : availability >= 70 ? 'warning' : 'danger';
  const kpis = [
    { label: 'Parc total', value: data.totalEquipements, helper: 'Equipements suivis', icon: '▣', tone: 'blue' },
    { label: 'Pannes ouvertes', value: openPannes, helper: openPannes > 0 ? 'A traiter en priorite' : 'Aucune panne active', icon: '△', tone: openPannes > 0 ? 'red' : 'green' },
    { label: 'Stock critique', value: stockCritical, helper: stockCritical > 0 ? 'Commande recommandee' : 'Stock stable', icon: '⬡', tone: stockCritical > 0 ? 'orange' : 'green' },
    { label: 'Cout maintenance', value: `${formatNumber(data.coutTotalReparations ?? 0)} DH`, helper: `${data.reparations ?? 0} intervention(s)`, icon: '◇', tone: 'purple' },
  ];
  const priorities = [
    { title: 'Incidents actifs', value: openPannes, detail: openPannes > 0 ? 'Suivre les pannes en cours' : 'Workflow pannes propre', tone: openPannes > 0 ? 'danger' : 'good' },
    { title: 'Pieces sous seuil', value: stockCritical, detail: stockCritical > 0 ? 'Verifier le stock critique' : 'Aucune rupture proche', tone: stockCritical > 0 ? 'warning' : 'good' },
    { title: 'Prets en retard', value: lateLoans, detail: lateLoans > 0 ? 'Relancer les retours' : 'Aucun retard detecte', tone: lateLoans > 0 ? 'danger' : 'good' },
  ];

  return (
    <section className="stack dashboard-page dashboard-pro">
      <div className="dashboard-welcome dashboard-command">
        <div>
          <span className="eyebrow">Centre de controle</span>
          <h2>Bonjour, {session?.login}</h2>
          <p>Vue claire des incidents, disponibilite, stock et couts de maintenance.</p>
        </div>
        <div className={`health-card ${healthTone}`}>
          <div className="health-ring" style={{ '--value': `${Math.min(100, Math.max(0, availability))}%` }}>
            <strong>{availability}%</strong>
          </div>
          <div>
            <span>Disponibilite</span>
            <strong>{healthTone === 'good' ? 'Stable' : healthTone === 'warning' ? 'A surveiller' : 'Critique'}</strong>
            <small>Objectif recommande : 95%</small>
          </div>
        </div>
      </div>

      <section className="kpis dashboard-kpis">
        {kpis.map((item) => (
          <article className={`card metric-card ${item.tone}`} key={item.label}>
            <div className="metric-icon">{item.icon}</div>
            <span>{item.label}</span>
            <strong>{item.value}</strong>
            <small>{item.helper}</small>
          </article>
        ))}
      </section>

      <section className="dashboard-analytics">
        <VerticalBars title="Evolution des pannes" subtitle="Lecture rapide par mois" data={monthlyPannes} />
        <section className="dashboard-side">
          <DonutChart title="Repartition des pannes" data={statusPannes} />
          <section className="panel priority-panel">
            <div className="section-title">
              <h2>Priorites</h2>
              <span>Action rapide</span>
            </div>
            <div className="priority-list">
              {priorities.map((item) => (
                <article className={`priority-item ${item.tone}`} key={item.title}>
                  <strong>{item.value}</strong>
                  <div>
                    <span>{item.title}</span>
                    <p>{item.detail}</p>
                  </div>
                </article>
              ))}
            </div>
          </section>
        </section>
      </section>

      <section className="dashboard-grid compact-dashboard-grid">
        <BarChart title="Reparations par technicien" data={techReparations} />
        <BarChart title="Top equipements defaillants" data={topEquipements} />
      </section>

      <section className="panel">
        <div className="section-title">
          <h2>Pieces en stock critique</h2>
          <span>{criticalPieces.length} alerte(s)</span>
        </div>
        <div className="critical-stock-grid">
          {criticalPieces.map((piece) => (
            <article className="critical-stock-card" key={piece.reference}>
              <div>
                <strong>{piece.designation}</strong>
                <span>{piece.reference}</span>
                <small>Stock {piece.quantiteStock} / seuil {piece.seuilMinimum}</small>
              </div>
            </article>
          ))}
          {criticalPieces.length === 0 && (
            <EmptyState compact title="Aucune piece critique" text="Le stock est stable pour le moment." />
          )}
        </div>
      </section>
    </section>
  );
}

function normalizeChartData(data = []) {
  if (!Array.isArray(data)) return [];
  return data.map((item, index) => {
    if (Array.isArray(item)) return { label: item[0], value: item[1] };
    return {
      label: item.label ?? item.nom ?? item.name ?? item.equipement ?? item.numSerie ?? `Item ${index + 1}`,
      value: item.value ?? item.total ?? item.count ?? item.nombre ?? item.pannes ?? 0,
    };
  });
}

function formatNumber(value) {
  return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(Number(value || 0));
}

function VerticalBars({ title, subtitle, data = [] }) {
  const max = Math.max(1, ...data.map((item) => Number(item.value)));
  return (
    <section className="panel vertical-chart-card">
      <div className="section-title">
        <div>
          <h2>{title}</h2>
          <p>{subtitle}</p>
        </div>
        <span>{data.length} point(s)</span>
      </div>
      <div className="vertical-bars">
        {data.length === 0 && <div className="empty-chart">Aucune donnee</div>}
        {data.map((item) => (
          <div className="vertical-bar" key={item.label}>
            <strong>{item.value}</strong>
            <div className="vertical-track">
              <span style={{ height: `${(Number(item.value) / max) * 100}%` }} />
            </div>
            <em>{item.label}</em>
          </div>
        ))}
      </div>
    </section>
  );
}

function DonutChart({ title, data = [] }) {
  const colors = ['#58aaff', '#f59e0b', '#ef4444', '#22c55e', '#a78bfa', '#14b8a6'];
  const total = data.reduce((sum, item) => sum + Number(item.value || 0), 0);
  let start = 0;
  const gradient = total > 0
    ? data.map((item, index) => {
        const value = Number(item.value || 0);
        const end = start + (value / total) * 100;
        const segment = `${colors[index % colors.length]} ${start}% ${end}%`;
        start = end;
        return segment;
      }).join(', ')
    : '#202a36 0% 100%';

  return (
    <section className="panel donut-card">
      <div className="section-title">
        <h2>{title}</h2>
        <span>{total} total</span>
      </div>
      <div className="donut-layout">
        <div className="donut" style={{ background: `conic-gradient(${gradient})` }}>
          <div>
            <strong>{total}</strong>
            <span>pannes</span>
          </div>
        </div>
        <div className="donut-legend">
          {data.length === 0 && <span className="muted">Aucune donnee</span>}
          {data.map((item, index) => (
            <div key={item.label}>
              <i style={{ background: colors[index % colors.length] }} />
              <span>{item.label}</span>
              <strong>{item.value}</strong>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function PieceLine({ piece }) {
  if (!piece) return null;
  return (
    <div className="piece-line">
      <div>
        <strong>{piece.designation}</strong>
        <span>{piece.reference} - stock {piece.quantiteStock}</span>
      </div>
    </div>
  );
}

function StockInventory({ rows = [] }) {
  const groups = rows.reduce((acc, piece) => {
    const group = piece.usage === 'MATERIEL' ? typeMateriel(piece) : 'Pieces de rechange';
    return { ...acc, [group]: [...(acc[group] ?? []), piece] };
  }, {});
  const order = ['Postes', 'Ecrans', 'Accessoires', 'Impression', 'Reseau', 'Pieces de rechange'];
  return (
    <section className="panel stock-inventory">
      <div className="section-title">
        <div>
          <h2>Catalogue stock</h2>
          <p>Catalogue groupe par type de materiel et seuil de stock.</p>
        </div>
        <span>{rows.length} references</span>
      </div>
      <div className="stock-groups">
        {order.filter((group) => groups[group]?.length).map((group) => (
          <section className="stock-group" key={group}>
            <div className="stock-group-title">
              <strong>{group}</strong>
              <span>{groups[group].length} item(s)</span>
            </div>
            <div className="stock-product-grid">
              {groups[group].map((piece) => (
                <article className={Number(piece.quantiteStock || 0) < Number(piece.seuilMinimum || 0) ? 'stock-product critical' : 'stock-product'} key={piece.id}>
                  <div>
                    <strong>{piece.designation}</strong>
                    <span>{piece.reference}</span>
                  </div>
                  <footer>
                    <small>Stock {piece.quantiteStock}</small>
                    <small>Seuil {piece.seuilMinimum}</small>
                    <small>{piece.localisation ?? '-'}</small>
                  </footer>
                </article>
              ))}
            </div>
          </section>
        ))}
      </div>
    </section>
  );
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

function Equipements({ rows = [], employes = [], pieces = [], onAction, session, quickView, onCloseQuickView }) {
  const [form, setForm] = useState({ departementId: '', employeId: '', pieceIds: [] });
  const [demandePieceId, setDemandePieceId] = useState('');
  const [suggestionReason, setSuggestionReason] = useState('');
  const [sectionsOuvertes, setSectionsOuvertes] = useState({
    'Postes': true,
    'Ecrans': true,
    'Accessoires': true,
  });
  const materielsDisponibles = pieces.filter((item) =>
    item.quantiteStock > 0 && item.usage === 'MATERIEL'
  );
  const pcPieces = materielsDisponibles.filter((item) =>
    `${item.reference} ${item.designation}`.toUpperCase().includes('PC')
  );
  const departements = [...new Map(
    employes
      .filter((item) => item.service)
      .map((item) => [item.service.id, item.service])
  ).values()].sort((left, right) => left.nom.localeCompare(right.nom, 'fr', { sensitivity: 'base' }));
  const employesFiltres = form.departementId
    ? employes.filter((item) => String(item.service?.id) === String(form.departementId))
    : [];
  const employeSelectionne = employes.find((item) => String(item.id) === String(form.employeId));
  const departementSelectionne = departements.find((item) => String(item.id) === String(form.departementId));
  const materielsParType = materielsDisponibles.reduce((groups, item) => {
    const type = typeMateriel(item);
    return { ...groups, [type]: [...(groups[type] ?? []), item] };
  }, {});
  const ordreTypes = ['Postes', 'Ecrans', 'Accessoires', 'Impression', 'Reseau'];
  const canAddEquipement = session?.role === 'ADMIN';
  const canRequestEquipement = session?.role === 'EMPLOYE' && session?.employeId;
  const presets = [
    {
      label: 'Developpeur',
      slots: [['macbook', 'latitude', 'thinkpad'], ['ecran'], ['dock'], ['clavier'], ['souris'], ['casque']],
      reason: 'Profil developpeur : 1 poste performant + ecran + dock + accessoires confort.',
    },
    {
      label: 'Finance',
      slots: [['elitebook', 'latitude', 'thinkpad'], ['ecran'], ['clavier'], ['souris'], ['casque']],
      reason: 'Profil finance : 1 laptop fiable, ecran de confort et accessoires bureau.',
    },
    {
      label: 'Support IT',
      slots: [['thinkpad', 'elitebook', 'vostro'], ['ecran'], ['clavier'], ['souris'], ['switch'], ['point acces', 'ubiquiti']],
      reason: 'Support IT : 1 poste mobile + materiel reseau pour intervention rapide.',
    },
    {
      label: 'Direction',
      slots: [['macbook', 'elitebook'], ['ecran'], ['dock'], ['casque'], ['souris']],
      reason: 'Direction : 1 poste premium, mobilite et setup reunion.',
    },
    {
      label: 'Teletravail',
      slots: [['latitude', 'thinkpad', 'elitebook'], ['ecran'], ['dock'], ['casque'], ['clavier'], ['souris']],
      reason: 'Teletravail : 1 poste + setup complet maison/bureau avec audio.',
    },
  ];

  function submit(event) {
    event.preventDefault();
    onAction(async () => {
      await api.affecterPack({
        employeId: Number(form.employeId),
        pieceIds: form.pieceIds.map(Number),
      });
      setForm({ departementId: '', employeId: '', pieceIds: [] });
    });
  }

  function toggleMateriel(pieceId) {
    const value = String(pieceId);
    const piece = materielsDisponibles.find((item) => String(item.id) === value);
    const alreadySelected = form.pieceIds.includes(value);
    const nextIds = alreadySelected
      ? form.pieceIds.filter((id) => id !== value)
      : [
          ...form.pieceIds.filter((id) => {
            const selectedPiece = materielsDisponibles.find((item) => String(item.id) === String(id));
            return typeMateriel(selectedPiece) !== 'Postes' || typeMateriel(piece) !== 'Postes';
          }),
          value,
        ];
    setForm({ ...form, pieceIds: nextIds });
  }

  function toggleSection(type) {
    setSectionsOuvertes({ ...sectionsOuvertes, [type]: !sectionsOuvertes[type] });
  }

  function suggestionPack() {
    if (!employeSelectionne && !departementSelectionne) return;
    applyPreset(presetForContext(departementSelectionne, employeSelectionne));
  }

  function presetForContext(departement, employe) {
    const profile = normalizeSearch(`${employe?.poste ?? ''} ${employe?.service?.nom ?? ''} ${departement?.nom ?? ''}`);
    const preset = profile.includes('developp') || profile.includes('dev') || profile.includes('front') || profile.includes('fullstack')
      ? presets[0]
      : profile.includes('technicien') || profile.includes('support') || profile.includes('reseau')
        ? presets[2]
        : profile.includes('finance') || profile.includes('comptable')
          ? presets[1]
          : profile.includes('direction') || profile.includes('directeur')
            ? presets[3]
            : presets[4];
    return preset;
  }

  function applyPreset(preset, baseSet = new Set(), formPatch = {}) {
    const selected = new Set(baseSet);
    const pick = (...keywords) => materielsDisponibles
      .filter((item) => keywords.some((keyword) => normalizeSearch(`${item.reference} ${item.designation}`).includes(normalizeSearch(keyword))))
      .sort((left, right) => Number(right.prixUnitaire || 0) - Number(left.prixUnitaire || 0))[0];
    preset.slots.forEach((slotKeywords) => {
      const item = pick(...slotKeywords);
      if (item?.id) selected.add(String(item.id));
    });
    const organizedIds = keepOnePoste([...selected], materielsDisponibles);
    setForm({ ...form, ...formPatch, pieceIds: organizedIds });
    setSectionsOuvertes({ Postes: true, Ecrans: true, Accessoires: true, Impression: false, Reseau: false });
    setSuggestionReason(`${preset.label} - ${preset.reason}`);
  }

  function choisirDepartement(departement) {
    const nextForm = { ...form, departementId: String(departement.id), employeId: '' };
    applyPreset(presetForContext(departement, null), new Set(), nextForm);
  }

  function choisirEmploye(employeId) {
    const employe = employes.find((item) => String(item.id) === String(employeId));
    applyPreset(presetForContext(departementSelectionne, employe), new Set(), { employeId });
  }

  return (
    <section className="stack">
      {canAddEquipement && (
        <form className="panel pack-form" onSubmit={submit}>
          <div className="section-title">
            <div>
              <span className="eyebrow">Onboarding employe</span>
              <h2>Affecter un pack d'integration</h2>
              <p>Le pack regroupe le materiel remis au collaborateur. Les pieces de rechange restent dans le stock technique.</p>
            </div>
          </div>
          <div className="pack-smart-layout">
            <aside className="ai-pack-card">
              <span className="eyebrow">AI Pack</span>
              <h3>Suggestion automatique</h3>
              <p>Selectionnez un employe puis laissez l'assistant proposer les pieces adaptees a son profil.</p>
              <div className="ai-picker">
                <div className="mini-section-title">
                  <span>1</span>
                  <div>
                    <strong>Departement</strong>
                    <small>Les services disponibles</small>
                  </div>
                </div>
                <div className="preset-buttons department-preset-buttons">
                  {departements.map((departement) => {
                    const count = employes.filter((item) => String(item.service?.id) === String(departement.id)).length;
                    return (
                      <button
                        className={String(form.departementId) === String(departement.id) ? 'selected' : ''}
                        key={departement.id}
                        type="button"
                        onClick={() => choisirDepartement(departement)}
                      >
                        {departement.nom}
                        <small>{count}</small>
                      </button>
                    );
                  })}
                </div>
                <Select
                  label="Employe"
                  value={form.employeId}
                  onChange={choisirEmploye}
                  options={employesFiltres.map((item) => [item.id, `${item.prenom} ${item.nom} - ${item.poste ?? 'Poste non renseigne'}`])}
                  placeholder={form.departementId ? 'Choisir un employe' : "Choisir d'abord le departement"}
                  disabled={!form.departementId}
                  required
                />
              </div>
              {employeSelectionne && (
                <div className="ai-profile">
                  <strong>{employeSelectionne.prenom} {employeSelectionne.nom}</strong>
                  <span>{employeSelectionne.poste ?? 'Poste non renseigne'} - {employeSelectionne.service?.nom ?? 'Sans departement'}</span>
                </div>
              )}
              {!employeSelectionne && departementSelectionne && (
                <div className="ai-profile">
                  <strong>Configuration {departementSelectionne.nom}</strong>
                  <span>Le pack s'adapte au departement selectionne.</span>
                </div>
              )}
              <button type="button" disabled={!employeSelectionne && !departementSelectionne} onClick={suggestionPack}>
                Proposer un pack
              </button>
              {suggestionReason && <p className="ai-reason">{suggestionReason}</p>}
              <button className="secondary" type="button" onClick={() => setForm({ ...form, pieceIds: [] })}>Vider selection</button>
            </aside>
            <div className="pack-groups">
              {ordreTypes.filter((type) => materielsParType[type]?.length).map((type) => {
                const open = sectionsOuvertes[type] ?? false;
                const selectedCount = materielsParType[type].filter((item) => form.pieceIds.includes(String(item.id))).length;
                return (
                  <section className="pack-group" key={type}>
                    <button className="pack-group-header" type="button" onClick={() => toggleSection(type)}>
                      <strong>{type}</strong>
                      <span>{selectedCount}/{materielsParType[type].length} selectionne(s)</span>
                      <em>{open ? 'Masquer' : 'Afficher'}</em>
                    </button>
                    {open && (
                      <div className="pack-materials grouped">
                        {materielsParType[type].map((item) => (
                          <label className={form.pieceIds.includes(String(item.id)) ? 'selected' : ''} key={item.id}>
                            <input
                              type="checkbox"
                              checked={form.pieceIds.includes(String(item.id))}
                              onChange={() => toggleMateriel(item.id)}
                            />
                            <span>{item.designation}</span>
                            <small>{item.reference} - stock {item.quantiteStock}</small>
                          </label>
                        ))}
                      </div>
                    )}
                  </section>
                );
              })}
            </div>
          </div>
          <div className="pack-submit">
            <div className="pack-selected-summary">
              <span>{form.pieceIds.length} materiel(s) selectionne(s)</span>
              <strong>{form.pieceIds.length === 0 ? 'Pack vide' : 'Pack pret a affecter'}</strong>
              <small>
                {form.pieceIds.length === 0
                  ? 'Selectionnez manuellement ou utilisez AI Pack.'
                  : form.pieceIds
                      .map((id) => materielsDisponibles.find((item) => String(item.id) === String(id))?.reference)
                      .filter(Boolean)
                      .join(' + ')}
              </small>
            </div>
            <button disabled={!form.employeId || form.pieceIds.length === 0}>Affecter le pack complet</button>
          </div>
        </form>
      )}
      {canRequestEquipement && (
        <form
          className="form-grid panel"
          onSubmit={(event) => {
            event.preventDefault();
            onAction(async () => {
              await api.demanderEquipement({
                pieceId: Number(demandePieceId),
                employeId: Number(session.employeId),
              });
              setDemandePieceId('');
            });
          }}
        >
          <Select
            label="Equipement demande"
            value={demandePieceId}
            onChange={setDemandePieceId}
            options={pcPieces.map((item) => [
              item.id,
              `${item.reference} - ${item.designation} (stock: ${item.quantiteStock})`,
            ])}
            placeholder="Choisir un equipement de remplacement"
            required
          />
          <button>Envoyer la demande</button>
        </form>
      )}
      <section className="panel">
        <div className="section-title">
          <h2>Inventaire</h2>
          <span>{rows.length} lignes</span>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Numero serie</th><th>Type</th><th>Pack</th><th>Marque</th><th>Modele</th><th>Employe</th><th>Statut</th><th>Photo</th><th>Upload</th></tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td>{row.numSerie}</td>
                  <td>{row.type}</td>
                  <td>{formatValue(row.packReference)}</td>
                  <td>{formatValue(row.marque)}</td>
                  <td>{formatValue(row.modele)}</td>
                  <td>{row.employe ? `${row.employe.nom} ${row.employe.prenom}` : '-'}</td>
                  <td><StatusBadge value={row.statut} /></td>
                  <td><PhotoPreview id={row.id} photoPath={row.photoPath} loader={api.equipementPhotoUrl} /></td>
                  <td><FileUpload onSelect={(file) => onAction(() => api.uploadEquipementPhoto(row.id, file))} /></td>
                </tr>
              ))}
              {rows.length === 0 && (
                <tr>
                  <td colSpan="9">
                    <EmptyState title="Aucun equipement" text="Ajoutez ou affectez du materiel pour alimenter l'inventaire." compact />
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}

function Pannes({ rows = [], equipements = [], utilisateurs = [], onAction, session }) {
  const [form, setForm] = useState({
    typeEquipement: '',
    equipementId: '',
    departementId: '',
    declarantId: '',
    description: '',
    urgence: 'MOYENNE',
  });
  const canPublish = session?.role === 'ADMIN';
  const canDeclarePanne = ['ADMIN', 'EMPLOYE'].includes(session?.role);
  const declarants = utilisateurs.filter((user) => user.actif && user.employe?.service);
  const departements = [...new Map(
    declarants.map((user) => [user.employe.service.id, user.employe.service])
  ).values()].sort((left, right) => left.nom.localeCompare(right.nom, 'fr', { sensitivity: 'base' }));
  const declarantsFiltres = form.departementId
    ? declarants.filter((user) => String(user.employe.service.id) === String(form.departementId))
    : [];
  const declarantSelectionne = utilisateurs.find((user) => String(user.id) === String(form.declarantId));
  const equipementsDeclarant = session?.role === 'ADMIN'
    ? equipements.filter((item) =>
        declarantSelectionne?.employe
        && String(item.employe?.id) === String(declarantSelectionne.employe.id)
        && item.statut !== 'EN_PANNE'
      )
    : equipements.filter((item) => item.statut !== 'EN_PANNE');
  const typesEquipement = [...new Set(
    equipementsDeclarant.map((item) => item.type?.trim()).filter(Boolean)
  )].sort((left, right) => left.localeCompare(right, 'fr', { sensitivity: 'base' }));
  const equipementsFiltres = form.typeEquipement
    ? equipementsDeclarant.filter((item) => item.type === form.typeEquipement)
    : [];

  function submit(event) {
    event.preventDefault();
    onAction(async () => {
      await api.createPanne({
        equipementId: Number(form.equipementId),
        declarantId: form.declarantId ? Number(form.declarantId) : null,
        description: form.description,
        urgence: form.urgence,
      });
      setForm({
        typeEquipement: '',
        equipementId: '',
        departementId: '',
        declarantId: '',
        description: '',
        urgence: 'MOYENNE',
      });
    });
  }

  return (
    <section className="stack">
      {canDeclarePanne && <form className="form-grid panel" onSubmit={submit}>
        {session?.role === 'ADMIN' && (
          <Select
            label="Departement"
            value={form.departementId}
            onChange={(departementId) => setForm({
              ...form,
              departementId,
              declarantId: '',
              typeEquipement: '',
              equipementId: '',
            })}
            options={departements.map((item) => [item.id, item.nom])}
            placeholder="Choisir un departement"
            required
          />
        )}
        {session?.role === 'ADMIN' && (
          <Select
            label="Employe declarant"
            value={form.declarantId}
            onChange={(declarantId) => setForm({
              ...form,
              declarantId,
              typeEquipement: '',
              equipementId: '',
            })}
            options={declarantsFiltres.map((item) => [
              item.id,
              `${item.employe.nom} ${item.employe.prenom} (${item.login})`,
            ])}
            placeholder={form.departementId ? 'Choisir un employe' : "Choisir d'abord le departement"}
            disabled={!form.departementId}
            required
          />
        )}
        <Select
          label="Type d'equipement"
          value={form.typeEquipement}
          onChange={(typeEquipement) => setForm({ ...form, typeEquipement, equipementId: '' })}
          options={typesEquipement}
          placeholder={equipementsDeclarant.length ? 'Choisir un type' : 'Aucun equipement affecte'}
          disabled={equipementsDeclarant.length === 0}
          required
        />
        <Select
          label="Equipement"
          value={form.equipementId}
          onChange={(equipementId) => setForm({ ...form, equipementId })}
          options={equipementsFiltres.map((item) => [
            item.id,
            `${item.numSerie} - ${item.marque ?? ''} ${item.modele ?? ''}`.trim(),
          ])}
          placeholder={form.typeEquipement ? "Choisir l'equipement" : "Choisir d'abord le type"}
          disabled={!form.typeEquipement}
          required
        />
        <Select label="Urgence" value={form.urgence} onChange={(urgence) => setForm({ ...form, urgence })} options={['HAUTE', 'MOYENNE', 'FAIBLE']} />
        <Field label="Description" value={form.description} onChange={(description) => setForm({ ...form, description })} required />
        <button>Declarer panne</button>
      </form>}
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
                  <td><StatusBadge value={panne.urgence} /></td>
                  <td><StatusBadge value={panne.statut} /></td>
                  <td>{formatValue(panne.technicien)}</td>
                  <td>
                    <div className="photo-cell">
                      <PhotoPreview id={panne.id} photoPath={panne.photoPath} loader={api.pannePhotoUrl} compact />
                      <FileUpload onSelect={(file) => onAction(() => api.uploadPannePhoto(panne.id, file))} />
                    </div>
                  </td>
                  <td className="row-actions">
                    {canPublish && panne.statut === 'DECLAREE' && !panne.technicien && (
                      <button onClick={() => onAction(() => api.publierPanne(panne.id))}>Envoyer aux techniciens</button>
                    )}
                    {session?.role === 'TECHNICIEN' && panne.statut === 'A_AFFECTER' && !panne.technicien && (
                      <button onClick={() => onAction(() => api.claimPanne(panne.id))}>Claim</button>
                    )}
                    {session?.role === 'TECHNICIEN'
                      && panne.technicien?.login === session?.login
                      && panne.statut === 'EN_COURS' && (
                      <button onClick={() => onAction(() => api.changerStatutPanne(panne.id, 'EN_ATTENTE_PIECE'))}>
                        Attente piece
                      </button>
                    )}
                    {session?.role === 'ADMIN' && panne.statut === 'REPAREE' && (
                      <button onClick={() => onAction(() => api.changerStatutPanne(panne.id, 'CLOTUREE'))}>
                        Cloturer la panne
                      </button>
                    )}
                  </td>
                </tr>
              ))}
              {rows.length === 0 && (
                <tr>
                  <td colSpan="7">
                    <EmptyState title="Aucune panne en cours" text="Les incidents declares apparaitront ici avec leur workflow." compact />
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}

function Notifications({ rows = [], onAction, session }) {
  const nonLues = rows.filter((item) => !item.lu).length;
  return (
    <section className="panel">
      <div className="section-title">
        <h2>Notifications</h2>
        <span>{nonLues} non lues</span>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr><th>Date</th><th>Titre</th><th>Message</th><th>Type</th><th>Statut</th><th>Action</th></tr>
          </thead>
          <tbody>
            {rows.map((notification) => (
              <tr key={notification.id}>
                <td>{formatValue(notification.dateCreation)}</td>
                <td>{notification.titre}</td>
                <td>{notification.message}</td>
                <td>{notification.type}</td>
                <td>
                  <StatusBadge value={notification.statut === 'NOUVELLE' || notification.statut === 'EN_COURS' ? 'PENDING' : notification.statut} />
                </td>
                <td className="row-actions">
                  {session?.role !== 'TECHNICIEN' && !notification.lu && (
                    <button onClick={() => onAction(() => api.marquerNotificationLue(notification.id))}>Marquer lue</button>
                  )}
                  {session?.role === 'TECHNICIEN'
                    && notification.type === 'EQUIPEMENT_AFFECTE'
                    && notification.statut === 'NOUVELLE' && (
                      <button onClick={() => onAction(() => api.affecterNotificationEquipement(notification.id))}>AFFECTER</button>
                  )}
                  {session?.role === 'TECHNICIEN'
                    && notification.type === 'EQUIPEMENT_AFFECTE'
                    && notification.statut === 'EN_COURS' && (
                      <button onClick={() => onAction(() => api.terminerNotificationEquipement(notification.id))}>DONE</button>
                  )}
                  {notification.statut === 'AFFECTEE' && 'Prise par un autre technicien'}
                  {notification.statut === 'DONE' && 'Terminee'}
                </td>
              </tr>
            ))}
            {rows.length === 0 && (
              <tr>
                <td colSpan="6">
                  <EmptyState title="Aucune notification" text="Les alertes et actions a traiter apparaitront ici." compact />
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function Stock({ rows = [], fournisseurs = [], onAction, session }) {
  const [approvisionnement, setApprovisionnement] = useState({
    mode: 'EXISTANTE',
    typePiece: '',
    fournisseurId: '',
    pieceId: '',
    quantite: 1,
    reference: '',
    designation: '',
    seuilMinimum: 0,
    localisation: '',
    nouvelleLocalisation: '',
    prixUnitaire: 0,
  });
  const [mouvement, setMouvement] = useState({
    typePiece: '',
    pieceId: '',
    quantite: 1,
    typeMouvement: 'ENTREE',
    motif: 'Ajustement stock',
    localisationDestination: '',
    nouvelleLocalisation: '',
  });
  const canCreatePiece = ['ADMIN', 'TECHNICIEN'].includes(session?.role);
  const materielRows = rows.filter((item) => item.usage === 'MATERIEL');
  const rechangeRows = rows.filter((item) => item.usage !== 'MATERIEL');
  const criticalRows = rows.filter((item) => Number(item.quantiteStock || 0) < Number(item.seuilMinimum || 0));
  const stockValue = rows.reduce((total, item) =>
    total + (Number(item.quantiteStock || 0) * Number(item.prixUnitaire || 0)), 0);
  const typesPieces = [...new Set(
    rows
      .map((item) => item.reference?.split('-')[0]?.toUpperCase())
      .filter(Boolean)
  )].sort((left, right) => left.localeCompare(right, 'fr', { sensitivity: 'base' }));
  const fournisseursApprovisionnement = approvisionnement.typePiece
    ? fournisseurs.filter((fournisseur) => rows.some((item) =>
        item.reference?.toUpperCase().startsWith(`${approvisionnement.typePiece}-`)
        && item.fournisseur?.id === fournisseur.id
      ))
    : [];
  const piecesApprovisionnement = approvisionnement.typePiece && approvisionnement.fournisseurId
    ? rows.filter((item) =>
        item.reference?.toUpperCase().startsWith(`${approvisionnement.typePiece}-`)
        && String(item.fournisseur?.id) === String(approvisionnement.fournisseurId)
      )
    : [];
  const piecesMouvement = mouvement.typePiece
    ? rows.filter((item) => item.reference?.toUpperCase().startsWith(`${mouvement.typePiece}-`))
    : [];
  const pieceMouvement = rows.find((item) => String(item.id) === String(mouvement.pieceId));
  const pieceApprovisionnement = rows.find((item) => String(item.id) === String(approvisionnement.pieceId));
  const localisations = [...new Set(rows.map((item) => item.localisation).filter(Boolean))]
    .sort((left, right) => left.localeCompare(right, 'fr', { sensitivity: 'base' }));
  const localisationsDestination = localisations.filter((item) =>
    item.toLowerCase() !== pieceMouvement?.localisation?.toLowerCase()
  );
  const motifsMouvement = [
    'Ajustement stock',
    'Reception commande',
    'Retour materiel',
    'Affectation equipement',
    'Consommation reparation',
    'Correction inventaire',
    'Transfert de stock',
  ];

  function submitApprovisionnement(event) {
    event.preventDefault();
    onAction(async () => {
      if (approvisionnement.mode === 'NOUVELLE') {
        const localisation = approvisionnement.localisation === '__NEW__'
          ? approvisionnement.nouvelleLocalisation
          : approvisionnement.localisation;
        await api.createPiece({
          reference: approvisionnement.reference,
          designation: approvisionnement.designation,
          quantiteStock: Number(approvisionnement.quantite),
          seuilMinimum: Number(approvisionnement.seuilMinimum),
          localisation,
          prixUnitaire: Number(approvisionnement.prixUnitaire),
          fournisseur: approvisionnement.fournisseurId
            ? { id: Number(approvisionnement.fournisseurId) }
            : null,
        });
      } else {
        await api.mouvementStock({
          pieceId: Number(approvisionnement.pieceId),
          quantite: Number(approvisionnement.quantite),
          typeMouvement: 'ENTREE',
          motif: 'Reception commande',
        });
      }
      setApprovisionnement({
        mode: 'EXISTANTE',
        typePiece: '',
        fournisseurId: '',
        pieceId: '',
        quantite: 1,
        reference: '',
        designation: '',
        seuilMinimum: 0,
        localisation: '',
        nouvelleLocalisation: '',
        prixUnitaire: 0,
      });
    });
  }

  function submitMouvement(event) {
    event.preventDefault();
    onAction(async () => {
      const nouvelleLocalisation = mouvement.localisationDestination === '__NEW__'
        ? mouvement.nouvelleLocalisation
        : mouvement.localisationDestination;
      await api.mouvementStock({
        pieceId: Number(mouvement.pieceId),
        quantite: mouvement.typeMouvement === 'TRANSFERT' ? 0 : Number(mouvement.quantite),
        typeMouvement: mouvement.typeMouvement,
        motif: mouvement.motif,
        nouvelleLocalisation: mouvement.typeMouvement === 'TRANSFERT' ? nouvelleLocalisation : null,
      });
      setMouvement({
        typePiece: '',
        pieceId: '',
        quantite: 1,
        typeMouvement: 'ENTREE',
        motif: 'Ajustement stock',
        localisationDestination: '',
        nouvelleLocalisation: '',
      });
    });
  }

  return (
    <section className="stack stock-page">
      <section className="stock-analytics">
        <article className="card metric-card blue">
          <div className="metric-icon">▦</div>
          <span>References</span>
          <strong>{rows.length}</strong>
          <small>Materiel + pieces</small>
        </article>
        <article className="card metric-card green">
          <div className="metric-icon">▣</div>
          <span>Materiel affectable</span>
          <strong>{materielRows.length}</strong>
          <small>Pour packs collaborateurs</small>
        </article>
        <article className="card metric-card purple">
          <div className="metric-icon">◇</div>
          <span>Pieces rechange</span>
          <strong>{rechangeRows.length}</strong>
          <small>Pour reparations</small>
        </article>
        <article className={`card metric-card ${criticalRows.length ? 'orange' : 'green'}`}>
          <div className="metric-icon">△</div>
          <span>Alertes stock</span>
          <strong>{criticalRows.length}</strong>
          <small>{criticalRows.length ? 'Sous seuil minimum' : 'Stock stable'}</small>
        </article>
        <article className="card metric-card blue stock-value-card">
          <div className="metric-icon">MAD</div>
          <span>Valeur stock</span>
          <strong>{formatNumber(stockValue)} DH</strong>
          <small>Estimation inventaire</small>
        </article>
      </section>
      {canCreatePiece && <form className="form-grid panel" onSubmit={submitApprovisionnement}>
        <Select
          label="Operation"
          value={approvisionnement.mode}
          onChange={(mode) => setApprovisionnement({
            ...approvisionnement,
            mode,
            typePiece: '',
            fournisseurId: '',
            pieceId: '',
          })}
          options={[
            ['EXISTANTE', 'Ajouter une piece existante'],
            ['NOUVELLE', 'Creer une nouvelle reference'],
          ]}
          required
        />
        {approvisionnement.mode === 'EXISTANTE' && <>
        <Select
          label="Type d'equipement"
          value={approvisionnement.typePiece}
          onChange={(typePiece) => setApprovisionnement({
            ...approvisionnement,
            typePiece,
            fournisseurId: '',
            pieceId: '',
          })}
          options={typesPieces}
          placeholder="Choisir un type"
          required
        />
        <Select
          label="Fournisseur"
          value={approvisionnement.fournisseurId}
          onChange={(fournisseurId) => setApprovisionnement({
            ...approvisionnement,
            fournisseurId,
            pieceId: '',
          })}
          options={fournisseursApprovisionnement.map((item) => [item.id, item.nom])}
          placeholder={approvisionnement.typePiece ? 'Choisir un fournisseur' : "Choisir d'abord le type"}
          disabled={!approvisionnement.typePiece}
          required
        />
        <Select
          label="Reference / designation"
          value={approvisionnement.pieceId}
          onChange={(pieceId) => setApprovisionnement({ ...approvisionnement, pieceId })}
          options={piecesApprovisionnement.map((item) => [
            item.id,
            `${item.reference} - ${item.designation} (stock: ${item.quantiteStock})`,
          ])}
          placeholder={approvisionnement.fournisseurId ? 'Choisir une piece' : "Choisir d'abord le fournisseur"}
          disabled={!approvisionnement.fournisseurId}
          required
        />
        {pieceApprovisionnement && <PieceLine piece={pieceApprovisionnement} />}
        </>}
        {approvisionnement.mode === 'NOUVELLE' && <>
          <Field
            label="Nouvelle reference"
            value={approvisionnement.reference}
            onChange={(reference) => setApprovisionnement({ ...approvisionnement, reference: reference.toUpperCase() })}
            placeholder="Ex: RAM-16G-DDR5"
            required
          />
          <Field
            label="Nouvelle designation"
            value={approvisionnement.designation}
            onChange={(designation) => setApprovisionnement({ ...approvisionnement, designation })}
            placeholder="Ex: Barrette RAM 16GB DDR5"
            required
          />
          <Select
            label="Fournisseur"
            value={approvisionnement.fournisseurId}
            onChange={(fournisseurId) => setApprovisionnement({ ...approvisionnement, fournisseurId })}
            options={fournisseurs.map((item) => [item.id, item.nom])}
            placeholder="Choisir un fournisseur"
            required
          />
          <Select
            label="Localisation"
            value={approvisionnement.localisation}
            onChange={(localisation) => setApprovisionnement({
              ...approvisionnement,
              localisation,
              nouvelleLocalisation: '',
            })}
            options={[...localisations, ['__NEW__', 'Nouvelle localisation...']]}
            placeholder="Choisir une localisation"
            required
          />
          {approvisionnement.localisation === '__NEW__' && (
            <Field
              label="Nouvelle localisation"
              value={approvisionnement.nouvelleLocalisation}
              onChange={(nouvelleLocalisation) => setApprovisionnement({ ...approvisionnement, nouvelleLocalisation })}
              placeholder="Ex: Armoire C2"
              required
            />
          )}
          <Field
            label="Seuil minimum"
            type="number"
            value={approvisionnement.seuilMinimum}
            onChange={(seuilMinimum) => setApprovisionnement({ ...approvisionnement, seuilMinimum })}
            required
          />
          <Field
            label="Prix unitaire"
            type="number"
            value={approvisionnement.prixUnitaire}
            onChange={(prixUnitaire) => setApprovisionnement({ ...approvisionnement, prixUnitaire })}
            required
          />
        </>}
        <Field
          label={approvisionnement.mode === 'NOUVELLE' ? 'Quantite initiale' : 'Quantite a ajouter'}
          type="number"
          value={approvisionnement.quantite}
          onChange={(quantite) => setApprovisionnement({ ...approvisionnement, quantite })}
          required
        />
        <button>{approvisionnement.mode === 'NOUVELLE' ? 'Creer la piece' : 'Ajouter au stock'}</button>
      </form>}
      <form className="form-grid panel" onSubmit={submitMouvement}>
        <Select
          label="Type d'equipement"
          value={mouvement.typePiece}
          onChange={(typePiece) => setMouvement({ ...mouvement, typePiece, pieceId: '' })}
          options={typesPieces}
          placeholder="Choisir un type"
          required
        />
        <Select
          label="Piece"
          value={mouvement.pieceId}
          onChange={(pieceId) => setMouvement({
            ...mouvement,
            pieceId,
            localisationDestination: '',
            nouvelleLocalisation: '',
          })}
          options={piecesMouvement.map((item) => [
            item.id,
            `${item.reference} - ${item.designation} (stock: ${item.quantiteStock})`,
          ])}
          placeholder={mouvement.typePiece ? 'Choisir une piece' : "Choisir d'abord le type"}
          disabled={!mouvement.typePiece}
          required
        />
        {pieceMouvement && <PieceLine piece={pieceMouvement} />}
        <Select
          label="Mouvement"
          value={mouvement.typeMouvement}
          onChange={(typeMouvement) => setMouvement({
            ...mouvement,
            typeMouvement,
            motif: typeMouvement === 'TRANSFERT' ? 'Transfert de stock' : mouvement.motif,
            localisationDestination: '',
            nouvelleLocalisation: '',
          })}
          options={['ENTREE', 'SORTIE', 'CONSOMMATION', 'TRANSFERT']}
        />
        {mouvement.typeMouvement !== 'TRANSFERT' && (
          <Field label="Quantite" type="number" value={mouvement.quantite} onChange={(quantite) => setMouvement({ ...mouvement, quantite })} required />
        )}
        {mouvement.typeMouvement === 'TRANSFERT' && <>
          <Field label="Localisation actuelle" value={pieceMouvement?.localisation ?? '-'} disabled />
          <Select
            label="Nouvelle localisation"
            value={mouvement.localisationDestination}
            onChange={(localisationDestination) => setMouvement({
              ...mouvement,
              localisationDestination,
              nouvelleLocalisation: '',
            })}
            options={[...localisationsDestination, ['__NEW__', 'Nouvelle localisation...']]}
            placeholder={mouvement.pieceId ? 'Choisir la destination' : "Choisir d'abord la piece"}
            disabled={!mouvement.pieceId}
            required
          />
          {mouvement.localisationDestination === '__NEW__' && (
            <Field
              label="Saisir la destination"
              value={mouvement.nouvelleLocalisation}
              onChange={(nouvelleLocalisation) => setMouvement({ ...mouvement, nouvelleLocalisation })}
              placeholder="Ex: Casier R4"
              required
            />
          )}
        </>}
        <Select label="Motif" value={mouvement.motif} onChange={(motif) => setMouvement({ ...mouvement, motif })} options={motifsMouvement} required />
        <button>Enregistrer mouvement</button>
      </form>
      <StockInventory rows={rows} />
    </section>
  );
}

function Reparations({ rows = [], pannes = [], pieces = [], utilisateurs = [], onAction, session }) {
  const [form, setForm] = useState({ panneId: '', technicienId: '', description: '' });
  const [executions, setExecutions] = useState({});
  const [filter, setFilter] = useState('TOUTES');
  const canCreateReparation = session?.role === 'ADMIN';
  const panneIdsEnCours = new Set(rows.filter((item) => !item.dateFin).map((item) => item.panne?.id));
  const pannesDisponibles = pannes.filter((item) =>
    !['REPAREE', 'CLOTUREE'].includes(item.statut) && !panneIdsEnCours.has(item.id)
  );
  const techniciens = utilisateurs.filter((item) => item.role === 'TECHNICIEN' && item.actif);
  const piecesRechange = pieces.filter((item) => item.usage !== 'MATERIEL');
  const interventionsOuvertes = rows.filter((item) => !item.dateFin);
  const interventionsAttente = rows.filter((item) => !item.dateFin && item.panne?.statut === 'EN_ATTENTE_PIECE');
  const interventionsTerminees = rows.filter((item) => item.dateFin);
  const coutTotal = rows.reduce((total, item) => total + Number(item.coutTotal || 0), 0);
  const rowsFiltres = [...rows]
    .filter((item) => {
      if (filter === 'OUVERTES') return !item.dateFin && item.panne?.statut !== 'EN_ATTENTE_PIECE';
      if (filter === 'ATTENTE') return !item.dateFin && item.panne?.statut === 'EN_ATTENTE_PIECE';
      if (filter === 'TERMINEES') return Boolean(item.dateFin);
      return true;
    })
    .sort((left, right) => {
      if (Boolean(left.dateFin) !== Boolean(right.dateFin)) return left.dateFin ? 1 : -1;
      return new Date(right.dateDebut || 0) - new Date(left.dateDebut || 0);
    });

  function submit(event) {
    event.preventDefault();
    onAction(async () => {
      await api.createReparation({
        panneId: Number(form.panneId),
        technicienId: Number(form.technicienId),
        description: form.description,
        coutTotal: 0,
        piecesConsommees: [],
      });
      setForm({ panneId: '', technicienId: '', description: '' });
    });
  }

  function executionFor(id) {
    const row = rows.find((item) => item.id === id);
    return executions[id] ?? {
      diagnostic: row?.diagnostic ?? '',
      coutTotal: row?.coutTotal ?? 0,
      pieceId: '',
      quantite: 1,
    };
  }

  function updateExecution(id, changes) {
    setExecutions({ ...executions, [id]: { ...executionFor(id), ...changes } });
  }

  function executer(row, resultat) {
    const execution = executionFor(row.id);
    if (!execution.diagnostic.trim()) {
      onAction(() => Promise.reject(new Error('Le diagnostic du technicien est obligatoire.')));
      return;
    }
    if (execution.pieceId && Number(execution.quantite) < 1) {
      onAction(() => Promise.reject(new Error('La quantite doit etre superieure a zero.')));
      return;
    }
    if (resultat === 'EN_ATTENTE_PIECE' && !execution.pieceId) {
      onAction(() => Promise.reject(new Error("Selectionnez la piece necessaire avant de signaler l'attente.")));
      return;
    }
    onAction(() => api.executerReparation(row.id, {
      diagnostic: execution.diagnostic,
      coutTotal: Number(execution.coutTotal),
      piecesConsommees: execution.pieceId
        ? [{ pieceId: Number(execution.pieceId), quantite: Number(execution.quantite) }]
        : [],
      resultat,
    }));
  }

  function statutIntervention(row) {
    if (row.dateFin) return { label: 'Terminee', tone: 'done' };
    if (row.panne?.statut === 'EN_ATTENTE_PIECE') return { label: 'En attente de piece', tone: 'waiting' };
    return { label: 'En cours', tone: 'active' };
  }

  function dateCourte(value) {
    if (!value) return '-';
    return new Intl.DateTimeFormat('fr-FR', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(value));
  }

  return (
    <section className="stack repairs-page">
      <section className="repair-summary">
        <article><span>En cours</span><strong>{interventionsOuvertes.length - interventionsAttente.length}</strong></article>
        <article><span>Attente piece</span><strong>{interventionsAttente.length}</strong></article>
        <article><span>Terminees</span><strong>{interventionsTerminees.length}</strong></article>
        <article><span>Cout total</span><strong>{coutTotal.toFixed(2)} DH</strong></article>
      </section>

      {canCreateReparation && (
        <section className="panel repair-assignment">
          <div className="section-title">
            <div>
              <span className="eyebrow">Planification</span>
              <h2>Affecter une intervention</h2>
              <p>Selectionnez une panne ouverte, le technicien responsable et la consigne de travail.</p>
            </div>
          </div>
          <form className="form-grid" onSubmit={submit}>
            <Select
              label="Panne a traiter"
              value={form.panneId}
              onChange={(panneId) => setForm({ ...form, panneId })}
              options={pannesDisponibles.map((item) => [
                item.id,
                `#${item.id} - ${item.equipement?.type ?? 'Equipement'} - ${item.description}`,
              ])}
              placeholder={pannesDisponibles.length ? 'Choisir une panne' : 'Aucune panne disponible'}
              disabled={pannesDisponibles.length === 0}
              required
            />
            <Select
              label="Technicien responsable"
              value={form.technicienId}
              onChange={(technicienId) => setForm({ ...form, technicienId })}
              options={techniciens.map((item) => [item.id, item.login])}
              placeholder="Choisir un technicien"
              required
            />
            <Field
              label="Consigne d'intervention"
              value={form.description}
              onChange={(description) => setForm({ ...form, description })}
              placeholder="Ex: verifier alimentation et connectique"
              required
            />
            <button type="submit">Affecter au technicien</button>
          </form>
        </section>
      )}

      <section className="panel">
        <div className="repair-list-header">
          <div>
            <span className="eyebrow">Suivi operationnel</span>
            <h2>{session?.role === 'TECHNICIEN' ? 'Mes interventions' : 'Interventions'}</h2>
            <p>{rowsFiltres.length} intervention(s) affichee(s)</p>
          </div>
          <div className="repair-filters">
            {[
              ['TOUTES', 'Toutes'],
              ['OUVERTES', 'En cours'],
              ['ATTENTE', 'Attente piece'],
              ['TERMINEES', 'Terminees'],
            ].map(([value, label]) => (
              <button
                className={filter === value ? 'active' : ''}
                key={value}
                onClick={() => setFilter(value)}
                type="button"
              >
                {label}
              </button>
            ))}
          </div>
        </div>

        <div className="repair-cards">
          {rowsFiltres.map((row) => {
            const statut = statutIntervention(row);
            const canExecute = session?.role === 'TECHNICIEN'
              && row.technicien?.login === session.login
              && !row.dateFin;
            const execution = executionFor(row.id);
            const selectedPiece = piecesRechange.find((item) => String(item.id) === String(execution.pieceId));
            return (
              <article className="repair-card" key={row.id}>
                <header>
                  <div>
                    <span className="repair-id">Intervention #{row.id}</span>
                    <h3>Panne #{row.panne?.id ?? '-'}</h3>
                    <p>{row.panne?.equipement
                      ? `${row.panne.equipement.type} · ${row.panne.equipement.numSerie}`
                      : row.panne?.description ?? 'Equipement non renseigne'}</p>
                  </div>
                  <span className={`status-pill ${statut.tone}`}>{statut.label}</span>
                </header>

                <div className="repair-meta">
                  <div><span>Technicien</span><strong>{formatValue(row.technicien)}</strong></div>
                  <div><span>Debut</span><strong>{dateCourte(row.dateDebut)}</strong></div>
                  <div><span>Fin</span><strong>{dateCourte(row.dateFin)}</strong></div>
                  <div><span>Cout</span><strong>{Number(row.coutTotal || 0).toFixed(2)} DH</strong></div>
                </div>

                <div className="repair-notes">
                  <div>
                    <span>Consigne Admin</span>
                    <p>{row.description || 'Aucune consigne renseignee.'}</p>
                  </div>
                  <div>
                    <span>Diagnostic Technicien</span>
                    <p>{row.diagnostic || (row.dateFin ? 'Non renseigne.' : 'En attente du diagnostic.')}</p>
                  </div>
                </div>

                {canExecute && (
                  <div className="repair-workspace">
                    <div className="workspace-heading">
                      <div>
                        <strong>Compte rendu technicien</strong>
                        <span>Renseignez le diagnostic avant de mettre a jour le resultat.</span>
                      </div>
                    </div>
                    <div className="repair-execution-form">
                      <Field
                        label="Diagnostic / travaux realises"
                        value={execution.diagnostic}
                        onChange={(diagnostic) => updateExecution(row.id, { diagnostic })}
                        placeholder="Cause identifiee et intervention realisee"
                      />
                      <Field
                        label="Cout total (DH)"
                        type="number"
                        value={execution.coutTotal}
                        onChange={(coutTotal) => updateExecution(row.id, { coutTotal })}
                      />
                      <Select
                        label="Piece concernee (optionnel)"
                        value={execution.pieceId}
                        onChange={(pieceId) => updateExecution(row.id, { pieceId })}
                        options={piecesRechange.map((item) => [
                          item.id,
                          `${item.reference} - ${item.designation} · stock ${item.quantiteStock}`,
                        ])}
                        placeholder="Aucune piece"
                      />
                      {selectedPiece && <PieceLine piece={selectedPiece} />}
                      <Field
                        label="Quantite"
                        type="number"
                        value={execution.quantite}
                        onChange={(quantite) => updateExecution(row.id, { quantite })}
                        disabled={!execution.pieceId}
                      />
                    </div>
                    <div className="repair-actions">
                      <button className="secondary" type="button" onClick={() => executer(row, 'EN_ATTENTE_PIECE')}>
                        Signaler attente piece
                      </button>
                      <button type="button" onClick={() => executer(row, 'REPAREE')}>
                        Confirmer reparation
                      </button>
                    </div>
                  </div>
                )}

                <footer>
                  <button className="pdf-button" type="button" onClick={() => onAction(() => api.downloadReparationReport(row.id))}>
                    Telecharger le bon PDF
                  </button>
                </footer>
              </article>
            );
          })}
          {rowsFiltres.length === 0 && (
            <div className="repair-empty">
              <strong>Aucune intervention dans cette categorie</strong>
              <span>Les nouvelles interventions apparaitront ici.</span>
            </div>
          )}
        </div>
      </section>
    </section>
  );
}

function Prets({ rows = [], equipements = [], utilisateurs = [], onAction, session }) {
  const employes = utilisateurs.filter((user) => user.employe).map((user) => user.employe);
  const [form, setForm] = useState({
    typeEquipement: '',
    equipementId: '',
    departementId: '',
    employeId: '',
    dateRetourPrevue: '',
    motif: '',
  });
  const [prolongations, setProlongations] = useState({});
  const canManagePrets = session?.role === 'ADMIN';
  const equipementsReserves = new Set(
    rows
      .filter((pret) => ['EN_ATTENTE', 'VALIDE', 'EN_RETARD'].includes(pret.statut))
      .map((pret) => pret.equipement?.id)
      .filter(Boolean)
  );
  const equipementsDisponibles = equipements.filter((item) =>
    item.statut === 'DISPONIBLE' && !equipementsReserves.has(item.id)
  );
  const typesEquipement = [...new Set(
    equipementsDisponibles.map((item) => item.type?.trim()).filter(Boolean)
  )].sort((left, right) => left.localeCompare(right, 'fr', { sensitivity: 'base' }));
  const equipementsFiltres = form.typeEquipement
    ? equipementsDisponibles.filter((item) => item.type === form.typeEquipement)
    : [];
  const departements = [...new Map(
    employes
      .filter((item) => item.service)
      .map((item) => [item.service.id, item.service])
  ).values()].sort((left, right) => left.nom.localeCompare(right.nom, 'fr', { sensitivity: 'base' }));
  const employesFiltres = form.departementId
    ? employes.filter((item) => String(item.service?.id) === String(form.departementId))
    : [];
  const motifsPret = [
    'Mission professionnelle',
    'Teletravail',
    'Remplacement temporaire',
    'Formation',
    'Projet interne',
    'Besoin exceptionnel',
  ];

  function submit(event) {
    event.preventDefault();
    onAction(async () => {
      await api.createPret({
        ...form,
        equipementId: Number(form.equipementId),
        employeId: Number(session?.role === 'EMPLOYE' ? session.employeId : form.employeId),
      });
      setForm({
        typeEquipement: '',
        equipementId: '',
        departementId: '',
        employeId: '',
        dateRetourPrevue: '',
        motif: '',
      });
    });
  }

  return (
    <section className="stack">
      <form className="form-grid panel" onSubmit={submit}>
        <Select
          label="Type d'equipement"
          value={form.typeEquipement}
          onChange={(typeEquipement) => setForm({ ...form, typeEquipement, equipementId: '' })}
          options={typesEquipement}
          placeholder="Choisir un type"
          required
        />
        <Select
          label="Equipement disponible"
          value={form.equipementId}
          onChange={(equipementId) => setForm({ ...form, equipementId })}
          options={equipementsFiltres.map((item) => [
            item.id,
            [item.numSerie, item.marque, item.modele].filter(Boolean).join(' - '),
          ])}
          placeholder={form.typeEquipement ? 'Choisir un equipement' : "Choisir d'abord le type"}
          disabled={!form.typeEquipement}
          required
        />
        {session?.role === 'ADMIN' && (
          <Select
            label="Departement"
            value={form.departementId}
            onChange={(departementId) => setForm({ ...form, departementId, employeId: '' })}
            options={departements.map((item) => [item.id, item.nom])}
            placeholder="Choisir un departement"
            required
          />
        )}
        {session?.role === 'ADMIN' && (
          <Select
            label="Employe"
            value={form.employeId}
            onChange={(employeId) => setForm({ ...form, employeId })}
            options={employesFiltres.map((item) => [item.id, `${item.nom} ${item.prenom}`])}
            placeholder={form.departementId ? 'Choisir un employe' : "Choisir d'abord le departement"}
            disabled={!form.departementId}
            required
          />
        )}
        <Field label="Retour prevu" type="date" value={form.dateRetourPrevue} onChange={(dateRetourPrevue) => setForm({ ...form, dateRetourPrevue })} required />
        <Select label="Motif" value={form.motif} onChange={(motif) => setForm({ ...form, motif })} options={motifsPret} placeholder="Choisir un motif" required />
        <button>Demander pret</button>
      </form>
      <section className="panel">
        <div className="section-title"><h2>Prets</h2><span>{rows.length} lignes</span></div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>ID</th><th>Equipement</th><th>Employe</th><th>Departement</th><th>Motif</th>
                <th>Depart</th><th>Retour prevu</th><th>Retour reel</th><th>Statut</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td>{row.id}</td>
                  <td>{row.equipement ? `${row.equipement.numSerie} - ${row.equipement.type}` : '-'}</td>
                  <td>{row.employe ? `${row.employe.nom} ${row.employe.prenom}` : '-'}</td>
                  <td>{formatValue(row.employe?.service?.nom)}</td>
                  <td>{formatValue(row.motif)}</td>
                  <td>{formatValue(row.dateDepart)}</td>
                  <td>{formatValue(row.dateRetourPrevue)}</td>
                  <td>{formatValue(row.dateRetourReelle)}</td>
                  <td><StatusBadge value={row.statut} /></td>
                  <td className="row-actions">
                    {canManagePrets && row.statut === 'EN_ATTENTE' && <button onClick={() => onAction(() => api.validerPret(row.id))}>Valider</button>}
                    {canManagePrets && row.statut === 'EN_ATTENTE' && <button onClick={() => onAction(() => api.refuserPret(row.id))}>Refuser</button>}
                    {canManagePrets && (row.statut === 'VALIDE' || row.statut === 'EN_RETARD') && <button onClick={() => onAction(() => api.cloturerPret(row.id))}>Retour</button>}
                    {canManagePrets && (row.statut === 'VALIDE' || row.statut === 'EN_RETARD') && (
                      <>
                        <input type="date" value={prolongations[row.id] ?? ''} onChange={(event) => setProlongations({ ...prolongations, [row.id]: event.target.value })} />
                        <button onClick={() => onAction(() => api.prolongerPret(row.id, prolongations[row.id]))}>Prolonger</button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
              {rows.length === 0 && (
                <tr>
                  <td colSpan="10">
                    <EmptyState title="Aucun pret" text="Les demandes de pret apparaitront ici apres creation." compact />
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}

function Utilisateurs({ rows = [], employes = [], onAction }) {
  const [employeId, setEmployeId] = useState('');
  const [credentials, setCredentials] = useState(null);
  const [searchEmploye, setSearchEmploye] = useState('');
  const [busyAction, setBusyAction] = useState('');
  const [toast, setToast] = useState(null);
  const employesAvecCompte = new Set(rows.map((user) => user.employe?.id).filter(Boolean));
  const employesDisponibles = employes.filter((employe) => !employesAvecCompte.has(employe.id));
  const employesFiltres = employesDisponibles
    .filter((employe) => {
      const query = normalizeSearch(searchEmploye);
      if (!query) return true;
      return normalizeSearch([
        employe.nom,
        employe.prenom,
        employe.email,
        employe.poste,
        employe.service?.nom,
      ].filter(Boolean).join(' ')).includes(query);
    })
    .sort((left, right) => `${left.nom} ${left.prenom}`.localeCompare(`${right.nom} ${right.prenom}`, 'fr', { sensitivity: 'base' }));
  const selectedEmploye = employesDisponibles.find((employe) => String(employe.id) === String(employeId));

  function showToast(kind, message) {
    setToast({ kind, message });
    window.setTimeout(() => setToast(null), 4500);
  }

  async function submit(event) {
    event.preventDefault();
    setBusyAction('create');
    const created = await onAction(() => api.createUtilisateur({ employeId: Number(employeId) }));
    if (created) {
      setCredentials(created);
      setEmployeId('');
      setSearchEmploye('');
      showToast(
        created.emailEnvoye ? 'success' : 'warning',
        created.emailEnvoye
          ? `Email envoye a ${created.email}.`
          : "Compte genere, mais l'email n'a pas ete envoye. Copiez les identifiants."
      );
    }
    setBusyAction('');
  }

  async function resetPassword(user) {
    setBusyAction(`reset-${user.id}`);
    const generated = await onAction(() => api.resetPasswordUtilisateur(user.id));
    if (generated) {
      setCredentials(generated);
      showToast(
        generated.emailEnvoye ? 'success' : 'warning',
        generated.emailEnvoye
          ? `Nouveau mot de passe envoye a ${generated.email}.`
          : "Mot de passe genere, mais l'email n'a pas ete envoye. Copiez-le maintenant."
      );
    }
    setBusyAction('');
  }

  async function copyText(value) {
    await navigator.clipboard.writeText(value);
    showToast('success', 'Copie dans le presse-papiers.');
  }

  function credentialsMessage() {
    if (!credentials) return '';
    return `Bonjour ${credentials.employe},\n\nVotre compte PT17 est pret.\nLogin : ${credentials.login}\nMot de passe temporaire : ${credentials.motDePasseTemporaire}\n\nMerci de garder ces informations confidentielles.`;
  }

  return (
    <section className="stack users-page">
      {toast && <div className={`toast ${toast.kind}`}>{toast.message}</div>}
      <form className="panel account-create" onSubmit={submit}>
        <div>
          <span className="eyebrow">Nouveau collaborateur</span>
          <h2>Creer un acces employe</h2>
          <p>Selectionnez l'employe. Le login et le mot de passe temporaire seront generes automatiquement.</p>
        </div>
        <div className="employee-search">
          <label>
            Recherche employe
            <input
              value={searchEmploye}
              onChange={(event) => setSearchEmploye(event.target.value)}
              placeholder="Nom, prenom, email, departement..."
            />
          </label>
          <span>{employesFiltres.length} resultat(s)</span>
        </div>
        <div className="employee-picker">
          {employesFiltres.slice(0, 8).map((employe) => (
            <button
              className={String(employe.id) === String(employeId) ? 'selected' : ''}
              key={employe.id}
              type="button"
              onClick={() => setEmployeId(String(employe.id))}
            >
              <strong>{employe.prenom} {employe.nom}</strong>
              <span>{employe.email || 'Email manquant'}</span>
              <small>{employe.service?.nom ?? 'Sans departement'} - {employe.poste ?? 'Poste non renseigne'}</small>
            </button>
          ))}
          {employesFiltres.length === 0 && (
            <div className="employee-empty">Aucun employe sans compte ne correspond a cette recherche.</div>
          )}
        </div>
        <Select
          label="Employe sans compte"
          value={employeId}
          onChange={setEmployeId}
          options={employesDisponibles.map((employe) => [
            employe.id,
            `${employe.nom} ${employe.prenom} · ${employe.service?.nom ?? 'Sans departement'}`,
          ])}
          placeholder={employesDisponibles.length ? 'Choisir un employe' : 'Tous les employes ont un compte'}
          disabled={employesDisponibles.length === 0}
          required
        />
        {selectedEmploye && (
          <div className="selected-employee">
            <span>Selection</span>
            <strong>{selectedEmploye.prenom} {selectedEmploye.nom}</strong>
            <small>{selectedEmploye.email || 'Attention: email manquant'}</small>
          </div>
        )}
        <button disabled={!employeId || busyAction === 'create'}>
          {busyAction === 'create' ? 'Generation + envoi email...' : 'Generer le compte et envoyer email'}
        </button>
      </form>

      {credentials && (
        <section className={`credential-card ${credentials.emailEnvoye ? 'email-sent' : 'email-pending'}`}>
          <div className="credential-heading">
            <div>
              <span className="eyebrow">Identifiants generes</span>
              <h2>{credentials.employe}</h2>
              <p>
                {credentials.emailEnvoye
                  ? `Email envoye avec succes a ${credentials.email}.`
                  : "L'email n'a pas pu etre envoye. Copiez les identifiants maintenant."}
              </p>
            </div>
            <button className="credential-close" type="button" onClick={() => setCredentials(null)}>Fermer</button>
          </div>
          <div className="credential-values">
            <div>
              <span>Login</span>
              <strong>{credentials.login}</strong>
              <button type="button" onClick={() => copyText(credentials.login)}>Copier</button>
            </div>
            <div>
              <span>Mot de passe temporaire</span>
              <strong>{credentials.motDePasseTemporaire}</strong>
              <button type="button" onClick={() => copyText(credentials.motDePasseTemporaire)}>Copier</button>
            </div>
          </div>
          <button className="copy-message" type="button" onClick={() => copyText(credentialsMessage())}>
            Copier le message complet
          </button>
        </section>
      )}

      <section className="panel">
        <div className="section-title"><h2>Comptes actifs et historiques</h2><span>{rows.length} comptes</span></div>
        <div className="table-wrap">
          <table>
            <thead><tr><th>Employe</th><th>Login</th><th>Email</th><th>Role</th><th>Actif</th><th>Echecs</th><th>Actions</th></tr></thead>
            <tbody>
              {rows.map((user) => (
                <tr key={user.id}>
                  <td>{user.employe ? `${user.employe.nom} ${user.employe.prenom}` : '-'}</td>
                  <td>{user.login}</td>
                  <td>{formatValue(user.email)}</td>
                  <td>{user.role}</td>
                  <td>{formatValue(user.actif)}</td>
                  <td>{user.tentativesEchec}</td>
                  <td className="row-actions">
                    {user.actif
                      ? <button disabled={Boolean(busyAction)} onClick={() => onAction(() => api.desactiverUtilisateur(user.id))}>Desactiver</button>
                      : <button disabled={Boolean(busyAction)} onClick={() => onAction(() => api.reactiverUtilisateur(user.id))}>Reactiver</button>}
                    <button disabled={Boolean(busyAction)} onClick={() => resetPassword(user)}>
                      {busyAction === `reset-${user.id}` ? 'Envoi email...' : 'Regenerer MDP + email'}
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
        <h2>Exports</h2>
        <span>Admin / Directeur</span>
      </div>
        <div className="report-actions">
        {reports.flatMap(([name, label]) => ['csv', 'xls', 'pdf'].map((format) => (
          <button key={`${name}-${format}`} onClick={() => onAction(() => api.downloadReport(name, format))}>{label} {format.toUpperCase()}</button>
        )))}
      </div>
    </section>
  );
}

function Field({ label, value, onChange, type = 'text', required = false, disabled = false, placeholder = '' }) {
  return (
    <label>
      {label}
      <input
        type={type}
        value={value ?? ''}
        required={required}
        disabled={disabled}
        placeholder={placeholder}
        onChange={(event) => onChange?.(event.target.value)}
      />
    </label>
  );
}

function Select({ label, value, onChange, options, required = false, disabled = false, placeholder = '-' }) {
  return (
    <label>
      {label}
      <select
        value={value ?? ''}
        required={required}
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
      >
        <option value="">{placeholder}</option>
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

function normalizeSearch(value) {
  return String(value ?? '')
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .trim();
}

function typeMateriel(piece) {
  if (!piece) return 'Autres';
  const text = normalizeSearch(`${piece.reference} ${piece.designation}`);
  if (text.includes('pc-') || text.includes('desk-') || text.includes('macbook') || text.includes('thinkpad') || text.includes('elitebook')) {
    return 'Postes';
  }
  if (text.includes('ecran')) return 'Ecrans';
  if (text.includes('imprimante') || text.includes('imp-')) return 'Impression';
  if (text.includes('switch') || text.includes('routeur') || text.includes('router') || text.includes('point acces') || text.includes('ubiquiti') || text.includes('ap-')) return 'Reseau';
  return 'Accessoires';
}

function pieceVisualMeta(piece) {
  const text = normalizeSearch(`${piece?.reference ?? ''} ${piece?.designation ?? ''}`);
  if (text.includes('pc-') || text.includes('macbook') || text.includes('thinkpad') || text.includes('elitebook') || text.includes('zenbook')) {
    return { icon: '▰', tone: 'laptop' };
  }
  if (text.includes('desk-') || text.includes('optiplex') || text.includes('thinkcentre') || text.includes('mini')) {
    return { icon: '▣', tone: 'desktop' };
  }
  if (text.includes('ecran') || text.includes('screen') || text.includes('dalle')) {
    return { icon: '▭', tone: 'screen' };
  }
  if (text.includes('clavier')) return { icon: '⌨', tone: 'keyboard' };
  if (text.includes('souris')) return { icon: '◖', tone: 'mouse' };
  if (text.includes('casque')) return { icon: '◔', tone: 'headset' };
  if (text.includes('dock') || text.includes('station')) return { icon: '▤', tone: 'dock' };
  if (text.includes('imprimante') || text.includes('toner') || text.includes('imp-')) return { icon: '▥', tone: 'printer' };
  if (text.includes('switch') || text.includes('routeur') || text.includes('point acces') || text.includes('ap-') || text.includes('rj45')) {
    return { icon: '⌁', tone: 'network' };
  }
  if (text.includes('ram') || text.includes('ssd') || text.includes('batterie') || text.includes('chargeur')) {
    return { icon: '◇', tone: 'part' };
  }
  return { icon: '□', tone: 'default' };
}

function keepOnePoste(pieceIds, pieces = []) {
  let posteKept = false;
  return pieceIds.filter((id) => {
    const piece = pieces.find((item) => String(item.id) === String(id));
    if (typeMateriel(piece) !== 'Postes') return true;
    if (posteKept) return false;
    posteKept = true;
    return true;
  });
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
