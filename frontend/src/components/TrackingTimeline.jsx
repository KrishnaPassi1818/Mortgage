import React from 'react'
import StatusBadge from './StatusBadge.jsx'

function formatTime(ts) {
  if (!ts) return ''
  try {
    const d = new Date(ts)
    if (isNaN(d.getTime())) return String(ts)
    return d.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
  } catch {
    return String(ts)
  }
}

export default function TrackingTimeline({ events }) {
  if (!events || events.length === 0) {
    return (
      <div className="timeline-empty">
        <p>No tracking events recorded yet. An initial BOOKED event appears upon shipment creation.</p>
      </div>
    )
  }

  const sorted = [...events].sort((a, b) => new Date(a.eventTime) - new Date(b.eventTime))

  return (
    <div className="timeline-container">
      <div className="timeline-title-row">
        <h3>Tracking Events</h3>
        <span className="events-counter">{sorted.length} {sorted.length === 1 ? 'checkpoint' : 'checkpoints'}</span>
      </div>

      <div className="checkpoints-list">
        {sorted.map((ev, idx) => {
          const isLatest = idx === sorted.length - 1
          const location = ev.location || 'Checkpoint recorded'
          const note = ev.note || ''

          return (
            <div key={`${ev.eventTime}-${idx}`} className={`checkpoint-item ${isLatest ? 'is-latest' : ''}`}>
              <div className="checkpoint-axis">
                <div className="checkpoint-marker" />
                {idx !== sorted.length - 1 && <div className="checkpoint-connector" />}
              </div>

              <div className="checkpoint-content">
                <div className="checkpoint-top">
                  <span className="checkpoint-location">{location}</span>
                  <span className="checkpoint-time">{formatTime(ev.eventTime)}</span>
                </div>
                <div className="checkpoint-badge-wrap">
                  <StatusBadge status={ev.status} />
                  {isLatest && <span className="latest-tag">Current Node</span>}
                </div>
                {note && <div className="checkpoint-note">{note}</div>}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
