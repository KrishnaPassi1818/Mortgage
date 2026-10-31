import { requestJSON, getEnvUrl } from './http.js'

const BASE = getEnvUrl('VITE_TRACKING_API_URL', 'http://localhost:8082')

export function getTracking(trackingId) {
  const id = encodeURIComponent(trackingId.trim())
  return requestJSON(`${BASE}/api/v1/tracking/${id}`, { method: 'GET' })
}

export function updateStatus(trackingId, payload) {
  // payload: { status, location, note }
  const id = encodeURIComponent(trackingId.trim())
  return requestJSON(`${BASE}/api/v1/tracking/${id}/events`, {
    method: 'POST',
    body: payload,
  })
}

export function healthTracking() {
  return requestJSON(`${BASE}/health`, { method: 'GET' })
}

export function getBase() { return BASE }
