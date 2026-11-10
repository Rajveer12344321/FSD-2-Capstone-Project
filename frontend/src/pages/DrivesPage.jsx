import React, { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { Alert, Empty } from '../components/Common.jsx';
import { PLACES, can, fmt } from '../constants.js';

function inDays(n) {
  const d = new Date(Date.now() + n * 86400000);
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
}

const emptyForm = () => ({ title: '', venue: '', city: PLACES[0].name, driveDate: inDays(7), capacity: 50, description: '' });

export default function DrivesPage({ user }) {
  const [drives, setDrives] = useState([]);
  const [form, setForm] = useState(emptyForm());
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const manage = can(user, 'DRIVE_MANAGE');
  const register = can(user, 'DRIVE_REGISTER');

  const load = useCallback(() => api('/api/drives').then(setDrives).catch((e) => setError(e.message)), []);
  useEffect(() => { load(); }, [load]);

  async function run(path, method, body, message) {
    setError(''); setOk('');
    try {
      await api(path, { method, body });
      setOk(message);
    } catch (e) { setError(e.message); }
    load();
  }

  async function create(e) {
    e.preventDefault();
    await run('/api/drives', 'POST', {
      ...form, capacity: Number(form.capacity), driveDate: new Date(form.driveDate).toISOString(),
    }, 'Blood drive published.');
    setForm(emptyForm());
  }

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  return (
    <div className="stack">
      <div className="page-head">
        <h1>Blood drives</h1>
        <span className="muted">Community donation camps you can join, organised by coordinators.</span>
      </div>
      <Alert error={error} ok={ok} />
      <div className="grid">
        {manage && (
          <section className="card">
            <h2>Publish a drive</h2>
            <form onSubmit={create}>
              <label>Title<input value={form.title} onChange={set('title')} required maxLength={120} /></label>
              <label>Venue<input value={form.venue} onChange={set('venue')} required maxLength={150} /></label>
              <div className="row">
                <label>City
                  <select value={form.city} onChange={set('city')}>{PLACES.map((p) => <option key={p.name}>{p.name}</option>)}</select>
                </label>
                <label>Capacity<input type="number" min="1" max="1000" value={form.capacity} onChange={set('capacity')} required /></label>
              </div>
              <label>Date and time<input type="datetime-local" value={form.driveDate} onChange={set('driveDate')} required /></label>
              <label>Description (optional)<input value={form.description} onChange={set('description')} maxLength={500} /></label>
              <button type="submit">Publish drive</button>
            </form>
          </section>
        )}
        <section className="stack">
          {drives.length === 0 && <div className="card"><Empty>No upcoming drives right now.</Empty></div>}
          {drives.map((d) => {
            const date = new Date(d.driveDate);
            const pct = Math.min(100, Math.round((d.registered / d.capacity) * 100));
            const past = date.getTime() < Date.now();
            return (
              <article className={`card drive ${d.cancelled ? 'drive-cancelled' : ''}`} key={d.id}>
                <div className="datebox">
                  <span>{date.toLocaleString(undefined, { month: 'short' })}</span>
                  <strong>{date.getDate()}</strong>
                </div>
                <div className="drive-main">
                  <h3>{d.title} {d.cancelled && <span className="badge badge-cancelled">cancelled</span>} {past && !d.cancelled && <span className="badge">past</span>}</h3>
                  <div>{d.venue}, {d.city}</div>
                  <div className="muted">{fmt(d.driveDate)}</div>
                  {d.description && <p className="muted">{d.description}</p>}
                  <div className="meter"><div style={{ width: `${pct}%` }} /></div>
                  <div className="muted small">{d.registered} of {d.capacity} places taken</div>
                </div>
                <div className="drive-actions">
                  {register && !d.cancelled && !past && (d.registeredByMe
                    ? <button className="secondary" onClick={() => run(`/api/drives/${d.id}/register`, 'DELETE', undefined, 'Registration removed.')}>Cancel my spot</button>
                    : <button onClick={() => run(`/api/drives/${d.id}/register`, 'POST', undefined, 'You are registered. Thank you!')}>Register</button>)}
                  {manage && !d.cancelled && !past && (
                    <button className="danger" onClick={() => run(`/api/drives/${d.id}/cancel`, 'PATCH', undefined, 'Drive cancelled and donors notified.')}>Cancel drive</button>
                  )}
                </div>
              </article>
            );
          })}
        </section>
      </div>
    </div>
  );
}
