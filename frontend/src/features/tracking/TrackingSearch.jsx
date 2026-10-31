import React, { useState } from 'react'
import { SearchIcon } from '../../components/Icons.jsx'
import FeedbackMessage from '../../components/FeedbackMessage.jsx'

export default function TrackingSearch({ value, onValueChange, onSearch, loading, error }) {
  const [touched, setTouched] = useState(false)

  function submit(e) {
    e.preventDefault()
    setTouched(true)
    if (!value.trim()) return
    onSearch(value.trim())
  }

  return (
    <div className="search-section">
      <form onSubmit={submit} className="tracking-search-form" noValidate>
        <div className="search-input-shell">
          <SearchIcon size={18} className="search-shell-icon" />
          <input
            id="trackingId"
            type="text"
            className="search-field"
            placeholder="Enter tracking ID (e.g. PF26A1B2C3)"
            value={value}
            onChange={e => onValueChange(e.target.value)}
            onBlur={() => setTouched(true)}
            aria-invalid={touched && !value.trim() ? 'true' : 'false'}
            autoComplete="off"
            spellCheck="false"
          />
          <button
            type="submit"
            className="action-btn btn-accent"
            disabled={loading || !value.trim()}
          >
            {loading ? 'Locating...' : 'Track'}
          </button>
        </div>
      </form>

      {touched && !value.trim() && (
        <div className="input-validation-msg">Tracking ID is required.</div>
      )}

      {error && (
        <div className="search-feedback-wrap">
          <FeedbackMessage tone="error" role="alert">{error}</FeedbackMessage>
        </div>
      )}
    </div>
  )
}
