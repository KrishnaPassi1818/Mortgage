import React from 'react'

const STATUS_LABELS = {
  BOOKED: 'Booked',
  IN_TRANSIT: 'In Transit',
  OUT_FOR_DELIVERY: 'Out for Delivery',
  DELIVERED: 'Delivered',
  DELAYED: 'Delayed',
}

export default function InteractiveMap({ shipment, tracking }) {
  const origin = shipment?.origin || 'Warsaw, PL'
  const destination = shipment?.destination || 'Berlin, DE'
  const currentStatus = tracking?.currentStatus || (shipment ? 'BOOKED' : 'IN_TRANSIT')
  const events = tracking?.events || []
  const isDelivered = currentStatus === 'DELIVERED'
  const isBooked = currentStatus === 'BOOKED'
  const isDelayed = currentStatus === 'DELAYED'
  const isOutForDelivery = currentStatus === 'OUT_FOR_DELIVERY'

  const latestLocation = events.length > 0 && events[events.length - 1].location 
    ? events[events.length - 1].location 
    : (isDelivered ? destination : isBooked ? origin : 'Dresden Transfer Hub')

  // Claude warm aesthetic status tones
  const statusColor = isDelivered
    ? '#2e694b'
    : isDelayed
    ? '#a82a2a'
    : isOutForDelivery
    ? '#b45309'
    : '#c15f3c'

  const activeRoutePath = isDelivered || isOutForDelivery
    ? 'M 180 420 Q 360 380, 470 290 T 720 190'
    : 'M 180 420 Q 360 380, 470 290'

  const statusDisplay = STATUS_LABELS[currentStatus] || currentStatus.replace('_', ' ')

  return (
    <div className="map-viewport-container">
      {/* Warm Parchment Cartographic SVG */}
      <svg className="map-vector" viewBox="0 0 900 620" preserveAspectRatio="xMidYMid slice" fill="none" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <pattern id="gridPattern" width="40" height="40" patternUnits="userSpaceOnUse">
            <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#b1ada1" strokeWidth="0.6" strokeOpacity="0.35" />
          </pattern>
        </defs>

        {/* Ambient Warm Parchment Background */}
        <rect width="100%" height="100%" fill="#eceae3" />
        <rect width="100%" height="100%" fill="url(#gridPattern)" />

        {/* Topographic Corridors */}
        <path d="M 120 480 Q 280 430, 420 320 T 780 180" stroke="#b1ada1" strokeWidth="2.5" fill="none" strokeOpacity="0.45" />
        <path d="M 210 550 Q 360 480, 520 280 T 840 220" stroke="#b1ada1" strokeWidth="1.2" fill="none" strokeOpacity="0.3" />
        <path d="M 60 210 Q 310 240, 560 160 T 820 120" stroke="#b1ada1" strokeWidth="1.2" fill="none" strokeOpacity="0.3" />

        {/* Base Route Vector (Full route track) */}
        <path
          d="M 180 420 Q 360 380, 470 290 T 720 190"
          stroke={isDelivered ? '#2e694b' : '#b1ada1'}
          strokeWidth="3.5"
          strokeLinecap="round"
          fill="none"
          strokeOpacity={isDelivered ? '1' : '0.6'}
        />

        {/* Traveled / Active Route Vector */}
        {isDelivered ? (
          <path
            d="M 180 420 Q 360 380, 470 290 T 720 190"
            stroke="#2e694b"
            strokeWidth="4"
            strokeLinecap="round"
            fill="none"
          />
        ) : !isBooked ? (
          <path
            d={activeRoutePath}
            stroke={statusColor}
            strokeWidth="3.5"
            strokeDasharray={isDelayed ? '4 4' : '6 4'}
            strokeLinecap="round"
            fill="none"
          />
        ) : null}

        {/* Waypoint 1: Origin */}
        <g transform="translate(180, 420)">
          <circle r="7" fill="#ffffff" stroke={isDelivered ? '#2e694b' : '#1f1d1a'} strokeWidth="2.5" />
          <circle r="3" fill={isDelivered ? '#2e694b' : '#1f1d1a'} />
          <text x="-10" y="24" fill="#575249" fontSize="12" fontFamily="var(--font-mono)" fontWeight="600">
            {origin}
          </text>
        </g>

        {/* If NOT delivered and NOT booked: show Midpoint / Current Transit Node */}
        {!isDelivered && !isBooked && (
          <g transform="translate(470, 290)">
            <circle r="22" fill={isDelayed ? 'rgba(168, 42, 42, 0.16)' : 'rgba(193, 95, 60, 0.16)'} />
            <circle r="8" fill="#ffffff" stroke={statusColor} strokeWidth="3" />
            <circle r="3.5" fill={statusColor} />
            {/* Active telemetry tag */}
            <g transform="translate(16, -24)">
              <rect width="130" height="26" rx="6" fill="#ffffff" stroke={statusColor} strokeWidth="1.2" />
              <text x="8" y="17" fill="#1f1d1a" fontSize="11" fontFamily="var(--font-mono)" fontWeight="600">
                {latestLocation}
              </text>
            </g>
          </g>
        )}

        {/* Waypoint 3: Destination */}
        <g transform="translate(720, 190)">
          {isDelivered ? (
            <>
              <circle r="26" fill="rgba(46, 105, 75, 0.16)" />
              <circle r="11" fill="#ffffff" stroke="#2e694b" strokeWidth="3" />
              <circle r="5" fill="#2e694b" />
              {/* Delivered Completed Badge */}
              <g transform="translate(-65, -45)">
                <rect width="130" height="28" rx="6" fill="#ffffff" stroke="#2e694b" strokeWidth="1.5" />
                <text x="14" y="18" fill="#2e694b" fontSize="11" fontFamily="var(--font-mono)" fontWeight="700">
                  ✓ DELIVERED
                </text>
              </g>
            </>
          ) : (
            <>
              <circle r="8" fill="#ffffff" stroke="#b1ada1" strokeWidth="2.5" />
              <circle r="3" fill="#b1ada1" />
            </>
          )}
          <text x="-20" y={isDelivered ? "32" : "-16"} fill={isDelivered ? "#2e694b" : "#575249"} fontSize="12" fontFamily="var(--font-mono)" fontWeight="600">
            {destination}
          </text>
        </g>
      </svg>

      {/* Floating HUD Overlay on Map */}
      <div className="map-hud-overlay">
        <div className="hud-card">
          <div className="hud-row">
            <span className="hud-label">Route Telemetry</span>
            <span className="hud-status" style={{ color: statusColor, fontWeight: 700 }}>
              {statusDisplay}
            </span>
          </div>
          <div className="hud-route">
            <span className="hud-node">{origin}</span>
            <span className="hud-arrow">→</span>
            <span className="hud-node highlight" style={{ color: isDelivered ? '#2e694b' : undefined }}>{destination}</span>
          </div>
          <div className="hud-details">
            <div className="hud-metric">
              <span className="metric-title">{isDelivered ? 'Delivered At' : isBooked ? 'Origin Node' : 'Active Node'}</span>
              <span className="metric-val">{isDelivered ? destination : latestLocation}</span>
            </div>
            <div className="hud-metric">
              <span className="metric-title">Transit Status</span>
              <span className="metric-val" style={{ color: statusColor }}>
                {statusDisplay}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
