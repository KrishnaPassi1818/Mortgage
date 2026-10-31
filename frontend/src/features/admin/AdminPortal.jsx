import React, { useState, useEffect } from 'react'
import FeedbackMessage from '../../components/FeedbackMessage.jsx'
import { ShieldIcon, EditIcon, CheckCircleIcon, SlidersIcon } from '../../components/Icons.jsx'
import { getAllShipmentRequests, processShipmentRequest } from '../../api/userApi.js'
import OperatorStatusForm from '../operator/OperatorStatusForm.jsx'

export default function AdminPortal({ currentUser, onTrackShipment, onShipmentUpdated }) {
  const [requests, setRequests] = useState([])
  const [loading, setLoading] = useState(false)
  const [feedback, setFeedback] = useState(null)
  const [editingId, setEditingId] = useState(null)
  const [activeOperatorReqId, setActiveOperatorReqId] = useState(null)
  const [editForm, setEditForm] = useState({
    adminNotes: '',
    destination: '',
    packageDetails: '',
  })
  const [submittingAction, setSubmittingAction] = useState(false)

  useEffect(() => {
    loadAllRequests()
  }, [])

  async function loadAllRequests() {
    setLoading(true)
    try {
      const data = await getAllShipmentRequests()
      setRequests(data)
    } catch (err) {
      setFeedback({ tone: 'error', message: err.message || 'Failed to load shipment requests.' })
    } finally {
      setLoading(false)
    }
  }

  function startEdit(req) {
    setEditingId(req.id)
    setEditForm({
      adminNotes: req.adminNotes || '',
      destination: req.destination || '',
      packageDetails: req.packageDetails || '',
    })
  }

  function cancelEdit() {
    setEditingId(null)
  }

  async function handleApprove(req) {
    setSubmittingAction(true)
    setFeedback(null)
    try {
      const res = await processShipmentRequest(req.id, {
        action: 'APPROVE',
        adminNotes: editForm.adminNotes.trim() || 'Approved by administrator; shipment registered and dispatched.',
        destination: editForm.destination.trim() || req.destination,
        packageDetails: editForm.packageDetails.trim() || req.packageDetails,
      })
      setFeedback({
        tone: 'success',
        message: `Shipment approved! Generated Tracking ID: ${res.trackingId}. Record updated.`,
      })
      setEditingId(null)
      loadAllRequests()
    } catch (err) {
      setFeedback({ tone: 'error', message: err.message || 'Approval failed.' })
    } finally {
      setSubmittingAction(false)
    }
  }

  async function handleSaveUpdates(req) {
    setSubmittingAction(true)
    setFeedback(null)
    try {
      await processShipmentRequest(req.id, {
        action: 'UPDATE',
        adminNotes: editForm.adminNotes.trim(),
        destination: editForm.destination.trim(),
        packageDetails: editForm.packageDetails.trim(),
      })
      setFeedback({
        tone: 'success',
        message: 'Record updated successfully! Changes will reflect on user profile.',
      })
      setEditingId(null)
      loadAllRequests()
    } catch (err) {
      setFeedback({ tone: 'error', message: err.message || 'Update failed.' })
    } finally {
      setSubmittingAction(false)
    }
  }

  async function handleReject(req) {
    if (!window.confirm('Are you sure you want to reject this shipment request?')) return
    setSubmittingAction(true)
    setFeedback(null)
    try {
      await processShipmentRequest(req.id, {
        action: 'REJECT',
        adminNotes: editForm.adminNotes.trim() || 'Rejected by administrator due to policy or weight constraints.',
      })
      setFeedback({ tone: 'warning', message: 'Shipment request marked as REJECTED.' })
      setEditingId(null)
      loadAllRequests()
    } catch (err) {
      setFeedback({ tone: 'error', message: err.message || 'Rejection failed.' })
    } finally {
      setSubmittingAction(false)
    }
  }

  return (
    <div className="admin-portal-layout" style={{ maxWidth: '1000px', margin: '0 auto' }}>
      <div className="panel-card" style={{ marginBottom: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <h2 className="panel-title" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <ShieldIcon size={20} />
              <span>Admin Shipment Approval & Record Update Console</span>
            </h2>
            <p className="panel-subtitle">
              Authorize user shipment requests, initialize tracking IDs, and update logistics telemetry visible on user profiles.
            </p>
          </div>
          <button
            type="button"
            className="secondary-action-btn"
            onClick={loadAllRequests}
            disabled={loading}
          >
            {loading ? 'Refreshing...' : 'Refresh List'}
          </button>
        </div>
      </div>

      {feedback && <FeedbackMessage tone={feedback.tone} message={feedback.message} />}

      <div className="panel-card">
        <div className="panel-header">
          <div className="shelf-header">
            <span className="shelf-title">Incoming & Active User Shipment Requests</span>
            <span className="shelf-count">{requests.length}</span>
          </div>
        </div>

        <div style={{ marginTop: '16px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {requests.length === 0 ? (
            <div className="timeline-empty" style={{ padding: '40px', textAlign: 'center' }}>
              <p>No shipment requests currently in the database queue.</p>
            </div>
          ) : (
            requests.map(req => {
              const isEditing = editingId === req.id
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
                    padding: '20px',
                    borderRadius: '8px',
                    border: '1px solid var(--pf-border-color, #e2e8f0)',
                    backgroundColor: 'var(--pf-card-bg-subtle, rgba(255,255,255,0.02))',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '12px' }}>
                    <div>
                      <div style={{ fontSize: '1.1rem', fontWeight: '600', color: 'var(--pf-text-primary)' }}>
                        Request #{req.id}: {req.origin} → {req.destination}
                      </div>
                      <div style={{ fontSize: '0.85rem', color: 'var(--pf-text-secondary)', marginTop: '4px' }}>
                        User ID: <strong>{req.userId}</strong> • Sender: <strong>{req.senderName}</strong> • Recipient: <strong>{req.recipientName}</strong>
                      </div>
                      <div style={{ fontSize: '0.85rem', color: 'var(--pf-text-secondary)', marginTop: '2px' }}>
                        Details: {req.packageDetails || 'Not specified'}
                      </div>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span
                        style={{
                          padding: '4px 12px',
                          borderRadius: '12px',
                          fontSize: '0.8rem',
                          fontWeight: '600',
                          backgroundColor: statusColors[req.status] || '#eee',
                          color: textColors[req.status] || '#333',
                        }}
                      >
                        {req.status}
                      </span>
                    </div>
                  </div>

                  {req.trackingId && (
                    <div style={{ marginTop: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span style={{ fontSize: '0.85rem', color: 'var(--pf-text-secondary)' }}>Generated Tracking ID:</span>
                      <strong style={{ color: 'var(--pf-accent-primary, #6366f1)', fontFamily: 'monospace', fontSize: '1rem' }}>
                        {req.trackingId}
                      </strong>
                      <button
                        type="button"
                        className="secondary-action-btn"
                        style={{ padding: '2px 8px', fontSize: '0.75rem' }}
                        onClick={() => onTrackShipment(req.trackingId)}
                      >
                        Open In Tracker →
                      </button>
                    </div>
                  )}

                  {/* Notes / Current status displayed */}
                  <div style={{ marginTop: '12px', padding: '10px 14px', borderRadius: '6px', backgroundColor: 'rgba(0,0,0,0.15)' }}>
                    <span style={{ fontSize: '0.8rem', color: 'var(--pf-text-secondary)', display: 'block', marginBottom: '2px' }}>
                      Admin Notes on Record (User-visible):
                    </span>
                    <span style={{ fontSize: '0.9rem', color: 'var(--pf-text-primary)' }}>
                      {req.adminNotes || <em>None yet.</em>}
                    </span>
                  </div>

                  {/* Edit/Approval Drawer */}
                  {isEditing ? (
                    <div style={{ marginTop: '16px', padding: '16px', borderTop: '1px solid var(--pf-border-color, #e2e8f0)' }}>
                      <h4 style={{ fontSize: '0.95rem', marginBottom: '12px', color: 'var(--pf-text-primary)' }}>
                        Update Record / Process Request
                      </h4>
                      <div className="input-field-group">
                        <label className="input-label-tag">Admin Remarks / Updates *</label>
                        <textarea
                          className="input-text-clean"
                          rows="2"
                          placeholder="Add update notes visible on user profile (e.g. Cleared customs, Dispatched via Fleet #4)"
                          value={editForm.adminNotes}
                          onChange={e => setEditForm(prev => ({ ...prev, adminNotes: e.target.value }))}
                        />
                      </div>

                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                        <div className="input-field-group">
                          <label className="input-label-tag">Edit Destination Address</label>
                          <input
                            type="text"
                            className="input-text-clean"
                            value={editForm.destination}
                            onChange={e => setEditForm(prev => ({ ...prev, destination: e.target.value }))}
                          />
                        </div>
                        <div className="input-field-group">
                          <label className="input-label-tag">Edit Package Details</label>
                          <input
                            type="text"
                            className="input-text-clean"
                            value={editForm.packageDetails}
                            onChange={e => setEditForm(prev => ({ ...prev, packageDetails: e.target.value }))}
                          />
                        </div>
                      </div>

                      <div style={{ display: 'flex', gap: '10px', marginTop: '16px', flexWrap: 'wrap' }}>
                        {req.status === 'PENDING' && (
                          <button
                            type="button"
                            className="primary-action-btn"
                            disabled={submittingAction}
                            onClick={() => handleApprove(req)}
                          >
                            <CheckCircleIcon size={16} />
                            <span>Approve & Create Shipment</span>
                          </button>
                        )}

                        <button
                          type="button"
                          className="primary-action-btn"
                          style={{ backgroundColor: '#2563eb' }}
                          disabled={submittingAction}
                          onClick={() => handleSaveUpdates(req)}
                        >
                          <EditIcon size={16} />
                          <span>Save Record Updates</span>
                        </button>

                        {req.status === 'PENDING' && (
                          <button
                            type="button"
                            className="secondary-action-btn"
                            style={{ color: '#ef4444', borderColor: '#ef4444' }}
                            disabled={submittingAction}
                            onClick={() => handleReject(req)}
                          >
                            Reject Request
                          </button>
                        )}

                        <button
                          type="button"
                          className="secondary-action-btn"
                          disabled={submittingAction}
                          onClick={cancelEdit}
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '16px', flexWrap: 'wrap' }}>
                      {req.trackingId && (
                        <button
                          type="button"
                          className="primary-action-btn"
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '6px',
                            backgroundColor: activeOperatorReqId === req.id ? 'var(--pf-surface-elevated, #334155)' : 'var(--pf-accent-primary, #6366f1)',
                            fontSize: '0.85rem',
                            padding: '6px 14px'
                          }}
                          onClick={() => setActiveOperatorReqId(prev => prev === req.id ? null : req.id)}
                        >
                          <SlidersIcon size={15} />
                          <span>{activeOperatorReqId === req.id ? 'Hide Fleet Dispatch Console' : 'Fleet Dispatch Operator Console'}</span>
                        </button>
                      )}
                      <button
                        type="button"
                        className="secondary-action-btn"
                        style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
                        onClick={() => startEdit(req)}
                      >
                        <EditIcon size={15} />
                        <span>{req.status === 'PENDING' ? 'Review & Approve / Edit' : 'Update Record'}</span>
                      </button>
                    </div>
                  )}

                  {/* Dedicated Fleet Dispatch Operator Console for this parcel request */}
                  {req.trackingId && activeOperatorReqId === req.id && (
                    <div style={{ marginTop: '20px', borderTop: '1px solid var(--pf-border-color, #e2e8f0)', paddingTop: '16px' }}>
                      <OperatorStatusForm
                        defaultTrackingId={req.trackingId}
                        onUpdated={(res) => {
                          setFeedback({
                            tone: 'success',
                            message: `Fleet dispatch status updated for ${req.trackingId} to ${res.currentStatus}.`
                          })
                          loadAllRequests()
                          if (onShipmentUpdated) onShipmentUpdated(res)
                        }}
                      />
                    </div>
                  )}
                </div>
              )
            })
          )}
        </div>
      </div>
    </div>
  )
}
