import React from 'react'
import StatusBadge from './StatusBadge.jsx'

function formatDate(dt) {
  if (!dt) return 'N/A'
  try {
    const d = new Date(dt)
    if (isNaN(d.getTime())) return String(dt)
    return d.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
  } catch {
    return String(dt)
  }
}

export default function ShipmentSummary({ shipment, tracking }) {
  if (!shipment && !tracking) return null

  const trackingId = shipment?.trackingId || tracking?.trackingId
  const currentStatus = tracking?.currentStatus

  return (
    <div className="telemetry-panel">
      <div className="telemetry-header">
        <div className="telemetry-id-group">
          <span className="telemetry-kicker">Order ID</span>
          <span className="telemetry-id">{trackingId || 'N/A'}</span>
        </div>
        {currentStatus && <StatusBadge status={currentStatus} />}
      </div>

      <div className="telemetry-grid">
        <div className="telemetry-cell">
          <span className="cell-label">From</span>
          <span className="cell-value">{shipment?.origin || 'N/A'}</span>
        </div>
        <div className="telemetry-cell">
          <span className="cell-label">To</span>
          <span className="cell-value">{shipment?.destination || 'N/A'}</span>
        </div>
        <div className="telemetry-cell">
          <span className="cell-label">Sender</span>
          <span className="cell-value">{shipment?.senderName || 'N/A'}</span>
        </div>
        <div className="telemetry-cell">
          <span className="cell-label">Recipient</span>
          <span className="cell-value">{shipment?.recipientName || 'N/A'}</span>
        </div>
        <div className="telemetry-cell">
          <span className="cell-label">Created At</span>
          <span className="cell-value cell-timestamp">{formatDate(shipment?.createdAt)}</span>
        </div>
      </div>
    </div>
  )
}
