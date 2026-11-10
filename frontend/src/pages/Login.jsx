import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, storeSession } from '../api.js';
import { Alert, Drop } from '../components/Common.jsx';
import { homePath } from '../constants.js';

const DEMO = [
  ['Administrator', 'admin@bloodbridge.local', 'Admin@12345'],
  ['Coordinator', 'coordinator@demo.bloodbridge.local', 'Demo@12345'],
  ['Requester', 'requester@demo.bloodbridge.local', 'Demo@12345'],
  ['Donor', 'donor01@demo.bloodbridge.local', 'Demo@12345'],
];

export default function Login({ onLogin }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();

  async function submit(e) {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      const res = await api('/api/auth/login', { method: 'POST', body: { email, password } });
      storeSession(res.token, res.user);
      onLogin(res.user);
      navigate(homePath(res.user));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth">
      <aside className="auth-side">
        <Drop size={64} />
        <h2>Welcome back</h2>
        <p>Log in to respond to requests, manage your donor card, or coordinate blood supply.</p>
      </aside>
      <form className="card auth-form" onSubmit={submit}>
        <h2>Log in</h2>
        <Alert error={error} />
        <label>Email<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="username" /></label>
        <label>Password<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password" /></label>
        <button type="submit" disabled={busy}>{busy ? 'Signing in...' : 'Log in'}</button>
        <p>No account? <Link to="/register">Register</Link></p>
        <details className="demo">
          <summary>Demo accounts (available when demo data is enabled)</summary>
          <ul>
            {DEMO.map(([role, mail, pw]) => (
              <li key={mail}>
                <button type="button" className="link" onClick={() => { setEmail(mail); setPassword(pw); }}>{role}</button>
                <span className="muted"> {mail}</span>
              </li>
            ))}
          </ul>
          <p className="muted">Click a role to fill the form. The administrator password may have been changed in your deployment.</p>
        </details>
      </form>
    </div>
  );
}
