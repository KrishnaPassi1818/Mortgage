import { requestJSON, getEnvUrl } from './http.js'

const BASE = getEnvUrl('VITE_NOTIFICATION_API_URL', 'http://localhost:8083')

export function listNotifications(trackingId) {
  const id = encodeURIComponent(trackingId.trim())
  return requestJSON(`${BASE}/api/v1/notifications?trackingId=${id}`, { method: 'GET' })
}

export function createNotification(payload) {
  // payload: { trackingId, status, title, message }
  return requestJSON(`${BASE}/api/v1/notifications`, {
    method: 'POST',
    body: payload,
  })
}

export function healthNotification() {
  return requestJSON(`${BASE}/health`, { method: 'GET' })
}

export function getBase() { return BASE }
