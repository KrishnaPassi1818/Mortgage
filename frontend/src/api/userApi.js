import { requestJSON, getEnvUrl } from './http.js'

const BASE = getEnvUrl('VITE_SHIPMENT_API_URL', 'http://localhost:8081')

export function signupUser(payload) {
  // payload: { username, email, password, fullName, role }
  return requestJSON(`${BASE}/api/v1/auth/signup`, {
    method: 'POST',
    body: payload,
  })
}

export function loginUser(payload) {
  // payload: { username, password }
  return requestJSON(`${BASE}/api/v1/auth/login`, {
    method: 'POST',
    body: payload,
  })
}

export function getUserProfile(userId) {
  return requestJSON(`${BASE}/api/v1/users/${userId}`, { method: 'GET' })
}

export function createShipmentRequest(payload) {
  // payload: { userId, senderName, recipientName, origin, destination, packageDetails }
  return requestJSON(`${BASE}/api/v1/requests`, {
    method: 'POST',
    body: payload,
  })
}

export function getUserShipmentRequests(userId) {
  return requestJSON(`${BASE}/api/v1/requests?userId=${encodeURIComponent(userId)}`, {
    method: 'GET',
  })
}

export function getAllShipmentRequests() {
  return requestJSON(`${BASE}/api/v1/requests`, {
    method: 'GET',
  })
}

export function processShipmentRequest(requestId, payload) {
  // payload: { action: 'APPROVE'|'REJECT'|'UPDATE', adminNotes, destination, packageDetails }
  return requestJSON(`${BASE}/api/v1/requests/${requestId}`, {
    method: 'POST',
    body: payload,
  })
}
