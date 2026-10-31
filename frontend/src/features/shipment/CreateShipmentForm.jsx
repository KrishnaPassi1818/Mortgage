import React, { useState } from 'react'
import FeedbackMessage from '../../components/FeedbackMessage.jsx'
import { CopyIcon, CheckCircleIcon } from '../../components/Icons.jsx'
import { createShipment } from '../../api/shipmentApi.js'

export default function CreateShipmentForm({ onCreated }) {
  const [form, setForm] = useState({ senderName: '', recipientName: '', origin: '', destination: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(null)
  const [copied, setCopied] = useState(false)

  function update(key, val) {
    setForm(prev => ({ ...prev, [key]: val }))
  }

  async function submit(e) {
    e.preventDefault()
    setError('')
    setSuccess(null)
    setCopied(false)

    if (!form.senderName.trim() || !form.recipientName.trim() || !form.origin.trim() || !form.destination.trim()) {
      setError('All fields are required.')
      return
    }

    setLoading(true)
    try {
      const res = await createShipment({
        senderName: form.senderName.trim(),
        recipientName: form.recipientName.trim(),
        origin: form.origin.trim(),
        destination: form.destination.trim(),
      })
      setSuccess(res)
      setForm({ senderName: '', recipientName: '', origin: '', destination: '' })
      if (onCreated) onCreated(res)
    } catch (err) {
      setError(err.message || 'Creation failed. Ensure Shipment Service on port 8081 is active.')
    } finally {
      setLoading(false)
    }
  }

  async function copyTrackingId() {
    if (!success?.trackingId) return
    try {
      await navigator.clipboard.writeText(success.trackingId)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      // fallback
    }
  }

  return (
    <div className="panel-card">
      <div className="panel-header">
        <h2 className="panel-title">Register New Shipment</h2>
        <p className="panel-subtitle">Create a shipment record and initialize tracking in the microservices cluster.</p>
      </div>

      <form onSubmit={submit} className="form-stack" noValidate>
        <div className="form-grid-2">
          <div className="field-group">
            <label htmlFor="senderName">Sender Name</label>
            <input
              id="senderName"
              className="text-input"
              value={form.senderName}
              onChange={e => update('senderName', e.target.value)}
              placeholder="e.g. Acme Logistics"
              maxLength={100}
              required
            />
          </div>

          <div className="field-group">
            <label htmlFor="recipientName">Recipient Name</label>
            <input
              id="recipientName"
              className="text-input"
              value={form.recipientName}
              onChange={e => update('recipientName', e.target.value)}
              placeholder="e.g. Global Tech Hub"
              maxLength={100}
              required
            />
          </div>
        </div>

        <div className="form-grid-2">
          <div className="field-group">
            <label htmlFor="origin">Origin Location</label>
            <input
              id="origin"
              className="text-input"
              value={form.origin}
              onChange={e => update('origin', e.target.value)}
              placeholder="e.g. Warsaw, PL"
              maxLength={120}
              required
            />
          </div>

          <div className="field-group">
            <label htmlFor="destination">Destination Location</label>
            <input
              id="destination"
              className="text-input"
              value={form.destination}
              onChange={e => update('destination', e.target.value)}
              placeholder="e.g. Berlin, DE"
              maxLength={120}
              required
            />
          </div>
        </div>

        <div className="form-actions">
          <button
            type="submit"
            className="action-btn btn-accent"
            disabled={loading}
          >
            {loading ? 'Registering...' : 'Register Shipment'}
          </button>
        </div>

        {error && (
          <div className="form-feedback-wrap">
            <FeedbackMessage tone="error" role="alert">{error}</FeedbackMessage>
          </div>
        )}

        {success && (
          <div className="success-receipt-card">
            <div className="receipt-header">
              <CheckCircleIcon size={18} className="text-success" />
              <span className="receipt-title">Shipment successfully created</span>
            </div>
            <div className="receipt-copy-bar">
              <span className="receipt-id-val">{success.trackingId}</span>
              <button
                type="button"
                className="action-btn btn-secondary btn-sm"
                onClick={copyTrackingId}
              >
                <CopyIcon size={14} />
                {copied ? 'Copied' : 'Copy ID'}
              </button>
            </div>
            <p className="receipt-hint">Use this ID to look up shipment route and real-time status.</p>
          </div>
        )}
      </form>
    </div>
  )
}
