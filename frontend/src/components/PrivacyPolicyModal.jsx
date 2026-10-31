import React from 'react'
import { ShieldIcon } from './Icons.jsx'

export default function PrivacyPolicyModal({ onClose }) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-window" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title-wrap">
            <ShieldIcon size={20} className="text-accent" />
            <h2 className="modal-title">Privacy Policy</h2>
          </div>
          <button className="modal-close-btn" onClick={onClose} aria-label="Close modal">&times;</button>
        </div>

        <div className="modal-body-scroll">
          <p className="legal-updated">Last Updated: October 2026</p>

          <section className="legal-section">
            <h3>1. Information We Collect</h3>
            <p>
              ParcelFlow collects operational data necessary to process and verify logistics shipments. 
              This includes sender and recipient names, shipping addresses, geographic checkpoints, and event timestamps.
            </p>
          </section>

          <section className="legal-section">
            <h3>2. Purpose of Processing</h3>
            <p>
              All shipment telemetry is utilized strictly for route determination, status updates, 
              and dispatch notification logging. We do not sell or monetize personal shipping data.
            </p>
          </section>

          <section className="legal-section">
            <h3>3. Data Retention and Security</h3>
            <p>
              Shipment and tracking records are maintained in secured database storage with role-based access limits. 
              Data is retained for operational audit trails and deleted upon customer account termination.
            </p>
          </section>

          <section className="legal-section">
            <h3>4. Contact and Rights</h3>
            <p>
              For data access requests or inquiries regarding privacy practices, contact our data protection team at privacy@parcelflow.io.
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
