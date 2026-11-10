import React, { useCallback, useEffect, useState } from 'react';
import { Area, AreaChart, Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api, downloadFile } from '../api.js';
import { Alert, Badge, Empty, GroupDrop, Stat, UnitBags, UrgencyTag } from '../components/Common.jsx';
import Icon from '../components/Icon.jsx';
import { BLOOD_GROUPS, COMPONENTS, ROLES, ROLE_LABEL, URGENCIES, URGENCY_COLORS, can, canAny, componentLabel, fmt, groupLabel } from '../constants.js';

const TABS = [
  { id: 'overview', label: 'Overview', perms: ['ANALYTICS_VIEW', 'QUEUE_VIEW'] },
  { id: 'stock', label: 'Blood stock', perms: ['INVENTORY_VIEW'] },
  { id: 'donations', label: 'Donations & reports', perms: ['DONATION_RECORD', 'REPORT_EXPORT'] },
  { id: 'deliveries', label: 'Deliveries', perms: ['DELIVERY_RETRY'] },
  { id: 'users', label: 'Users & roles', perms: ['USER_VIEW'] },
  { id: 'access', label: 'Access control', perms: ['PERMISSION_MANAGE'] },
  { id: 'audit', label: 'Audit log', perms: ['AUDIT_VIEW'] },
];

function usePolling(fn, ms) {
  useEffect(() => {
    fn();
    const t = setInterval(fn, ms);
    return () => clearInterval(t);
  }, [fn, ms]);
}

function Overview({ user }) {
  const [stats, setStats] = useState(null);
  const [queue, setQueue] = useState([]);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const showStats = can(user, 'ANALYTICS_VIEW');
  const showQueue = can(user, 'QUEUE_VIEW');

  const load = useCallback(async () => {
    try {
      if (showStats) setStats(await api('/api/admin/analytics'));
      if (showQueue) setQueue(await api('/api/admin/queue'));
    } catch (e) { setError(e.message); }
  }, [showStats, showQueue]);
  usePolling(load, 10000);

  async function setUrgency(id, urgency) {
    setError(''); setOk('');
    try {
      await api(`/api/admin/requests/${id}/urgency`, { method: 'PATCH', body: { urgency } });
      setOk(`Urgency of #${id} updated.`);
      load();
    } catch (e) { setError(e.message); }
  }

  const chart = stats ? Object.entries(stats.outcomes).map(([name, count]) => ({ name, count })) : [];
  const urgency = stats ? Object.entries(stats.requestsByUrgency).map(([name, count]) => ({ name, count })) : [];
  const groups = stats ? Object.entries(stats.requestsByGroup).map(([name, count]) => ({ name, count })) : [];

  return (
    <div className="stack">
      <Alert error={error} ok={ok} />
      {showStats && stats && (
        <section className="card">
          <h2>Analytics</h2>
          <div className="stats">
            <Stat label="Total requests" value={stats.totalRequests} />
            <Stat label="Requests with eligible matches" value={stats.requestsWithMatches} hint="Requests that received at least one invitation" />
            <Stat label="Invitations sent" value={stats.invitationsSent} />
            <Stat label="Donor response rate" value={`${stats.responseRatePercent}%`} hint="(accepted + declined) / invitations sent" />
            <Stat label="Avg. time to first response" value={stats.avgMinutesToFirstResponse == null ? 'n/a' : `${stats.avgMinutesToFirstResponse} min`} hint="From request creation to first accept or decline" />
            <Stat label="Delivery failures" value={stats.deliveryFailures} tone={stats.deliveryFailures ? 'alert' : undefined} />
            <Stat label="Pending emergencies" value={stats.pendingEmergencyRequests} tone={stats.pendingEmergencyRequests ? 'alert' : undefined} hint="Emergency requests that are open or matching" />
            <Stat label="Low-stock items" value={stats.lowStockCount} tone={stats.lowStockCount ? 'alert' : undefined} hint="Stock rows at or below their threshold" />
            <Stat label="Recorded donations" value={stats.totalDonations} />
            <Stat label="Units donated" value={stats.unitsDonated} />
          </div>
          <div className="charts">
            <div>
              <h3>Requests, last 14 days</h3>
              <ResponsiveContainer width="100%" height={220}>
                <AreaChart data={stats.dailyRequests}>
                  <CartesianGrid strokeDasharray="3 3" opacity={0.4} />
                  <XAxis dataKey="date" tickFormatter={(d) => d.slice(5)} />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Area type="monotone" dataKey="count" name="Requests" stroke="#c8102e" fill="#c8102e" fillOpacity={0.2} />
                </AreaChart>
              </ResponsiveContainer>
            </div>
            <div>
              <h3>Urgency mix</h3>
              <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                  <Pie data={urgency} dataKey="count" nameKey="name" innerRadius={50} outerRadius={80} paddingAngle={3}>
                    {urgency.map((u) => <Cell key={u.name} fill={URGENCY_COLORS[u.name]} />)}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div>
              <h3>Demand by blood group</h3>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={groups}>
                  <CartesianGrid strokeDasharray="3 3" opacity={0.4} />
                  <XAxis dataKey="name" />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Bar dataKey="count" name="Requests" fill="#c8102e" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
            <div>
              <h3>Request outcomes</h3>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={chart}>
                  <CartesianGrid strokeDasharray="3 3" opacity={0.4} />
                  <XAxis dataKey="name" />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Bar dataKey="count" name="Requests" fill="#e08e0b" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </section>
      )}

      {showQueue && (
        <section className="card">
          <h2>Priority queue</h2>
          <p className="muted">Active requests ordered by urgency, then deadline, then submission time. A coordination order, not clinical triage.</p>
          {queue.length === 0 ? <Empty>No active requests.</Empty> : queue.map(({ position, request: r }) => (
            <div className={`req-card urgency-border-${r.urgency.toLowerCase()}`} key={r.id}>
              <span className="rank">#{position}</span>
              <GroupDrop group={r.bloodGroup} size={36} />
              <div className="req-main">
                <div><strong>#{r.id} {groupLabel(r.bloodGroup)} {componentLabel(r.component)}</strong> · {r.unitsRequired} unit(s) <UrgencyTag value={r.urgency} /> <Badge value={r.status} /></div>
                <div>{r.hospital}, {r.city}</div>
                <div className="muted">Needed by {fmt(r.requiredBy)} · {r.invitedCount} invited · {r.acceptedCount} accepted</div>
                <UnitBags units={r.unitsRequired} accepted={r.acceptedCount} />
              </div>
              {can(user, 'URGENCY_REVIEW') && (
                <label className="inline">Urgency review
                  <select value={r.urgency} onChange={(e) => setUrgency(r.id, e.target.value)}>
                    {URGENCIES.map(([k, l]) => <option key={k} value={k}>{l}</option>)}
                  </select>
                </label>
              )}
            </div>
          ))}
        </section>
      )}
    </div>
  );
}

function Stock({ user }) {
  const [items, setItems] = useState([]);
  const [sel, setSel] = useState(null);
  const [delta, setDelta] = useState(1);
  const [reason, setReason] = useState('');
  const [threshold, setThreshold] = useState(5);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const manage = can(user, 'INVENTORY_MANAGE');

  const load = useCallback(() => api('/api/inventory').then(setItems).catch((e) => setError(e.message)), []);
  usePolling(load, 15000);

  const cell = (g, c) => items.find((i) => i.bloodGroup === g && i.component === c);
  const selected = items.find((i) => i.id === sel);

  function choose(item) {
    setSel(item.id);
    setThreshold(item.lowThreshold);
    setDelta(1);
    setReason('');
  }

  async function adjust(e) {
    e.preventDefault();
    setError(''); setOk('');
    try {
      await api(`/api/inventory/${sel}/adjust`, { method: 'PATCH', body: { delta: Number(delta), reason } });
      setOk('Stock updated.');
      load();
    } catch (err) { setError(err.message); }
  }

  async function saveThreshold() {
    setError(''); setOk('');
    try {
      await api(`/api/inventory/${sel}/threshold`, { method: 'PATCH', body: { lowThreshold: Number(threshold) } });
      setOk('Threshold updated.');
      load();
    } catch (err) { setError(err.message); }
  }

  const level = (i) => (i.low ? 'low' : i.units <= i.lowThreshold * 2 ? 'mid' : 'ok');

  return (
    <section className="card">
      <h2>Blood stock</h2>
      <p className="muted">Units on hand per group and component. Red cells are at or below their low-stock threshold, amber are close.</p>
      <Alert error={error} ok={ok} />
      <table className="stockgrid">
        <thead><tr><th>Group</th>{COMPONENTS.map(([k, l]) => <th key={k}>{l}</th>)}</tr></thead>
        <tbody>
          {BLOOD_GROUPS.map(([g, label]) => (
            <tr key={g}>
              <td><strong>{label}</strong></td>
              {COMPONENTS.map(([c]) => {
                const item = cell(g, c);
                if (!item) return <td key={c}>-</td>;
                return (
                  <td key={c}>
                    <button className={`stockcell stock-${level(item)} ${sel === item.id ? 'selected' : ''}`} onClick={() => choose(item)}>
                      {item.units}
                    </button>
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
      {selected && (
        <div className="stockpanel">
          <h3>{groupLabel(selected.bloodGroup)} {componentLabel(selected.component)}: {selected.units} unit(s)</h3>
          <p className="muted">Last updated {fmt(selected.updatedAt)}. Low-stock threshold {selected.lowThreshold}.</p>
          {manage ? (
            <>
              <form className="row" onSubmit={adjust}>
                <label>Change (+ add, - remove)<input type="number" value={delta} onChange={(e) => setDelta(e.target.value)} required /></label>
                <label>Reason<input value={reason} onChange={(e) => setReason(e.target.value)} required maxLength={200} placeholder="e.g. received from blood bank" /></label>
                <button type="submit">Apply</button>
              </form>
              <div className="row">
                <label>Low-stock threshold<input type="number" min="0" max="1000" value={threshold} onChange={(e) => setThreshold(e.target.value)} /></label>
                <button className="secondary" onClick={saveThreshold}>Save threshold</button>
              </div>
            </>
          ) : <p className="muted">You have read-only access to stock.</p>}
        </div>
      )}
    </section>
  );
}

function Donations({ user }) {
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');
  const exportOk = can(user, 'REPORT_EXPORT');
  const load = useCallback(() => api('/api/donations').then(setRows).catch((e) => setError(e.message)), []);
  usePolling(load, 20000);

  async function download(path, name) {
    setError('');
    try { await downloadFile(path, name); } catch (e) { setError(e.message); }
  }

  return (
    <section className="card">
      <div className="section-head">
        <h2>Recorded donations</h2>
        {exportOk && (
          <div className="row">
            <button className="secondary" onClick={() => download('/api/reports/requests.csv', 'bloodbridge-requests.csv')}><Icon name="download" /> Requests CSV</button>
            <button className="secondary" onClick={() => download('/api/reports/donations.csv', 'bloodbridge-donations.csv')}><Icon name="download" /> Donations CSV</button>
          </div>
        )}
      </div>
      <p className="muted">Donations are recorded from a request's candidate list once a donor has accepted and donated.</p>
      <Alert error={error} />
      {rows.length === 0 ? <Empty>No donations recorded yet.</Empty> : (
        <table>
          <thead><tr><th>Date</th><th>Donor</th><th>Group</th><th>Units</th><th>Request</th><th>Recorded by</th></tr></thead>
          <tbody>
            {rows.map((d) => (
              <tr key={d.id}><td>{d.donatedOn}</td><td>{d.donorLabel}</td><td>{groupLabel(d.bloodGroup)}</td><td>{d.units}</td><td>{d.requestId ? `#${d.requestId}` : '-'}</td><td>{d.recordedBy}</td></tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

function Deliveries() {
  const [failed, setFailed] = useState([]);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const load = useCallback(() => api('/api/admin/failed-deliveries').then(setFailed).catch((e) => setError(e.message)), []);
  usePolling(load, 10000);

  async function retry(id) {
    setError(''); setOk('');
    try {
      await api(`/api/admin/invitations/${id}/retry`, { method: 'POST' });
      setOk('Delivery retried.');
      load();
    } catch (e) { setError(e.message); }
  }

  return (
    <section className="card">
      <h2>Failed notification deliveries</h2>
      <Alert error={error} ok={ok} />
      {failed.length === 0 ? <Empty>No failed deliveries.</Empty> : (
        <table>
          <thead><tr><th>Invitation</th><th>Request</th><th>Donor</th><th>Attempts</th><th /></tr></thead>
          <tbody>
            {failed.map((f) => (
              <tr key={f.invitationId}>
                <td>#{f.invitationId}</td><td>#{f.requestId}</td><td>{f.donorLabel}</td><td>{f.attempts}</td>
                <td><button onClick={() => retry(f.invitationId)}>Retry</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

function Users({ user }) {
  const [users, setUsers] = useState([]);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [form, setForm] = useState({ name: '', email: '', password: '', role: 'MODERATOR' });
  const isAdmin = user.role === 'ADMIN';
  const roleOptions = ROLES.filter((r) => r !== 'ADMIN' || isAdmin);

  const load = useCallback(() => api('/api/admin/users').then(setUsers).catch((e) => setError(e.message)), []);
  useEffect(() => { load(); }, [load]);

  async function run(path, method, body, message) {
    setError(''); setOk('');
    try {
      await api(path, { method, body });
      setOk(message);
      load();
    } catch (e) { setError(e.message); }
  }

  async function createUser(e) {
    e.preventDefault();
    await run('/api/admin/users', 'POST', form, `Account created for ${form.email}.`);
    setForm({ name: '', email: '', password: '', role: 'MODERATOR' });
  }

  return (
    <div className="stack">
      <Alert error={error} ok={ok} />
      {can(user, 'USER_MANAGE') && (
        <section className="card">
          <h2>Create an account</h2>
          <form onSubmit={createUser} className="row">
            <label>Name<input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></label>
            <label>Email<input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required /></label>
            <label>Temporary password<input type="password" minLength={8} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required /></label>
            <label>Role
              <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
                {roleOptions.map((r) => <option key={r} value={r}>{ROLE_LABEL[r]}</option>)}
              </select>
            </label>
            <button type="submit">Create</button>
          </form>
        </section>
      )}
      <section className="card">
        <h2>Users & roles</h2>
        <table>
          <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th>Verified</th><th>Actions</th></tr></thead>
          <tbody>
            {users.map((u) => {
              const self = u.id === user.id;
              const lockedAdmin = u.role === 'ADMIN' && !isAdmin;
              return (
                <tr key={u.id}>
                  <td>{u.name}</td>
                  <td>{u.email}</td>
                  <td>
                    {can(user, 'ROLE_ASSIGN') && !self && !lockedAdmin ? (
                      <select value={u.role} onChange={(e) => run(`/api/admin/users/${u.id}/role`, 'PATCH', { role: e.target.value }, `${u.name} is now ${ROLE_LABEL[e.target.value]}.`)}>
                        {roleOptions.map((r) => <option key={r} value={r}>{ROLE_LABEL[r]}</option>)}
                      </select>
                    ) : <span className={`rolepill role-${u.role.toLowerCase()}`}>{ROLE_LABEL[u.role]}</span>}
                  </td>
                  <td>{u.active ? <Badge value="active" /> : <Badge value="inactive" />}</td>
                  <td>{u.verified == null ? '-' : u.verified ? 'Yes' : 'No'}</td>
                  <td className="row">
                    {can(user, 'USER_MANAGE') && !self && !lockedAdmin && (
                      <button className="secondary" onClick={() => run(`/api/admin/users/${u.id}/active`, 'PATCH', { value: !u.active }, `${u.name} ${u.active ? 'deactivated' : 'activated'}.`)}>
                        {u.active ? 'Deactivate' : 'Activate'}
                      </button>
                    )}
                    {can(user, 'DONOR_VERIFY') && u.verified != null && (
                      <button className="secondary" onClick={() => run(`/api/admin/donors/${u.id}/verify`, 'PATCH', { value: !u.verified }, `Verification updated for ${u.name}.`)}>
                        {u.verified ? 'Unverify' : 'Verify'}
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </section>
    </div>
  );
}

function Access({ user }) {
  const [matrix, setMatrix] = useState(null);
  const [draft, setDraft] = useState({});
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const isAdmin = user.role === 'ADMIN';

  const load = useCallback(async () => {
    try {
      const m = await api('/api/admin/permissions');
      setMatrix(m);
      const d = {};
      m.roles.forEach((r) => { d[r.role] = r.permissions; });
      setDraft(d);
    } catch (e) { setError(e.message); }
  }, []);
  useEffect(() => { load(); }, [load]);

  function toggle(role, perm) {
    const current = draft[role] || [];
    setDraft({ ...draft, [role]: current.includes(perm) ? current.filter((p) => p !== perm) : [...current, perm] });
  }

  async function save() {
    setError(''); setOk('');
    try {
      for (const r of matrix.roles) {
        if (!r.editable) continue;
        const before = [...r.permissions].sort().join(',');
        const after = [...(draft[r.role] || [])].sort().join(',');
        if (before !== after) {
          await api(`/api/admin/permissions/${r.role}`, { method: 'PUT', body: { permissions: draft[r.role] || [] } });
        }
      }
      setOk('Permissions saved. They apply to the next request each user makes.');
      load();
    } catch (e) { setError(e.message); }
  }

  if (!matrix) return <section className="card"><Alert error={error} /><p className="muted">Loading...</p></section>;
  const groups = [...new Set(matrix.permissions.map((p) => p.group))];

  return (
    <section className="card">
      <h2>Role and permission matrix</h2>
      <p className="muted">
        Tick the actions each role may perform. The administrator role is fixed so nobody can lock themselves out.
        {!isAdmin && ' Only an administrator can save changes.'}
      </p>
      <Alert error={error} ok={ok} />
      <table className="matrix">
        <thead>
          <tr><th>Permission</th>{matrix.roles.map((r) => <th key={r.role}>{ROLE_LABEL[r.role]}</th>)}</tr>
        </thead>
        <tbody>
          {groups.map((g) => (
            <React.Fragment key={g}>
              <tr className="group-row"><td colSpan={matrix.roles.length + 1}>{g}</td></tr>
              {matrix.permissions.filter((p) => p.group === g).map((p) => (
                <tr key={p.name}>
                  <td><strong>{p.name.toLowerCase().replace(/_/g, ' ')}</strong><div className="muted">{p.description}</div></td>
                  {matrix.roles.map((r) => (
                    <td key={r.role} className="center">
                      <input type="checkbox" checked={(draft[r.role] || []).includes(p.name)}
                             disabled={!r.editable || !isAdmin} onChange={() => toggle(r.role, p.name)}
                             aria-label={`${ROLE_LABEL[r.role]} ${p.name}`} />
                    </td>
                  ))}
                </tr>
              ))}
            </React.Fragment>
          ))}
        </tbody>
      </table>
      {isAdmin && <div className="row"><button onClick={save}>Save permissions</button><button className="secondary" onClick={load}>Reset changes</button></div>}
    </section>
  );
}

function Audit() {
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');
  const load = useCallback(() => api('/api/admin/audit').then(setRows).catch((e) => setError(e.message)), []);
  usePolling(load, 15000);
  return (
    <section className="card">
      <h2>Security audit log</h2>
      <p className="muted">Most recent 100 events: logins, role changes, verification, permission edits and request actions.</p>
      <Alert error={error} />
      {rows.length === 0 ? <Empty>No events yet.</Empty> : (
        <table>
          <thead><tr><th>When</th><th>Who</th><th>Action</th><th>Target</th><th>Detail</th></tr></thead>
          <tbody>
            {rows.map((a) => (
              <tr key={a.id}>
                <td>{fmt(a.createdAt)}</td><td>{a.actor}</td><td><code>{a.action}</code></td><td>{a.target}</td><td>{a.detail}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

export default function ControlRoom({ user }) {
  const tabs = TABS.filter((t) => canAny(user, t.perms));
  const [tab, setTab] = useState(tabs.length ? tabs[0].id : '');

  return (
    <div className="stack">
      <div className="page-head">
        <h1>Control room</h1>
        <span className="muted">You see only the tools your role is permitted to use.</span>
      </div>
      <div className="tabs" role="tablist">
        {tabs.map((t) => (
          <button key={t.id} role="tab" aria-selected={tab === t.id} className={tab === t.id ? 'tab active' : 'tab'} onClick={() => setTab(t.id)}>
            {t.label}
          </button>
        ))}
      </div>
      {tab === 'overview' && <Overview user={user} />}
      {tab === 'stock' && <Stock user={user} />}
      {tab === 'donations' && <Donations user={user} />}
      {tab === 'deliveries' && <Deliveries />}
      {tab === 'users' && <Users user={user} />}
      {tab === 'access' && <Access user={user} />}
      {tab === 'audit' && <Audit />}
    </div>
  );
}
