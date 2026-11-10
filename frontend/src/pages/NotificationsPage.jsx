import React, { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { Alert, Drop, Empty } from '../components/Common.jsx';
import { fmt } from '../constants.js';

export default function NotificationsPage({ onChange }) {
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      const list = await api('/api/notifications');
      setItems(list);
      onChange(list.filter((n) => !n.seen).length);
    } catch (e) { setError(e.message); }
  }, [onChange]);

  useEffect(() => { load(); }, [load]);

  async function readAll() {
    try { await api('/api/notifications/read-all', { method: 'POST' }); load(); }
    catch (e) { setError(e.message); }
  }

  async function readOne(id) {
    try { await api(`/api/notifications/${id}/read`, { method: 'POST' }); load(); }
    catch (e) { setError(e.message); }
  }

  return (
    <section className="card narrow-wide">
      <div className="detail-head">
        <h2>Alerts</h2>
        <button className="secondary" onClick={readAll}>Mark all as read</button>
      </div>
      <Alert error={error} />
      {items.length === 0 && <Empty>Nothing here yet.</Empty>}
      {items.map((n) => (
        <div key={n.id} className={`alert-item ${n.seen ? '' : 'unread'}`}>
          <Drop size={16} className="alert-drop" />
          <div className="req-main">
            <div>{n.message}</div>
            <div className="muted">{fmt(n.createdAt)}</div>
          </div>
          {!n.seen && <button className="secondary" onClick={() => readOne(n.id)}>Mark read</button>}
        </div>
      ))}
    </section>
  );
}
