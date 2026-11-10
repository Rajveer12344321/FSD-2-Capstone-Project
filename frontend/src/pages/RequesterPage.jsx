import React, { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import Radar from '../components/Radar.jsx';
import { Alert, Badge, Empty, GroupDrop, ScoreBar, StatusSteps, UnitBags, UrgencyTag } from '../components/Common.jsx';
import { BLOOD_GROUPS, COMPONENTS, PLACES, URGENCIES, can, componentLabel, fmt, groupLabel } from '../constants.js';

function defaultDeadline() {
  const d = new Date(Date.now() + 6 * 3600 * 1000);
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
}

const emptyForm = () => ({
  bloodGroup: 'O_POS', component: 'RED_CELLS', unitsRequired: 1, hospital: '',
  city: PLACES[0].name, urgency: 'URGENT', requiredBy: defaultDeadline(),
});

export default function RequesterPage({ user }) {
  const [form, setForm] = useState(emptyForm());
  const [requests, setRequests] = useState([]);
  const [selected, setSelected] = useState(null);
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');

  const canCreate = can(user, 'REQUEST_CREATE');
  const viewAll = can(user, 'REQUEST_VIEW_ALL');

  const loadList = useCallback(() => api('/api/requests').then(setRequests).catch((e) => setError(e.message)), []);
  const loadDetail = useCallback((id) => api(`/api/requests/${id}`).then(setDetail).catch((e) => setError(e.message)), []);

  useEffect(() => {
    loadList();
    const t = setInterval(() => {
      loadList();
      if (selected) loadDetail(selected);
    }, 10000);
    return () => clearInterval(t);
  }, [loadList, loadDetail, selected]);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function create(e) {
    e.preventDefault();
    setError(''); setOk('');
    const place = PLACES.find((p) => p.name === form.city);
    try {
      const res = await api('/api/requests', {
        method: 'POST',
        body: {
          bloodGroup: form.bloodGroup, component: form.component, unitsRequired: Number(form.unitsRequired),
          hospital: form.hospital, city: place.name, latitude: place.lat, longitude: place.lng,
          urgency: form.urgency, requiredBy: new Date(form.requiredBy).toISOString(),
        },
      });
      setOk(`Request #${res.request.id} created. ${res.candidates.length} donor(s) invited.`);
      setForm(emptyForm());
      setSelected(res.request.id);
      setDetail(res);
      loadList();
    } catch (err) { setError(err.message); }
  }

  async function act(path, method, body, message) {
    setError(''); setOk('');
    try {
      const res = await api(path, { method, body });
      setDetail(res);
      setOk(message);
      loadList();
    } catch (err) { setError(err.message); }
  }

  async function recordDonation(invitationId) {
    setError(''); setOk('');
    try {
      await api('/api/donations', { method: 'POST', body: { invitationId, units: 1 } });
      setOk('Donation recorded. The donor and requester were notified.');
      loadDetail(detail.request.id);
      loadList();
    } catch (err) { setError(err.message); }
  }

  function open(id) {
    setSelected(id);
    setDetail(null);
    loadDetail(id);
  }

  const active = detail && (detail.request.status === 'OPEN' || detail.request.status === 'MATCHING');
  const mayManage = detail && ((detail.request.requesterId === user.id && can(user, 'REQUEST_VIEW_OWN')) || can(user, 'REQUEST_MANAGE_ALL'));
  const isOwner = detail && detail.request.requesterId === user.id;

  return (
    <div className="stack">
      <div className="page-head">
        <h1>{viewAll ? 'All blood requests' : 'My blood requests'}</h1>
        {viewAll && <span className="muted">Read access to every request and its ranked candidates. Donor phone numbers are visible only to the request owner.</span>}
      </div>
      <Alert error={error} ok={ok} />
      <div className="grid">
        {canCreate && (
          <section className="card">
            <h2>New blood request</h2>
            <form onSubmit={create}>
              <div className="row">
                <label>Blood group
                  <select value={form.bloodGroup} onChange={set('bloodGroup')}>
                    {BLOOD_GROUPS.map(([k, l]) => <option key={k} value={k}>{l}</option>)}
                  </select>
                </label>
                <label>Component
                  <select value={form.component} onChange={set('component')}>
                    {COMPONENTS.map(([k, l]) => <option key={k} value={k}>{l}</option>)}
                  </select>
                </label>
                <label>Units
                  <input type="number" min="1" max="20" value={form.unitsRequired} onChange={set('unitsRequired')} required />
                </label>
              </div>
              <label>Hospital<input value={form.hospital} onChange={set('hospital')} required maxLength={150} /></label>
              <div className="row">
                <label>Simulated hospital location
                  <select value={form.city} onChange={set('city')}>
                    {PLACES.map((p) => <option key={p.name}>{p.name}</option>)}
                  </select>
                </label>
                <label>Urgency
                  <select value={form.urgency} onChange={set('urgency')}>
                    {URGENCIES.map(([k, l]) => <option key={k} value={k}>{l}</option>)}
                  </select>
                </label>
              </div>
              <label>Required by<input type="datetime-local" value={form.requiredBy} onChange={set('requiredBy')} required /></label>
              <button type="submit">Create request</button>
            </form>
          </section>
        )}

        <section className="card">
          <h2>{viewAll ? 'Requests' : 'Your requests'}</h2>
          {requests.length === 0 && <Empty>No requests yet.</Empty>}
          {requests.map((r) => (
            <div className={`req-card clickable urgency-border-${r.urgency.toLowerCase()} ${selected === r.id ? 'selected' : ''}`}
                 key={r.id} onClick={() => open(r.id)}>
              <GroupDrop group={r.bloodGroup} size={38} />
              <div className="req-main">
                <div><strong>#{r.id} {groupLabel(r.bloodGroup)} {componentLabel(r.component)}</strong> <UrgencyTag value={r.urgency} /></div>
                <div>{r.hospital}, {r.city}{viewAll && <span className="muted"> · by {r.requesterName}</span>}</div>
                <div className="muted">By {fmt(r.requiredBy)} · {r.invitedCount} invited · {r.acceptedCount} accepted</div>
                <UnitBags units={r.unitsRequired} accepted={r.acceptedCount} />
              </div>
              <Badge value={r.status} />
            </div>
          ))}
        </section>
      </div>

      {detail && (
        <section className="card">
          <div className="detail-head">
            <div>
              <h2>Request #{detail.request.id}: ranked candidates</h2>
              <p className="muted">Preliminary candidates only. Each needs confirmation by qualified healthcare professionals.</p>
            </div>
            <StatusSteps status={detail.request.status} />
          </div>
          {mayManage && active && (
            <div className="row">
              <button onClick={() => act(`/api/requests/${detail.request.id}/rematch`, 'POST', undefined, 'Matching re-run.')}>Re-run matching</button>
              {detail.request.status === 'MATCHING' && (
                <button className="good" onClick={() => act(`/api/requests/${detail.request.id}/status`, 'PATCH', { status: 'FULFILLED' }, 'Marked as fulfilled.')}>Mark fulfilled</button>
              )}
              <button className="danger" onClick={() => act(`/api/requests/${detail.request.id}/status`, 'PATCH', { status: 'CANCELLED' }, 'Request cancelled.')}>Cancel request</button>
            </div>
          )}
          {detail.candidates.length === 0 ? (
            <Empty>No eligible candidates yet. The request stays open and matching is retried automatically.</Empty>
          ) : (
            <div className="detail-split">
              <Radar candidates={detail.candidates} />
              <div className="cand-grid">
              {detail.candidates.map((c, idx) => (
                <div className="cand" key={c.invitationId}>
                  <div className="cand-top">
                    <span className="rank">#{idx + 1}</span>
                    <GroupDrop group={c.bloodGroup} size={34} />
                    <div>
                      <strong>{c.donorLabel}</strong>{c.verified && <span className="verified" title="Verified donor"> ✓</span>}
                      <div className="muted">{c.city}</div>
                    </div>
                  </div>
                  <ScoreBar score={c.score} />
                  <div className="cand-status">
                    <Badge value={c.status} />
                    <span title={`Delivery attempts: ${c.attempts}`}><Badge value={c.deliveryStatus} /></span>
                  </div>
                  {isOwner && c.phone && <div className="phone">Phone: <strong>{c.phone}</strong></div>}
                  {c.donated && <div className="phone">Donation recorded ✓</div>}
                  {can(user, 'DONATION_RECORD') && c.status === 'ACCEPTED' && !c.donated && (
                    <button className="good" onClick={() => recordDonation(c.invitationId)}>Record donation</button>
                  )}
                  <details>
                    <summary>Why this rank</summary>
                    <ul className="reasons">{c.reasons.map((r, i) => <li key={i}>{r}</li>)}</ul>
                  </details>
                </div>
              ))}
              </div>
            </div>
          )}
        </section>
      )}
    </div>
  );
}
