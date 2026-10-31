import React, { useState } from 'react'
import FeedbackMessage from '../../components/FeedbackMessage.jsx'
import { UserIcon, ShieldIcon } from '../../components/Icons.jsx'
import { signupUser, loginUser } from '../../api/userApi.js'

export default function AuthModal({ isOpen, onClose, onAuthSuccess }) {
  const [mode, setMode] = useState('signup') // 'signup' | 'login'
  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
    fullName: '',
    role: 'USER',
  })
  const [validationErrors, setValidationErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [successMsg, setSuccessMsg] = useState('')

  if (!isOpen) return null

  function update(field, val) {
    setForm(prev => ({ ...prev, [field]: val }))
    if (validationErrors[field]) {
      setValidationErrors(prev => ({ ...prev, [field]: null }))
    }
  }

  function validate() {
    const errs = {}
    if (!form.username.trim()) {
      errs.username = 'Username is required.'
    } else if (!/^[a-zA-Z0-9_]{3,30}$/.test(form.username.trim())) {
      errs.username = 'Username must be 3-30 letters, numbers, or underscores.'
    }

    if (mode === 'signup') {
      if (!form.email.trim()) {
        errs.email = 'Email address is required.'
      } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
        errs.email = 'Please enter a valid email address.'
      }

      if (!form.fullName.trim()) {
        errs.fullName = 'Full name is required.'
      } else if (form.fullName.trim().length > 100) {
        errs.fullName = 'Full name must be under 100 characters.'
      }
    }

    if (!form.password) {
      errs.password = 'Password is required.'
    } else if (form.password.length < 6) {
      errs.password = 'Password must be at least 6 characters.'
    }

    setValidationErrors(errs)
    return Object.keys(errs).length === 0
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setSuccessMsg('')

    if (!validate()) return

    setLoading(true)
    try {
      if (mode === 'signup') {
        const user = await signupUser({
          username: form.username.trim(),
          email: form.email.trim(),
          password: form.password,
          fullName: form.fullName.trim(),
          role: form.role,
        })
        setSuccessMsg(`Account created successfully for ${user.fullName || user.username}!`)
        setTimeout(() => {
          onAuthSuccess(user)
          onClose()
        }, 1000)
      } else {
        const user = await loginUser({
          username: form.username.trim(),
          password: form.password,
        })
        setSuccessMsg(`Welcome back, ${user.fullName || user.username}!`)
        setTimeout(() => {
          onAuthSuccess(user)
          onClose()
        }, 800)
      }
    } catch (err) {
      setError(err.message || 'Authentication failed. Please check inputs.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-backdrop-flow" onClick={onClose}>
      <div className="auth-dialog-card" onClick={e => e.stopPropagation()}>
        <div className="auth-header">
          <div className="auth-icon-badge">
            {form.role === 'ADMIN' ? <ShieldIcon size={24} /> : <UserIcon size={24} />}
          </div>
          <h2 className="auth-title">
            {mode === 'signup' ? 'Create ParcelFlow Account' : 'Sign In to ParcelFlow'}
          </h2>
          <p className="auth-subtitle">
            {mode === 'signup'
              ? 'Register with validation to request shipments and monitor dispatch status.'
              : 'Enter your credentials to access your portal.'}
          </p>
        </div>

        <div className="auth-tabs-toggle">
          <button
            type="button"
            className={`auth-toggle-btn ${mode === 'signup' ? 'active' : ''}`}
            onClick={() => { setMode('signup'); setError(''); setSuccessMsg(''); }}
          >
            Sign Up
          </button>
          <button
            type="button"
            className={`auth-toggle-btn ${mode === 'login' ? 'active' : ''}`}
            onClick={() => { setMode('login'); setError(''); setSuccessMsg(''); }}
          >
            Sign In
          </button>
        </div>

        {error && <FeedbackMessage tone="error" message={error} />}
        {successMsg && <FeedbackMessage tone="success" message={successMsg} />}

        <form onSubmit={handleSubmit} className="auth-form-layout" noValidate>
          {mode === 'signup' && (
            <>
              <div className="input-field-group">
                <label className="input-label-tag">Full Name *</label>
                <input
                  type="text"
                  className={`input-text-clean ${validationErrors.fullName ? 'has-error' : ''}`}
                  placeholder="e.g. John Doe"
                  value={form.fullName}
                  onChange={e => update('fullName', e.target.value)}
                />
                {validationErrors.fullName && (
                  <span className="field-err-text">{validationErrors.fullName}</span>
                )}
              </div>

              <div className="input-field-group">
                <label className="input-label-tag">Email Address *</label>
                <input
                  type="email"
                  className={`input-text-clean ${validationErrors.email ? 'has-error' : ''}`}
                  placeholder="e.g. john@example.com"
                  value={form.email}
                  onChange={e => update('email', e.target.value)}
                />
                {validationErrors.email && (
                  <span className="field-err-text">{validationErrors.email}</span>
                )}
              </div>
            </>
          )}

          <div className="input-field-group">
            <label className="input-label-tag">Username *</label>
            <input
              type="text"
              className={`input-text-clean ${validationErrors.username ? 'has-error' : ''}`}
              placeholder="e.g. user123 or admin"
              value={form.username}
              onChange={e => update('username', e.target.value)}
            />
            {validationErrors.username && (
              <span className="field-err-text">{validationErrors.username}</span>
            )}
          </div>

          <div className="input-field-group">
            <label className="input-label-tag">Password *</label>
            <input
              type="password"
              className={`input-text-clean ${validationErrors.password ? 'has-error' : ''}`}
              placeholder="Min. 6 characters"
              value={form.password}
              onChange={e => update('password', e.target.value)}
            />
            {validationErrors.password && (
              <span className="field-err-text">{validationErrors.password}</span>
            )}
          </div>

          {mode === 'signup' && (
            <div className="input-field-group">
              <label className="input-label-tag">Account Role</label>
              <select
                className="input-text-clean"
                value={form.role}
                onChange={e => update('role', e.target.value)}
              >
                <option value="USER">Customer / User</option>
                <option value="ADMIN">Administrator (Shipment Approval Privileges)</option>
              </select>
            </div>
          )}

          <div className="auth-actions-group">
            <button
              type="submit"
              className="primary-action-btn"
              style={{ width: '100%', justifyContent: 'center' }}
              disabled={loading}
            >
              {loading
                ? 'Processing...'
                : mode === 'signup'
                ? 'Complete Registration'
                : 'Sign In'}
            </button>
            <button
              type="button"
              className="secondary-action-btn"
              style={{ width: '100%', justifyContent: 'center', marginTop: '8px' }}
              onClick={onClose}
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
