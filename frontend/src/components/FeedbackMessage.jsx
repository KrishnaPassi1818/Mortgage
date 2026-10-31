import React from 'react'
import { CheckCircleIcon, AlertCircleIcon } from './Icons.jsx'

export default function FeedbackMessage({ tone = 'info', children, role = 'status' }) {
  const Icon = tone === 'success' ? CheckCircleIcon : AlertCircleIcon

  return (
    <div className={`feedback-banner feedback-${tone}`} role={role} aria-live="polite">
      <Icon size={16} className="feedback-icon" />
      <div className="feedback-body">{children}</div>
    </div>
  )
}
