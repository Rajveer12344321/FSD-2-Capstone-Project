import React from 'react';
import { Link } from 'react-router-dom';
import { Drop, GroupDrop } from '../components/Common.jsx';
import Icon from '../components/Icon.jsx';

const STEPS = [
  ['1', 'Request', 'A requester posts the blood group, component, units, hospital, urgency and deadline.'],
  ['2', 'Match', 'Donors are filtered by compatibility, availability and consent, then ranked with clear reasons.'],
  ['3', 'Respond', 'Invited donors accept or decline once. Coordinators record completed donations.'],
];

const FEATURES = [
  ['users', 'Role-based access', 'Donors, requesters, coordinators and administrators see only what their permissions allow, and sensitive actions are audited.'],
  ['box', 'Blood stock tracking', 'Coordinators track units per group and component with low-stock alerts.'],
  ['calendar', 'Blood drives', 'Publish community donation camps and let donors register within capacity.'],
  ['chart', 'Live analytics', 'Response rates, urgency mix, demand by blood group and exportable CSV reports.'],
  ['heart', 'Donor recognition', 'Recorded donations earn badges and build a personal donation history.'],
  ['shield', 'Privacy first', 'Exact locations and emails are never shown to others. Phone numbers reach only the request owner after consent.'],
];

export default function Landing() {
  return (
    <div className="landing">
      <section className="hero">
        <div className="hero-text">
          <p className="eyebrow">Smart blood donation network</p>
          <h1>Every drop is a bridge between a donor and a patient in need.</h1>
          <p className="lead">
            BloodBridge organizes blood requests, finds potentially compatible and available donors, and keeps
            everyone informed, with privacy and role-based access built in.
          </p>
          <div className="row">
            <Link to="/register" className="solidlink big">Become a donor</Link>
            <Link to="/register" className="outlinelink big">Request blood</Link>
          </div>
          <p className="muted">Already registered? <Link to="/login">Log in</Link></p>
        </div>
        <div className="hero-art" aria-hidden="true">
          <Drop size={170} className="hero-drop" />
          <div className="hero-groups">
            {['O_NEG', 'A_POS', 'B_POS', 'AB_POS'].map((g) => <GroupDrop key={g} group={g} size={44} />)}
          </div>
        </div>
      </section>

      <section>
        <h2 className="section-title">How it works</h2>
        <div className="cards3">
          {STEPS.map(([n, title, text]) => (
            <div className="card step-card" key={n}>
              <span className="step-no">{n}</span>
              <h3>{title}</h3>
              <p>{text}</p>
            </div>
          ))}
        </div>
      </section>

      <section>
        <h2 className="section-title">Built for coordination at scale</h2>
        <div className="cards3">
          {FEATURES.map(([icon, title, text]) => (
            <div className="card" key={title}>
              <div className="feature-icon"><Icon name={icon} size={20} /></div>
              <h3>{title}</h3>
              <p className="muted">{text}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="card">
        <h3>Prototype notice</h3>
        <p className="muted">
          BloodBridge is a coordination demo with synthetic data and simulated locations. It is not a medical
          system. Matches are preliminary candidates, and clinical eligibility and compatibility must be confirmed by
          qualified healthcare professionals.
        </p>
      </section>
    </div>
  );
}
