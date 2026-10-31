import React, { useEffect, useState } from 'react'
import { healthShipment } from '../api/shipmentApi.js'
import { healthTracking } from '../api/trackingApi.js'
import { healthNotification } from '../api/notificationApi.js'

function ServiceNode({ label, port, status }) {
  const isOnline = status === 'online'
  const isOffline = status === 'offline'

  return (
    <div className="service-node" title={`Service on port ${port} status: ${status}`}>
      <span className={`node-indicator ${isOnline ? 'online' : isOffline ? 'offline' : 'checking'}`} />
      <span className="node-label">{label}</span>
      <span className="node-port">:{port}</span>
    </div>
  )
}

export default function ServiceStatus() {
  const [states, setStates] = useState({ shipment: 'checking', tracking: 'checking', notification: 'checking' })

  useEffect(() => {
    let cancelled = false
    async function check() {
      const results = await Promise.allSettled([
        healthShipment(),
        healthTracking(),
        healthNotification(),
      ])
      if (cancelled) return
      setStates({
        shipment: results[0].status === 'fulfilled' ? 'online' : 'offline',
        tracking: results[1].status === 'fulfilled' ? 'online' : 'offline',
        notification: results[2].status === 'fulfilled' ? 'online' : 'offline',
      })
    }
    check()
    const id = setInterval(check, 10000)
    return () => { cancelled = true; clearInterval(id) }
  }, [])

  return (
    <div className="system-health-bar" aria-label="Microservice Cluster Status">
      <div className="health-title">Cluster Status</div>
      <div className="nodes-group">
        <ServiceNode label="Shipment" port="8081" status={states.shipment} />
        <ServiceNode label="Tracking" port="8082" status={states.tracking} />
        <ServiceNode label="Notification" port="8083" status={states.notification} />
      </div>
    </div>
  )
}
