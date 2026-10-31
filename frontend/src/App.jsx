import React, { useState, useEffect } from 'react'
import AppShell from './components/AppShell.jsx'
import ShipmentSummary from './components/ShipmentSummary.jsx'
import TrackingTimeline from './components/TrackingTimeline.jsx'
import InteractiveMap from './components/InteractiveMap.jsx'
import StatusBadge from './components/StatusBadge.jsx'
import TrackingSearch from './features/tracking/TrackingSearch.jsx'
import NotificationList from './features/notifications/NotificationList.jsx'
import AuthModal from './features/auth/AuthModal.jsx'
import UserProfile from './features/user/UserProfile.jsx'
import AdminPortal from './features/admin/AdminPortal.jsx'
import { getShipment } from './api/shipmentApi.js'
import { getTracking } from './api/trackingApi.js'
import { getUserShipmentRequests } from './api/userApi.js'

export default function App() {
  const [activeTab, setActiveTab] = useState('track')
  const [trackingId, setTrackingId] = useState('')
  const [shipment, setShipment] = useState(null)
  const [tracking, setTracking] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [recentShipments, setRecentShipments] = useState([])
  const [currentUser, setCurrentUser] = useState(null)
  const [userShipments, setUserShipments] = useState([])
  const [loadingUserShipments, setLoadingUserShipments] = useState(false)
  const [isAuthOpen, setIsAuthOpen] = useState(false)

  // On mount: restore tracking ID, recent list, and user session
  useEffect(() => {
    const savedId = localStorage.getItem('parcelflow:lastTrackingId')
    const savedList = localStorage.getItem('parcelflow:recentShipments')
    const savedUser = localStorage.getItem('parcelflow:currentUser')
    if (savedId) {
      setTrackingId(savedId)
      executeSearch(savedId)
    }
    if (savedList) {
      try {
        setRecentShipments(JSON.parse(savedList))
      } catch {
        // ignore parse error
      }
    }
    if (savedUser) {
      try {
        const parsed = JSON.parse(savedUser)
        setCurrentUser(parsed)
      } catch {
        // ignore parse error
      }
    }
  }, [])

  // When currentUser changes or when activeTab is 'track', fetch user's parcels if logged in
  useEffect(() => {
    if (currentUser?.id) {
      loadCurrentUserShipments(currentUser.id)
    } else {
      setUserShipments([])
    }
  }, [currentUser?.id, activeTab])

  async function loadCurrentUserShipments(userId) {
    setLoadingUserShipments(true)
    try {
      const requests = await getUserShipmentRequests(userId)
      // Extract parcels with assigned trackingId
      const approvedWithTracking = (requests || []).filter(r => r.trackingId)
      // Fetch latest tracking statuses for these parcels in parallel
      const detailed = await Promise.all(
        approvedWithTracking.map(async req => {
          try {
            const tr = await getTracking(req.trackingId)
            return {
              trackingId: req.trackingId,
              origin: req.origin,
              destination: req.destination,
              status: tr?.currentStatus || req.status || 'BOOKED',
              timestamp: req.updatedAt || req.createdAt
            }
          } catch {
            return {
              trackingId: req.trackingId,
              origin: req.origin,
              destination: req.destination,
              status: req.status === 'APPROVED' ? 'IN_TRANSIT' : req.status,
              timestamp: req.updatedAt || req.createdAt
            }
          }
        })
      )
      setUserShipments(detailed)
      // If no current trackingId is selected and user has shipments, auto select the first one
      if (!trackingId && detailed.length > 0) {
        executeSearch(detailed[0].trackingId)
      }
    } catch (err) {
      console.warn('Failed to fetch user shipments for live dashboard:', err)
    } finally {
      setLoadingUserShipments(false)
    }
  }

  function handleAuthSuccess(user) {
    setCurrentUser(user)
    localStorage.setItem('parcelflow:currentUser', JSON.stringify(user))
    if (user.role === 'ADMIN') {
      setActiveTab('admin')
    } else {
      setActiveTab('user')
    }
  }

  function handleLogout() {
    setCurrentUser(null)
    localStorage.removeItem('parcelflow:currentUser')
    setUserShipments([])
    setActiveTab('track')
  }

  function recordRecent(item) {
    setRecentShipments(prev => {
      const filtered = prev.filter(p => p.trackingId !== item.trackingId)
      const updated = [item, ...filtered].slice(0, 8)
      localStorage.setItem('parcelflow:recentShipments', JSON.stringify(updated))
      return updated
    })
  }

  async function executeSearch(targetId) {
    const tid = (targetId || trackingId).trim()
    if (!tid) {
      setError('Please provide a tracking ID.')
      return
    }

    localStorage.setItem('parcelflow:lastTrackingId', tid)
    setTrackingId(tid)
    setLoading(true)
    setError('')
    setShipment(null)
    setTracking(null)

    const shipmentP = getShipment(tid).catch(e => { throw { source: 'shipment', err: e } })
    const trackingP = getTracking(tid).catch(e => { throw { source: 'tracking', err: e } })

    try {
      const results = await Promise.allSettled([shipmentP, trackingP])
      const shipmentRes = results[0]
      const trackingRes = results[1]

      let currentShipment = null
      let currentTracking = null

      if (shipmentRes.status === 'fulfilled') {
        currentShipment = shipmentRes.value
        setShipment(currentShipment)
      } else {
        const e = shipmentRes.reason?.err || shipmentRes.reason
        if (e.status === 404) {
          setError(`No shipment found matching ID "${tid}".`)
          setLoading(false)
          return
        }
        setError(`Shipment lookup error: ${e.message}`)
      }

      if (trackingRes.status === 'fulfilled') {
        currentTracking = trackingRes.value
        setTracking(currentTracking)
      }

      if (currentShipment || currentTracking) {
        recordRecent({
          trackingId: tid,
          origin: currentShipment?.origin || 'Unknown',
          destination: currentShipment?.destination || 'Unknown',
          status: currentTracking?.currentStatus || 'BOOKED',
          timestamp: new Date().toISOString()
        })
      }
    } catch (err) {
      setError(err.message || 'Error occurred while querying shipment telemetry.')
    } finally {
      setLoading(false)
    }
  }

  function handleCreated(newShipment) {
    if (newShipment?.trackingId) {
      setTrackingId(newShipment.trackingId)
      recordRecent({
        trackingId: newShipment.trackingId,
        origin: newShipment.origin,
        destination: newShipment.destination,
        status: 'BOOKED',
        timestamp: new Date().toISOString()
      })
      setActiveTab('track')
      executeSearch(newShipment.trackingId)
    }
  }

  function handleUpdated(res) {
    if (res?.trackingId) {
      executeSearch(res.trackingId)
    }
    if (currentUser?.id) {
      loadCurrentUserShipments(currentUser.id)
    }
  }

  function handleTrackShipment(tid) {
    setActiveTab('track')
    executeSearch(tid)
  }

  const displayedShipments = currentUser ? userShipments : recentShipments
  const shelfTitle = currentUser ? `${currentUser.username}'s Shipments` : 'Shipment Records'

  return (
    <AppShell
      active={activeTab}
      onNavigate={setActiveTab}
      currentUser={currentUser}
      onOpenAuth={() => setIsAuthOpen(true)}
      onLogout={handleLogout}
    >
      <AuthModal
        isOpen={isAuthOpen}
        onClose={() => setIsAuthOpen(false)}
        onAuthSuccess={handleAuthSuccess}
      />

      {activeTab === 'track' && (
        <div className="dribbble-dashboard-grid">
          {/* Left Column: Search & Shipment List Drawer */}
          <div className="dashboard-sidebar-column">
            <TrackingSearch
              value={trackingId}
              onValueChange={setTrackingId}
              onSearch={executeSearch}
              loading={loading}
              error={error}
            />

            {/* User Shipments / Recent Orders List (Ronas IT Left-Drawer style) */}
            <div className="shipments-shelf-card">
              <div className="shelf-header">
                <span className="shelf-title">{shelfTitle}</span>
                <span className="shelf-count">{loadingUserShipments ? '...' : displayedShipments.length}</span>
              </div>
              <div className="shelf-items-list">
                {currentUser && displayedShipments.length === 0 && !loadingUserShipments && (
                  <div className="timeline-empty">
                    <span>No active shipments found for {currentUser.username}. Create a request in the User Section.</span>
                  </div>
                )}
                {!currentUser && displayedShipments.length === 0 && (
                  <div className="timeline-empty">
                    <span>No recent shipments queried. Search an ID or sign in to view your account shipments.</span>
                  </div>
                )}
                {displayedShipments.map(item => (
                  <button
                    key={item.trackingId}
                    type="button"
                    className={`shelf-item-row ${trackingId === item.trackingId ? 'is-active' : ''}`}
                    onClick={() => executeSearch(item.trackingId)}
                  >
                    <div className="item-main-info">
                      <span className="item-route-title">{item.origin} → {item.destination}</span>
                      <span className="item-order-id">{item.trackingId}</span>
                    </div>
                    <StatusBadge status={item.status} />
                  </button>
                ))}
              </div>
            </div>

            {/* In-App Dispatch Logs */}
            <NotificationList trackingId={trackingId} />
          </div>

          {/* Right Column: Precision Dark Map & Checkpoint Timeline */}
          <div className="dashboard-main-column">
            <InteractiveMap shipment={shipment} tracking={tracking} />
            <ShipmentSummary shipment={shipment} tracking={tracking} />
            <TrackingTimeline events={tracking?.events} />
          </div>
        </div>
      )}

      {activeTab === 'user' && (
        <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
          {currentUser ? (
            <UserProfile user={currentUser} onTrackShipment={handleTrackShipment} />
          ) : (
            <div className="panel-card" style={{ textAlign: 'center', padding: '48px 24px' }}>
              <h2 className="panel-title" style={{ marginBottom: '12px' }}>Welcome to ParcelFlow User Section</h2>
              <p style={{ color: 'var(--pf-text-secondary)', marginBottom: '24px', maxWidth: '480px', margin: '0 auto 24px' }}>
                Please sign in or create an account with valid credentials to request new shipments and view real-time admin updates on your profile.
              </p>
              <button
                type="button"
                className="primary-action-btn"
                style={{ margin: '0 auto' }}
                onClick={() => setIsAuthOpen(true)}
              >
                Sign In / Sign Up
              </button>
            </div>
          )}
        </div>
      )}

      {activeTab === 'admin' && (
        <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
          {currentUser && currentUser.role === 'ADMIN' ? (
            <AdminPortal
              currentUser={currentUser}
              onTrackShipment={handleTrackShipment}
              onShipmentUpdated={handleUpdated}
            />
          ) : (
            <div className="panel-card" style={{ textAlign: 'center', padding: '48px 24px' }}>
              <h2 className="panel-title" style={{ marginBottom: '12px', color: '#f59e0b' }}>Admin Privilege Required</h2>
              <p style={{ color: 'var(--pf-text-secondary)', marginBottom: '24px', maxWidth: '480px', margin: '0 auto 24px' }}>
                {currentUser
                  ? `You are currently signed in as "${currentUser.username}" with role "${currentUser.role}". Only administrators have privileges to approve user shipments and update records.`
                  : 'Please sign in or register with an Administrator account to access the approval console.'}
              </p>
              <button
                type="button"
                className="primary-action-btn"
                style={{ margin: '0 auto' }}
                onClick={() => setIsAuthOpen(true)}
              >
                {currentUser ? 'Switch Account / Sign In as Admin' : 'Sign In as Administrator'}
              </button>
            </div>
          )}
        </div>
      )}
    </AppShell>
  )
}

