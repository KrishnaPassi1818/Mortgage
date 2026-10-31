import React, { useState } from 'react'
import { BoxIcon, MapPinIcon, PlusIcon, SlidersIcon, BellIcon, ShieldIcon, FileTextIcon, UserIcon, LogOutIcon } from './Icons.jsx'
import ServiceStatus from './ServiceStatus.jsx'
import PrivacyPolicyModal from './PrivacyPolicyModal.jsx'
import TermsConditionsModal from './TermsConditionsModal.jsx'

export default function AppShell({ active, onNavigate, currentUser, onOpenAuth, onLogout, children }) {
  const [showPrivacy, setShowPrivacy] = useState(false)
  const [showTerms, setShowTerms] = useState(false)

  return (
    <div className="layout-shell">
      {/* Top Application Header */}
      <header className="site-header">
        <div className="header-inner">
          <div className="brand-group" onClick={() => onNavigate('track')}>
            <div className="brand-logo-icon">
              <BoxIcon size={20} className="brand-icon" />
            </div>
            <div className="brand-text">
              <span className="brand-name">ParcelFlow</span>
              <span className="brand-badge">Logistics OS</span>
            </div>
          </div>

          <nav className="header-navigation" aria-label="Main Navigation">
            <button
              className={`nav-tab-btn ${active === 'track' ? 'active' : ''}`}
              onClick={() => onNavigate('track')}
            >
              <MapPinIcon size={16} />
              <span>Live Tracking</span>
            </button>
            <button
              className={`nav-tab-btn ${active === 'user' ? 'active' : ''}`}
              onClick={() => onNavigate('user')}
            >
              <UserIcon size={16} />
              <span>User Section</span>
            </button>
            <button
              className={`nav-tab-btn ${active === 'admin' ? 'active' : ''}`}
              onClick={() => onNavigate('admin')}
            >
              <ShieldIcon size={16} />
              <span>Admin Portal</span>
            </button>
          </nav>

          <div className="header-status-slot" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <ServiceStatus />
            {currentUser ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span
                  style={{
                    fontSize: '0.85rem',
                    padding: '4px 8px',
                    borderRadius: '4px',
                    backgroundColor: 'rgba(255,255,255,0.06)',
                    color: 'var(--pf-text-primary)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                  }}
                >
                  <UserIcon size={14} />
                  <span>{currentUser.username}</span>
                  <strong style={{ fontSize: '0.7rem', opacity: 0.8 }}>({currentUser.role})</strong>
                </span>
                <button
                  type="button"
                  className="secondary-action-btn"
                  style={{ padding: '4px 8px', fontSize: '0.8rem' }}
                  title="Log Out"
                  onClick={onLogout}
                >
                  <LogOutIcon size={14} />
                </button>
              </div>
            ) : (
              <button
                type="button"
                className="primary-action-btn"
                style={{ padding: '6px 14px', fontSize: '0.85rem' }}
                onClick={onOpenAuth}
              >
                <UserIcon size={14} />
                <span>Sign In / Sign Up</span>
              </button>
            )}
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="main-content-flow">
        {children}
      </main>

      {/* Structured Footer */}
      <footer className="site-footer">
        <div className="footer-container">
          <div className="footer-left">
            <div className="footer-brand">
              <span className="footer-logo-text">ParcelFlow Operations</span>
              <span className="footer-domain-tag">parcelflow.io</span>
            </div>
            <p className="footer-copy">
              Enterprise parcel tracking and event distribution architecture powered by Core Java microservices.
            </p>
          </div>

          <div className="footer-nav-col">
            <span className="footer-heading">Compliance & Legal</span>
            <div className="footer-links-list">
              <button
                type="button"
                className="footer-link-action"
                onClick={() => setShowPrivacy(true)}
              >
                Privacy Policy
              </button>
              <button
                type="button"
                className="footer-link-action"
                onClick={() => setShowTerms(true)}
              >
                Terms and Conditions
              </button>
            </div>
          </div>

          <div className="footer-bottom-bar">
            <span>© 2026 ParcelFlow Technologies Inc. All rights reserved.</span>
            <span>Production Endpoint: api.parcelflow.io</span>
          </div>
        </div>
      </footer>

      {showPrivacy && <PrivacyPolicyModal onClose={() => setShowPrivacy(false)} />}
      {showTerms && <TermsConditionsModal onClose={() => setShowTerms(false)} />}
    </div>
  )
}
