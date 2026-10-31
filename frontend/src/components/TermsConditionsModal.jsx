import React from 'react'
import { FileTextIcon } from './Icons.jsx'

export default function TermsConditionsModal({ onClose }) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-window" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title-wrap">
            <FileTextIcon size={20} className="text-accent" />
            <h2 className="modal-title">Terms & Conditions</h2>
          </div>
          <button className="modal-close-btn" onClick={onClose} aria-label="Close modal">&times;</button>
        </div>

        <div className="modal-body-scroll">
          <p className="legal-updated">Last Updated: October 2026</p>

          <section className="legal-section">
            <h3>1. Platform Scope</h3>
            <p>
              ParcelFlow provides enterprise parcel tracking, waypoint registration, and dispatch event distribution. 
              Use of the service constitutes agreement to these binding terms.
            </p>
          </section>

          <section className="legal-section">
            <h3>2. Authorized Use</h3>
            <p>
              Users may not submit fraudulent tracking inquiries, inject harmful payloads, or overload API endpoints. 
              The operator console is restricted to authenticated transit personnel.
            </p>
          </section>

          <section className="legal-section">
            <h3>3. Service Availability and Telemetry</h3>
            <p>
              While our microservices cluster targets 99.9% uptime, actual network latency and transit carrier delays may affect 
              real-time checkpoint synchronization. Timelines reflect recorded event timestamps.
            </p>
          </section>

          <section className="legal-section">
            <h3>4. Limitation of Liability</h3>
            <p>
              ParcelFlow is not liable for indirect or consequential damages resulting from network downtime or third-party carrier delays.
            </p>
          </section>
        </div>

        <div className="modal-footer">
          <button className="action-btn btn-secondary" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  )
}
