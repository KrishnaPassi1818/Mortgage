import React, { useState, useEffect } from 'react'
import FeedbackMessage from '../../components/FeedbackMessage.jsx'
import { PlusIcon, BoxIcon } from '../../components/Icons.jsx'
import { createShipmentRequest, getUserShipmentRequests } from '../../api/userApi.js'

export default function UserProfile({ user, onTrackShipment }) {
  const [requests, setRequests] = useState([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const [form, setForm] = useState({
    senderName: user?.fullName || '',
    recipientName: '',
    origin: '',
    destination: '',
    packageDetails: '',
  })

  useEffect(() => {
    if (user?.id) {
      loadRequests()
    }
  }, [user?.id])

  async function loadRequests() {
    setLoading(true)
    try {
      const data = await getUserShipmentRequests(user.id)
      setRequests(data)
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  function update(field, val) {
    setForm(prev => ({ ...prev, [field]: val }))
  }

  async function handleSendRequest(e) {
    e.preventDefault()
    setError('')
    setSuccess('')

    if (!form.senderName.trim() || !form.recipientName.trim() || !form.origin.trim() || !form.destination.trim()) {
      setError('Please provide sender, recipient, origin, and destination.')
      return
    }

    setSubmitting(true)
    try {
      await createShipmentRequest({
        userId: user.id,
        senderName: form.senderName.trim(),
        recipientName: form.recipientName.trim(),
        origin: form.origin.trim(),
        destination: form.destination.trim(),
        packageDetails: form.packageDetails.trim(),
      })
      setSuccess('Shipment request dispatched to Administrator for approval.')
      setForm({
        senderName: user?.fullName || '',
        recipientName: '',
        origin: '',
        destination: '',
        packageDetails: '',
      })
      loadRequests()
    } catch (err) {
      setError(err.message || 'Failed to submit shipment request.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="user-profile-layout">
      {/* User Info Bar */}
      <div className="panel-card" style={{ marginBottom: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <h2 className="panel-title" style={{ fontSize: '1.25rem', marginBottom: '4px' }}>
              User Profile & Activity
            </h2>
            <div style={{ color: 'var(--pf-text-secondary)', fontSize: '0.875rem' }}>
              Logged in as <strong style={{ color: 'var(--pf-text-primary)' }}>{user.fullName}</strong> (@{user.username}) • {user.email}
            </div>
          </div>
          <div>
            <span className="badge-chip role-badge">
              Role: {user.role}
            </span>
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(300px, 1fr) minmax(360px, 1.4fr)', gap: '24px' }}>
        {/* Left Column: Create Shipment Request */}
        <div className="panel-card">
          <div className="panel-header">
            <h3 className="panel-title" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <PlusIcon size={18} />
              <span>Request New Shipment</span>
            </h3>
            <p className="panel-subtitle">
              Submit parcel details to admin for validation and dispatch assignment.
            </p>
          </div>

          {error && <FeedbackMessage tone="error" message={error} />}
          {success && <FeedbackMessage tone="success" message={success} />}

          <form onSubmit={handleSendRequest} className="auth-form-layout" style={{ marginTop: '16px' }}>
            <div className="input-field-group">
              <label className="input-label-tag">Sender Name *</label>
              <input
                type="text"
                className="input-text-clean"
                value={form.senderName}
                onChange={e => update('senderName', e.target.value)}
                placeholder="Sender full name"
                required
              />
            </div>

            <div className="input-field-group">
              <label className="input-label-tag">Recipient Name *</label>
              <input
                type="text"
                className="input-text-clean"
                value={form.recipientName}
                onChange={e => update('recipientName', e.target.value)}
                placeholder="Recipient full name"
                required
              />
            </div>

            <div className="input-field-group">
              <label className="input-label-tag">Origin City / Hub *</label>
              <input
                type="text"
                className="input-text-clean"
                value={form.origin}
                onChange={e => update('origin', e.target.value)}
                placeholder="e.g. San Francisco, CA"
                required
              />
            </div>

            <div className="input-field-group">
              <label className="input-label-tag">Destination City / Address *</label>
              <input
                type="text"
                className="input-text-clean"
                value={form.destination}
                onChange={e => update('destination', e.target.value)}
                placeholder="e.g. New York, NY"
                required
              />
            </div>

            <div className="input-field-group">
              <label className="input-label-tag">Package Details & Weight</label>
              <input
                type="text"
                className="input-text-clean"
                value={form.packageDetails}
                onChange={e => update('packageDetails', e.target.value)}
                placeholder="e.g. 2.5kg Box, Fragile Electronics"
              />
            </div>

            <button
              type="submit"
              className="primary-action-btn"
              disabled={submitting}
              style={{ width: '100%', justifyContent: 'center', marginTop: '12px' }}
            >
              {submitting ? 'Submitting Request...' : 'Send Shipment Request to Admin'}
            </button>
          </form>
        </div>

        {/* Right Column: User's Shipment Requests & Admin Updates */}
        <div className="panel-card">
          <div className="panel-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <h3 className="panel-title" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <BoxIcon size={18} />
                <span>My Shipment Requests & Status Updates</span>
              </h3>
              <p className="panel-subtitle">
                Real-time updates, admin approval decisions, and assigned tracking IDs.
              </p>
            </div>
            <button
              type="button"
              className="secondary-action-btn"
              style={{ fontSize: '0.8rem', padding: '6px 12px' }}
              onClick={loadRequests}
              disabled={loading}
            >
              {loading ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>

          <div style={{ marginTop: '16px' }}>
            {requests.length === 0 ? (
              <div className="timeline-empty" style={{ padding: '32px 16px', textAlign: 'center' }}>
                <p>No shipment requests submitted yet. Use the form on the left to send a request.</p>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                {requests.map(req => {
                  const statusColors = {
                    PENDING: 'var(--pf-status-delayed-bg, #fff3cd)',
                    APPROVED: 'var(--pf-status-delivered-bg, #d1e7dd)',
                    REJECTED: '#f8d7da',
                  }
                  const textColors = {
                    PENDING: '#856404',
                    APPROVED: '#0f5132',
                    REJECTED: '#842029',
                  }

                  return (
                    <div
                      key={req.id}
                      style={{
                        padding: '16px',
                        borderRadius: '8px',
                        border: '1px solid var(--pf-border-color, #e2e8f0)',
                        backgroundColor: 'var(--pf-card-bg-subtle, rgba(255,255,255,0.02))',
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                        <div>
                          <div style={{ fontWeight: '600', fontSize: '1rem', color: 'var(--pf-text-primary)' }}>
                            {req.origin} → {req.destination}
                          </div>
                          <div style={{ fontSize: '0.8rem', color: 'var(--pf-text-secondary)' }}>
                            Recipient: {req.recipientName} • Package: {req.packageDetails || 'Standard'}
                          </div>
                        </div>

                        <span
                          style={{
                            padding: '4px 10px',
                            borderRadius: '12px',
                            fontSize: '0.75rem',
                            fontWeight: '600',
                            backgroundColor: statusColors[req.status] || '#eee',
                            color: textColors[req.status] || '#333',
                          }}
                        >
                          {req.status}
                        </span>
                      </div>

                      {/* Admin Updates Section */}
                      <div
                        style={{
                          marginTop: '12px',
                          padding: '10px 12px',
                          borderRadius: '6px',
                          backgroundColor: 'rgba(0, 0, 0, 0.2)',
                          borderLeft: '3px solid var(--pf-accent-primary, #6366f1)',
                          fontSize: '0.85rem',
                        }}
                      >
                        <div style={{ fontWeight: '600', marginBottom: '4px', color: 'var(--pf-text-primary)' }}>
                          Admin Status & Remarks:
                        </div>
                        {req.adminNotes ? (
                          <div style={{ color: 'var(--pf-text-primary)' }}>{req.adminNotes}</div>
                        ) : (
                          <div style={{ color: 'var(--pf-text-secondary)', fontStyle: 'italic' }}>
                            Awaiting admin review and dispatch scheduling.
                          </div>
                        )}

                        {req.trackingId && (
                          <div style={{ marginTop: '8px', display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                            <span style={{ color: 'var(--pf-text-secondary)' }}>Assigned Tracking ID:</span>
                            <strong style={{ color: 'var(--pf-accent-primary, #6366f1)', fontFamily: 'monospace' }}>
                              {req.trackingId}
                            </strong>
                            <button
                              type="button"
                              className="secondary-action-btn"
                              style={{ padding: '2px 8px', fontSize: '0.75rem' }}
                              onClick={() => onTrackShipment(req.trackingId)}
                            >
                              Track Live →
                            </button>
                          </div>
                        )}
                      </div>

                      <div style={{ fontSize: '0.75rem', color: 'var(--pf-text-secondary)', marginTop: '8px', textAlign: 'right' }}>
                        Updated: {new Date(req.updatedAt || req.createdAt).toLocaleString()}
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
