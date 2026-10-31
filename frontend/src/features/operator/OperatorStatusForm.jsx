import React, { useState, useEffect } from 'react'
import FeedbackMessage from '../../components/FeedbackMessage.jsx'
import { updateStatus, getTracking } from '../../api/trackingApi.js'

const STATUSES = ['IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'DELAYED']

export default function OperatorStatusForm({ defaultTrackingId, onUpdated }) {
  const [form, setForm] = useState({
    trackingId: defaultTrackingId || '',
    status: 'IN_TRANSIT',
    location: '',
    note: '',
  })
  const [currentTrackingStatus, setCurrentTrackingStatus] = useState(null)
  const [loading, setLoading] = useState(false)
  const [feedback, setFeedback] = useState(null)

  useEffect(() => {
    if (defaultTrackingId) {
      setForm(prev => ({ ...prev, trackingId: defaultTrackingId }))
      checkCurrentStatus(defaultTrackingId)
    }
  }, [defaultTrackingId])

  async function checkCurrentStatus(id) {
    if (!id || !id.trim()) return
    try {
      const data = await getTracking(id.trim())
      if (data && data.currentStatus) {
        setCurrentTrackingStatus(data.currentStatus)
      }
    } catch {
      // Ignore if tracking not found or network issue
    }
  }

  function update(key, val) {
    setForm(prev => ({ ...prev, [key]: val }))
    if (key === 'trackingId') {
      checkCurrentStatus(val)
    }
  }

  async function submit(e) {
    e.preventDefault()
    setFeedback(null)
    if (!form.trackingId.trim()) {
      setFeedback({ tone: 'error', message: 'Tracking ID is required.' })
      return
    }

    if (currentTrackingStatus === 'DELIVERED') {
      setFeedback({
        tone: 'warning',
        message: 'This shipment is already DELIVERED (terminal state). No further transitions are allowed.'
      })
      return
    }

    setLoading(true)
    try {
      const res = await updateStatus(form.trackingId.trim(), {
        status: form.status,
        location: form.location.trim() || undefined,
        note: form.note.trim() || undefined,
      })

      setCurrentTrackingStatus(res.currentStatus)
      const isPartial = res.notificationRecorded === false
      setFeedback({
        tone: isPartial ? 'warning' : 'success',
        message: isPartial
          ? `Status updated to ${res.currentStatus}. Notification service was unreachable but status is recorded.`
          : `Status updated to ${res.currentStatus}. Checkpoint broadcasted.`
      })
      if (onUpdated) onUpdated(res)
    } catch (err) {
      let msg = err.message
      if (err.status === 409) {
        msg = `Invalid state transition: ${err.message}`
        if (err.message && err.message.includes('already delivered')) {
          setCurrentTrackingStatus('DELIVERED')
        }
      }
      if (err.status === 404) msg = `Shipment not found: ${err.message}`
      if (err.status === 503) msg = `Downstream dependency unavailable: ${err.message}`
      setFeedback({ tone: 'error', message: msg || 'Status update failed.' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="panel-card">
      <div className="panel-header">
        <div className="panel-badge-row">
          <h2 className="panel-title">Fleet Dispatch Operator Console</h2>
          <span className="environment-tag">Internal Operations</span>
        </div>
        <p className="panel-subtitle">Simulate waypoint checkpoint events across the transit network.</p>
      </div>

      <form onSubmit={submit} className="form-stack" noValidate>
        <div className="field-group">
          <label htmlFor="op-trackingId">Target Tracking ID</label>
          <input
            id="op-trackingId"
            className="text-input"
            value={form.trackingId}
            onChange={e => update('trackingId', e.target.value)}
            placeholder="e.g. PF26A1B2C3"
            required
          />
        </div>

        <div className="form-grid-2">
          <div className="field-group">
            <label htmlFor="op-status">Target Status</label>
            <select
              id="op-status"
              className="select-input"
              value={form.status}
              onChange={e => update('status', e.target.value)}
            >
              {STATUSES.map(s => (
                <option key={s} value={s}>{s.replace('_', ' ')}</option>
              ))}
            </select>
          </div>

          <div className="field-group">
            <label htmlFor="op-location">Current Waypoint / Facility</label>
            <input
              id="op-location"
              className="text-input"
              value={form.location}
              onChange={e => update('location', e.target.value)}
              placeholder="e.g. Dresden Regional Sort Center"
              maxLength={120}
            />
          </div>
        </div>

        <div className="field-group">
          <label htmlFor="op-note">Operational Note</label>
          <textarea
            id="op-note"
            className="textarea-input"
            value={form.note}
            onChange={e => update('note', e.target.value)}
            placeholder="e.g. Package cleared customs inspection and transferred to line-haul vehicle."
            maxLength={255}
            rows={2}
          />
        </div>

        <div className="form-actions">
          <button
            type="submit"
            className="action-btn btn-accent"
            disabled={loading || !form.trackingId.trim() || currentTrackingStatus === 'DELIVERED'}
          >
            {loading
              ? 'Transmitting...'
              : currentTrackingStatus === 'DELIVERED'
              ? 'Delivered (Terminal)'
              : 'Emit Status Update'}
          </button>
        </div>

        {feedback && (
          <div className="form-feedback-wrap">
            <FeedbackMessage tone={feedback.tone}>{feedback.message}</FeedbackMessage>
          </div>
        )}
      </form>
    </div>
  )
}
