import React from 'react';

const COLORS = { ACCEPTED: '#1b8a5a', PENDING: '#e08e0b', DECLINED: '#9aa0a6', CLOSED: '#c7c9cc' };

// Deterministic direction from the invitation id, so dots do not jump between refreshes.
const angleFor = (id) => ((id * 137.508) % 360) * (Math.PI / 180);

/**
 * Distance radar: the hospital sits in the centre, rings show distance bands, and each invited donor is a dot at
 * their (simulated) distance. Direction is illustrative only; real coordinates are never sent to requesters.
 */
export default function Radar({ candidates, maxKm = 50 }) {
  const size = 300;
  const c = size / 2;
  const R = c - 18;
  const rings = [10, 25, 50];
  const dots = candidates.filter((x) => x.distanceKm != null);
  return (
    <figure className="radar">
      <svg viewBox={`0 0 ${size} ${size}`} role="img" aria-label="Donor distance radar">
        {rings.map((km) => (
          <g key={km}>
            <circle cx={c} cy={c} r={(km / maxKm) * R} className="radar-ring" />
            <text x={c + 4} y={c - (km / maxKm) * R + 11} className="radar-label">{km} km</text>
          </g>
        ))}
        {dots.map((d) => {
          const r = (Math.min(d.distanceKm, maxKm) / maxKm) * R;
          const a = angleFor(d.invitationId);
          return (
            <circle key={d.invitationId} cx={c + r * Math.cos(a)} cy={c + r * Math.sin(a)} r={7}
                    fill={COLORS[d.status] || '#999'} stroke="#fff" strokeWidth="2">
              <title>{`${d.donorLabel}: ${d.distanceKm} km, ${d.status.toLowerCase()}`}</title>
            </circle>
          );
        })}
        <circle cx={c} cy={c} r={9} className="radar-hospital" />
        <path d={`M${c - 4} ${c}h8M${c} ${c - 4}v8`} stroke="#fff" strokeWidth="2" strokeLinecap="round" />
      </svg>
      <figcaption>
        <span className="legend-dot" style={{ background: COLORS.ACCEPTED }} /> accepted
        <span className="legend-dot" style={{ background: COLORS.PENDING }} /> pending
        <span className="legend-dot" style={{ background: COLORS.DECLINED }} /> declined
        <div className="muted small">Distance is simulated. Direction is illustrative only.</div>
      </figcaption>
    </figure>
  );
}
