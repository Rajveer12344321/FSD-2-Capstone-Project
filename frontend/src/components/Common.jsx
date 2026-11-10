import React from 'react';
import { groupLabel } from '../constants.js';

export function Drop({ size = 24, className = '', fill = 'currentColor' }) {
  return (
    <svg className={className} width={size} height={size * 1.25} viewBox="0 0 64 80" aria-hidden="true">
      <path d="M32 4C32 4 8 34 8 52a24 24 0 0 0 48 0C56 34 32 4 32 4Z" fill={fill} />
    </svg>
  );
}

export function GroupDrop({ group, size = 48 }) {
  const label = groupLabel(group);
  return (
    <span className="groupdrop" style={{ width: size, height: size * 1.25 }} title={`Blood group ${label}`}>
      <Drop size={size} />
      <b style={{ fontSize: Math.round(size * 0.3) }}>{label}</b>
    </span>
  );
}

export function Disclaimer() {
  return (
    <div className="disclaimer" role="note">
      <Drop size={12} /> BloodBridge is a coordination prototype with synthetic demonstration data and simulated
      locations. Matches are preliminary candidates, not medically cleared donors. Qualified healthcare professionals
      must confirm donor eligibility and clinical compatibility.
    </div>
  );
}

export function Badge({ value }) {
  return <span className={`badge badge-${String(value).toLowerCase()}`}>{String(value).replace(/_/g, ' ').toLowerCase()}</span>;
}

export function UrgencyTag({ value }) {
  return <span className={`urgency urgency-${String(value).toLowerCase()}`}>{String(value).toLowerCase()}</span>;
}

export function Alert({ error, ok }) {
  if (!error && !ok) return null;
  return <div className={error ? 'alert alert-error' : 'alert alert-ok'} role="alert">{error || ok}</div>;
}

export function Empty({ icon = true, children }) {
  return (
    <div className="empty">
      {icon && <Drop size={28} className="empty-drop" />}
      <p>{children}</p>
    </div>
  );
}

export function Stat({ label, value, hint, tone }) {
  return (
    <div className={`stat ${tone ? `stat-${tone}` : ''}`} title={hint}>
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  );
}

const STEPS = ['OPEN', 'MATCHING', 'FULFILLED'];
const STEP_LABEL = { OPEN: 'Request open', MATCHING: 'Donors invited', FULFILLED: 'Fulfilled' };

export function StatusSteps({ status }) {
  if (status === 'CANCELLED' || status === 'EXPIRED') {
    return <div className="steps-end"><Badge value={status} /></div>;
  }
  const idx = STEPS.indexOf(status);
  return (
    <ol className="steps">
      {STEPS.map((s, i) => (
        <li key={s} className={i <= idx ? 'done' : ''}>
          <span className="dot" />{STEP_LABEL[s]}
        </li>
      ))}
    </ol>
  );
}

/** One small drop per unit needed; drops fill as donors accept. */
export function UnitBags({ units, accepted }) {
  const shown = Math.min(units, 12);
  return (
    <span className="bags" title={`${accepted} of ${units} unit(s) have an accepting donor`}>
      {Array.from({ length: shown }).map((_, i) => (
        <Drop key={i} size={14} className={i < accepted ? 'bag filled' : 'bag'} />
      ))}
      {units > shown && <span className="muted">+{units - shown}</span>}
    </span>
  );
}

export function ScoreBar({ score }) {
  return (
    <div className="scorebar" title={`Match score ${score} / 100`}>
      <div style={{ width: `${Math.max(0, Math.min(100, score))}%` }} />
      <span>{score}</span>
    </div>
  );
}

export function Skeleton({ height = 16, width = '100%' }) {
  return <div className="skeleton" style={{ height, width }} />;
}

export function SectionHead({ title, children }) {
  return (
    <div className="section-head">
      <h2>{title}</h2>
      <div className="row">{children}</div>
    </div>
  );
}
