import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, storeSession } from '../api.js';
import { Alert, Drop } from '../components/Common.jsx';
import { homePath } from '../constants.js';

export default function Register({ onLogin }) {
  const [form, setForm] = useState({ name: '', email: '', password: '', role: 'DONOR' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      const res = await api('/api/auth/register', { method: 'POST', body: form });
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
        <h2>Join the network</h2>
        <p>Donors and requesters register here. Coordinator and administrator accounts are created by an administrator.</p>
      </aside>
      <form className="card auth-form" onSubmit={submit}>
        <h2>Create an account</h2>
        <Alert error={error} />
        <div className="choice">
          <label className={form.role === 'DONOR' ? 'active' : ''}>
            <input type="radio" name="role" value="DONOR" checked={form.role === 'DONOR'} onChange={set('role')} />
            <strong>I want to donate</strong><span>Create a donor card</span>
          </label>
          <label className={form.role === 'REQUESTER' ? 'active' : ''}>
            <input type="radio" name="role" value="REQUESTER" checked={form.role === 'REQUESTER'} onChange={set('role')} />
            <strong>I need blood</strong><span>Post a blood request</span>
          </label>
        </div>
        <label>Full name<input value={form.name} onChange={set('name')} required maxLength={100} /></label>
        <label>Email<input type="email" value={form.email} onChange={set('email')} required /></label>
        <label>Password (min 8 characters)<input type="password" minLength={8} value={form.password} onChange={set('password')} required autoComplete="new-password" /></label>
        <button type="submit" disabled={busy}>{busy ? 'Creating...' : 'Register'}</button>
        <p>Already registered? <Link to="/login">Log in</Link></p>
      </form>
    </div>
  );
}
