import { requestJSON, getEnvUrl } from './http.js'

const BASE = getEnvUrl('VITE_SHIPMENT_API_URL', 'http://localhost:8081')

export function createShipment(payload) {
  // payload: { senderName, recipientName, origin, destination }
  return requestJSON(`${BASE}/api/v1/shipments`, {
    method: 'POST',
    body: payload,
  })
}

export function getShipment(trackingId) {
  const id = encodeURIComponent(trackingId.trim())
  return requestJSON(`${BASE}/api/v1/shipments/${id}`, { method: 'GET' })
}

export function healthShipment() {
  return requestJSON(`${BASE}/health`, { method: 'GET' })
}

export function getBase() { return BASE }
