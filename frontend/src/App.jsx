import React, { useCallback, useEffect, useState } from 'react';
import { Link, NavLink, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { api, clearSession, getStoredUser, getToken, storeSession } from './api.js';
import { Disclaimer, Drop } from './components/Common.jsx';
import Icon from './components/Icon.jsx';
import { ROLE_LABEL, STAFF_PERMISSIONS, can, canAny, homePath } from './constants.js';
import { currentTheme, setTheme } from './theme.js';
import Landing from './pages/Landing.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import DonorPage from './pages/DonorPage.jsx';
import DrivesPage from './pages/DrivesPage.jsx';
import RequesterPage from './pages/RequesterPage.jsx';
import ControlRoom from './pages/ControlRoom.jsx';
import NotificationsPage from './pages/NotificationsPage.jsx';

/** Route guard: sends anonymous users to login and users without the needed permission to their home page. */
function Guard({ user, allow, children }) {
  const location = useLocation();
  if (!user) return <Navigate to="/login" replace />;
  if (allow && !allow(user)) {
    const home = homePath(user);
    if (home === location.pathname) {
      return (
        <section className="card narrow-wide">
          <h2>No access</h2>
          <p>Your role does not currently include permission to use this page. Ask an administrator if you need access.</p>
        </section>
      );
    }
    return <Navigate to={home} replace />;
  }
  return children;
}

function ThemeToggle() {
  const [theme, setLocal] = useState(currentTheme());
  function toggle() {
    const next = theme === 'dark' ? 'light' : 'dark';
    setTheme(next);
    setLocal(next);
  }
  return (
    <button className="iconbtn" onClick={toggle} title="Toggle dark mode" aria-label="Toggle dark mode">
      <Icon name={theme === 'dark' ? 'sun' : 'moon'} />
    </button>
  );
}

export default function App() {
  const [user, setUser] = useState(() => {
    // Sessions saved by an older version have no permission list; ask the user to sign in again.
    const stored = getStoredUser();
    return getToken() && stored && Array.isArray(stored.permissions) ? stored : null;
  });
  const [unread, setUnread] = useState(0);
  const [menuOpen, setMenuOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const logout = useCallback(() => {
    clearSession();
    setUser(null);
    navigate('/');
  }, [navigate]);

  useEffect(() => {
    window.addEventListener('bb-logout', logout);
    return () => window.removeEventListener('bb-logout', logout);
  }, [logout]);

  useEffect(() => { setMenuOpen(false); }, [location.pathname]);

  // Refresh role and permissions from the server so UI changes follow permission edits.
  useEffect(() => {
    if (!getToken()) return;
    api('/api/me').then((fresh) => {
      storeSession(getToken(), fresh);
      setUser(fresh);
    }).catch(() => {});
  }, []);

  useEffect(() => {
    if (!user) return undefined;
    let active = true;
    const poll = () =>
      api('/api/notifications/unread-count').then((r) => active && setUnread(r.count)).catch(() => {});
    poll();
    const t = setInterval(poll, 15000);
    return () => { active = false; clearInterval(t); };
  }, [user]);

  const isStaff = user && canAny(user, STAFF_PERMISSIONS);
  const seesRequests = user && (can(user, 'REQUEST_VIEW_OWN') || can(user, 'REQUEST_VIEW_ALL'));
  const seesDrives = user && (can(user, 'DRIVE_REGISTER') || can(user, 'DRIVE_MANAGE'));

  const routes = (
    <Routes>
      <Route path="/" element={user ? <Navigate to={homePath(user)} replace /> : <Landing />} />
      <Route path="/login" element={<Login onLogin={setUser} />} />
      <Route path="/register" element={<Register onLogin={setUser} />} />
      <Route path="/donor" element={<Guard user={user} allow={(u) => can(u, 'PROFILE_MANAGE')}><DonorPage /></Guard>} />
      <Route path="/drives" element={<Guard user={user} allow={(u) => can(u, 'DRIVE_REGISTER') || can(u, 'DRIVE_MANAGE')}><DrivesPage user={user} /></Guard>} />
      <Route path="/requests" element={
        <Guard user={user} allow={(u) => can(u, 'REQUEST_VIEW_OWN') || can(u, 'REQUEST_VIEW_ALL')}>
          <RequesterPage user={user} />
        </Guard>} />
      <Route path="/control" element={<Guard user={user} allow={(u) => canAny(u, STAFF_PERMISSIONS)}><ControlRoom user={user} /></Guard>} />
      <Route path="/notifications" element={<Guard user={user}><NotificationsPage onChange={setUnread} /></Guard>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );

  if (!user) {
    return (
      <div className="app">
        <header className="publicbar">
          <Link to="/" className="brand"><Drop size={20} /> BloodBridge</Link>
          <div className="session">
            <ThemeToggle />
            <Link to="/login" className="textlink">Log in</Link>
            <Link to="/register" className="pillbtn">Join now</Link>
          </div>
        </header>
        <Disclaimer />
        <main>{routes}</main>
        <footer className="footer">BloodBridge · FSD-II capstone prototype · synthetic data only</footer>
      </div>
    );
  }

  return (
    <div className={`shell ${menuOpen ? 'menu-open' : ''}`}>
      <aside className="sidebar">
        <Link to="/" className="brand"><Drop size={22} /> BloodBridge</Link>
        <nav>
          {can(user, 'PROFILE_MANAGE') && <NavLink to="/donor"><Icon name="heart" /> My donor card</NavLink>}
          {seesDrives && <NavLink to="/drives"><Icon name="calendar" /> Blood drives</NavLink>}
          {seesRequests && <NavLink to="/requests"><Icon name="list" /> {can(user, 'REQUEST_VIEW_ALL') ? 'All requests' : 'My requests'}</NavLink>}
          {isStaff && <NavLink to="/control"><Icon name="shield" /> Control room</NavLink>}
          <NavLink to="/notifications"><Icon name="bell" /> Alerts{unread > 0 && <span className="count">{unread}</span>}</NavLink>
        </nav>
        <div className="side-user">
          <span className={`rolepill role-${user.role.toLowerCase()}`}>{ROLE_LABEL[user.role]}</span>
          <div className="who">{user.name}</div>
          <div className="muted small">{user.email}</div>
        </div>
      </aside>
      <div className="content">
        <header className="topbar2">
          <button className="iconbtn menu-btn" onClick={() => setMenuOpen(!menuOpen)} aria-label="Menu">
            <Icon name={menuOpen ? 'close' : 'menu'} />
          </button>
          <span className="top-title"><Drop size={14} /> Saving time when it matters</span>
          <div className="session">
            <ThemeToggle />
            <button className="iconbtn" onClick={logout} title="Log out" aria-label="Log out"><Icon name="logout" /></button>
          </div>
        </header>
        <Disclaimer />
        <main>{routes}</main>
        <footer className="footer">BloodBridge · FSD-II capstone prototype · synthetic data only</footer>
      </div>
      {menuOpen && <div className="scrim" onClick={() => setMenuOpen(false)} />}
    </div>
  );
}
