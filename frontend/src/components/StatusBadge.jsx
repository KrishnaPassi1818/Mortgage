import React from 'react'

const STATUS_MAP = {
  BOOKED: { label: 'Booked', cls: 'badge-booked' },
  IN_TRANSIT: { label: 'In Transit', cls: 'badge-in_transit' },
  OUT_FOR_DELIVERY: { label: 'Out for Delivery', cls: 'badge-out_for_delivery' },
  DELIVERED: { label: 'Delivered', cls: 'badge-delivered' },
  DELAYED: { label: 'Delayed', cls: 'badge-delayed' },
}

export default function StatusBadge({ status }) {
  if (!status) return null
  const key = String(status).trim().toUpperCase()
  const conf = STATUS_MAP[key] || { label: status, cls: 'badge-booked' }

  return (
    <span className={`status-pill ${conf.cls}`} role="status">
      <span className="status-dot" aria-hidden="true" />
      {conf.label}
    </span>
  )
}
