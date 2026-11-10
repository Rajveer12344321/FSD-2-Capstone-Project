import React, { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { Alert, Badge, Drop, Empty, GroupDrop, Stat, UrgencyTag } from '../components/Common.jsx';
import { BLOOD_GROUPS, MIN_DONATION_INTERVAL_DAYS, PLACES, componentLabel, fmt, groupLabel } from '../constants.js';

const EMPTY = {
  bloodGroup: 'O_POS', city: 'Chandigarh', latitude: 30.7333, longitude: 76.7794,
  phone: '', available: true, lastDonationDate: '', contactConsent: false,
};

function eligibility(lastDate) {
  if (!lastDate) return { eligible: true, days: 0, pct: 100 };
  const since = Math.floor((Date.now() - new Date(lastDate).getTime()) / 86400000);
  const remaining = Math.max(0, MIN_DONATION_INTERVAL_DAYS - since);
  return { eligible: remaining === 0, days: remaining, pct: Math.min(100, Math.round((since / MIN_DONATION_INTERVAL_DAYS) * 100)) };
}

export default function DonorPage() {
  const [profile, setProfile] = useState(EMPTY);
  const [meta, setMeta] = useState({ exists: false, verified: false, availabilityConfirmedAt: null });
  const [invites, setInvites] = useState([]);
  const [impact, setImpact] = useState(null);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');

  const load = useCallback(async () => {
    try {
      const p = await api('/api/donor/profile');
      if (p.exists) {
        setProfile({ ...EMPTY, ...p, phone: p.phone || '', lastDonationDate: p.lastDonationDate || '' });
      }
      setMeta({ exists: p.exists, verified: p.verified, availabilityConfirmedAt: p.availabilityConfirmedAt });
      setInvites(await api('/api/donor/invitations'));
      setImpact(await api('/api/donor/impact'));
    } catch (e) { setError(e.message); }
  }, []);

  useEffect(() => {
    load();
    const t = setInterval(() => api('/api/donor/invitations').then(setInvites).catch(() => {}), 10000);
    return () => clearInterval(t);
  }, [load]);

  const set = (k) => (e) =>
    setProfile({ ...profile, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });

  function pickPlace(e) {
    const place = PLACES.find((p) => p.name === e.target.value);
    if (place) setProfile({ ...profile, city: place.name, latitude: place.lat, longitude: place.lng });
  }

  async function save(e) {
    e.preventDefault();
    setError(''); setOk('');
    try {
      const body = {
        bloodGroup: profile.bloodGroup,
        city: profile.city,
        latitude: Number(profile.latitude),
        longitude: Number(profile.longitude),
        phone: profile.phone,
        available: profile.available,
        lastDonationDate: profile.lastDonationDate || null,
        contactConsent: profile.contactConsent,
      };
      const p = await api('/api/donor/profile', { method: 'PUT', body });
      setMeta({ exists: true, verified: p.verified, availabilityConfirmedAt: p.availabilityConfirmedAt });
      setOk('Donor card saved.');
    } catch (err) { setError(err.message); }
  }

  async function confirm() {
    setError(''); setOk('');
    try {
      const p = await api('/api/donor/profile/confirm-availability', { method: 'POST' });
      setMeta({ exists: true, verified: p.verified, availabilityConfirmedAt: p.availabilityConfirmedAt });
      setProfile({ ...profile, available: true });
      setOk('Availability confirmed. Recent confirmations rank you higher.');
    } catch (err) { setError(err.message); }
  }

  async function respond(id, accept) {
    setError(''); setOk('');
    try {
      await api(`/api/invitations/${id}/respond`, { method: 'POST', body: { accept } });
      setOk(accept ? 'Thank you. You accepted the invitation.' : 'You declined the invitation.');
    } catch (err) { setError(err.message); }
    try { setInvites(await api('/api/donor/invitations')); } catch { /* keep current list */ }
  }

  const elig = eligibility(profile.lastDonationDate);
  const pending = invites.filter((i) => i.status === 'PENDING' && (i.requestStatus === 'OPEN' || i.requestStatus === 'MATCHING')).length;
  const accepted = invites.filter((i) => i.status === 'ACCEPTED').length;

  return (
    <div className="stack">
      <section className="donor-hero">
        <GroupDrop group={profile.bloodGroup} size={84} />
        <div className="donor-hero-text">
          <h1>My donor card</h1>
          <div className="chips">
            <span className={`chip ${profile.available ? 'chip-ok' : 'chip-off'}`}>{profile.available ? 'Available' : 'Not available'}</span>
            <span className={`chip ${meta.verified ? 'chip-ok' : ''}`}>{meta.verified ? 'Verified' : 'Not yet verified'}</span>
            <span className={`chip ${profile.contactConsent ? 'chip-ok' : ''}`}>{profile.contactConsent ? 'Contact consent given' : 'No contact consent'}</span>
          </div>
          <p className="muted">
            {meta.availabilityConfirmedAt ? `Availability confirmed ${fmt(meta.availabilityConfirmedAt)}` : 'Save your card to become matchable.'}
          </p>
        </div>
        <div className="eligibility">
          <div className="muted">Donation interval (indicative)</div>
          <div className="meter"><div style={{ width: `${elig.pct}%` }} /></div>
          <strong>{elig.eligible ? 'Interval satisfied' : `${elig.days} day(s) until interval is met`}</strong>
          <div className="muted small">Real eligibility must be confirmed by healthcare professionals.</div>
        </div>
      </section>

      <div className="stats">
        <Stat label="Waiting for your answer" value={pending} tone={pending ? 'alert' : undefined} />
        <Stat label="Invitations accepted" value={accepted} />
        <Stat label="Total invitations" value={invites.length} />
      </div>
      <Alert error={error} ok={ok} />

      <div className="grid">
        <section className="card">
          <h2>Edit donor card</h2>
          <form onSubmit={save}>
            <div className="row">
              <label>Blood group
                <select value={profile.bloodGroup} onChange={set('bloodGroup')}>
                  {BLOOD_GROUPS.map(([k, l]) => <option key={k} value={k}>{l}</option>)}
                </select>
              </label>
              <label>Simulated location
                <select value={profile.city} onChange={pickPlace}>
                  {PLACES.map((p) => <option key={p.name}>{p.name}</option>)}
                </select>
              </label>
            </div>
            <div className="row">
              <label>Latitude<input type="number" step="any" value={profile.latitude} onChange={set('latitude')} required /></label>
              <label>Longitude<input type="number" step="any" value={profile.longitude} onChange={set('longitude')} required /></label>
            </div>
            <label>Phone (shown only to the requester, after you accept, and only with consent)
              <input value={profile.phone} onChange={set('phone')} maxLength={30} />
            </label>
            <label>Last donation date (optional)
              <input type="date" value={profile.lastDonationDate} onChange={set('lastDonationDate')} />
            </label>
            <label className="check"><input type="checkbox" checked={profile.available} onChange={set('available')} /> I am currently available to donate</label>
            <label className="check"><input type="checkbox" checked={profile.contactConsent} onChange={set('contactConsent')} /> I consent to being contacted about matching requests</label>
            <div className="row">
              <button type="submit">Save donor card</button>
              <button type="button" className="secondary" onClick={confirm} disabled={!meta.exists}>I'm still available</button>
            </div>
          </form>
        </section>

        <section className="card">
          <h2>Requests that need you</h2>
          {invites.length === 0 && <Empty>No invitations yet. Keep your card available and consented so you can be matched.</Empty>}
          {invites.map((i) => (
            <div className={`req-card urgency-border-${i.urgency.toLowerCase()}`} key={i.id}>
              <GroupDrop group={i.bloodGroup} size={38} />
              <div className="req-main">
                <div><strong>{groupLabel(i.bloodGroup)} {componentLabel(i.component)}</strong> · {i.unitsRequired} unit(s) <UrgencyTag value={i.urgency} /></div>
                <div>{i.hospital}, {i.city}</div>
                <div className="muted">Needed by {fmt(i.requiredBy)} · request {i.requestStatus.toLowerCase()} · you: <Badge value={i.status} /></div>
              </div>
              {i.status === 'PENDING' && (i.requestStatus === 'OPEN' || i.requestStatus === 'MATCHING') && (
                <div className="req-actions">
                  <button onClick={() => respond(i.id, true)}>Accept</button>
                  <button className="secondary" onClick={() => respond(i.id, false)}>Decline</button>
                </div>
              )}
            </div>
          ))}
        </section>
      </div>

      {impact && (
        <section className="card">
          <h2>My impact</h2>
          <div className="stats">
            <Stat label="Recorded donations" value={impact.donations} />
            <Stat label="Units donated" value={impact.units} />
            <Stat label="Drives joined" value={impact.drivesJoined} />
            <Stat label="Last donation" value={impact.lastDonation || 'n/a'} />
          </div>
          <h3>Badges</h3>
          <div className="badges">
            {impact.badges.map((b) => (
              <div key={b.code} className={`medal ${b.earned ? 'earned' : ''}`} title={b.description}>
                <span className="medal-icon"><Drop size={18} /></span>
                <strong>{b.label}</strong>
                <span className="muted small">{b.earned ? 'Earned' : b.description}</span>
              </div>
            ))}
          </div>
          {impact.history.length > 0 && (
            <>
              <h3>Donation history</h3>
              <table>
                <thead><tr><th>Date</th><th>Group</th><th>Units</th><th>Request</th></tr></thead>
                <tbody>
                  {impact.history.map((h) => (
                    <tr key={h.id}><td>{h.donatedOn}</td><td>{groupLabel(h.bloodGroup)}</td><td>{h.units}</td><td>{h.requestId ? `#${h.requestId}` : '-'}</td></tr>
                  ))}
                </tbody>
              </table>
            </>
          )}
        </section>
      )}
    </div>
  );
}
