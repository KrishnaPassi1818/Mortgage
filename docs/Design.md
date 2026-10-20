# ParcelFlow — UX and Frontend Design Specification

**Design goal:** A clean, trustworthy parcel-tracking dashboard that makes the status and journey immediately understandable. Keep the visual language restrained and implementation practical.

## 1. Product Experience

The app has three primary tasks:
1. Create a shipment.
2. Track a parcel.
3. Update parcel status as a demo operator.

The UI should explain the system through its behavior: a tracking ID identifies the parcel, the timeline records events, and the notification panel shows status-change records. It should not look like a complex enterprise logistics platform.

## 2. Information Architecture

Recommended single-page layout:
- **Top navigation:** ParcelFlow wordmark, “Track Parcel”, “Create Shipment”, “Operator Demo”.
- **Overview strip:** short product description and service status indicators.
- **Track Parcel section:** tracking ID search field and result panel.
- **Shipment summary:** tracking ID, origin, destination, recipient, created date, current status.
- **Tracking timeline:** chronological list of events.
- **Create Shipment form:** sender, recipient, origin, destination.
- **Operator panel:** tracking ID, new status, location, note, update action.
- **Notifications panel:** notification records for the current tracking ID.
- **Footer:** demo disclaimer and local-project note.

These can be tabs or sections in one page. Avoid adding React Router unless multiple distinct pages become necessary.

## 3. Visual Direction

- Style: modern, calm, practical, logistics-focused.
- Background: light neutral gray or off-white.
- Surfaces: white cards with subtle borders and restrained shadows.
- Primary color: deep blue/navy for actions and navigation.
- Status colors: neutral for `BOOKED`, blue for `IN_TRANSIT`, amber for `OUT_FOR_DELIVERY` and `DELAYED`, green for `DELIVERED`, red only for errors.
- Typography: system font stack; clear hierarchy; avoid decorative fonts.
- Radius: consistent medium rounded corners.
- Spacing: use a small spacing scale (4, 8, 12, 16, 24, 32 px).
- Motion: subtle hover/focus transitions only; no distracting animation.

Use CSS variables for colors, spacing, radii, and shadows. Do not introduce a large component library for the baseline.

## 4. Core Components

### `AppShell`
Owns the page frame, navigation, page title, and main content width.

### `ServiceStatus`
Displays the status of Shipment, Tracking, and Notification services using `/health`.
- States: `Checking`, `Online`, `Unavailable`.
- Do not imply that a service is online if the check has not completed.

### `TrackingSearch`
- Labelled tracking ID field.
- Submit button.
- Enter key submits.
- Shows validation and request errors.
- Does not clear the field on a failed lookup.

### `ShipmentSummary`
Shows tracking ID, origin → destination, sender/recipient as appropriate, creation date, and current status.

### `StatusBadge`
Maps backend status codes to human-readable labels and a consistent color:
- `BOOKED` → Booked
- `IN_TRANSIT` → In Transit
- `OUT_FOR_DELIVERY` → Out for Delivery
- `DELIVERED` → Delivered
- `DELAYED` → Delayed

The UI must not invent a status that is not returned by the API.

### `TrackingTimeline`
- Events ordered oldest to newest.
- Each item displays status, timestamp, optional location, and note.
- Clearly distinguishes the current/latest event.
- Shows an empty state if no events exist rather than fake entries.

### `CreateShipmentForm`
Fields:
- Sender name
- Recipient name
- Origin
- Destination

Requirements:
- Labels remain visible.
- Required fields are indicated.
- Disable the submit button while saving.
- On success, display the tracking ID prominently with a copy action.
- On error, preserve entered values and display the server message.

### `OperatorStatusForm`
Fields:
- Tracking ID
- Status select
- Location (optional)
- Note (optional)

Requirements:
- Explain that this is a demo operator panel.
- Display invalid transition errors clearly.
- On successful update, refresh tracking history and notifications.
- If notification recording fails, show a warning that the status changed but the notification could not be recorded.

### `NotificationList`
Displays title, message, status, and timestamp.
- Empty state: “No notifications recorded for this parcel yet.”
- Avoid claiming that a message was emailed or texted; these are in-app records only.

### `FeedbackMessage`
Shared component for success, warning, and error messages with accessible text.

## 5. Responsive Behavior

- Desktop: centered content with a two-column layout where useful.
- Tablet: reduce column gaps and stack forms when needed.
- Mobile: single-column layout, full-width primary buttons, wrapping tracking IDs, no horizontal scrolling.
- Navigation may wrap or collapse into simple section links.
- Timeline content must remain readable on narrow screens.

## 6. Loading, Empty, and Error States

Every network-driven panel must cover:
- **Loading:** skeleton or short progress text.
- **Empty:** explain what the user can do next.
- **Success:** show returned backend data.
- **Error:** explain the failure and provide a retry action where appropriate.
- **Partial success:** distinguish status update success from notification failure.

Example messages:
- “Searching for parcel…”
- “No parcel found for that tracking ID. Check the ID and try again.”
- “Shipment created. Save this tracking ID to look it up later.”
- “Status updated, but the notification service did not respond. The status change is saved.”
- “Tracking service is unavailable. Please try again.”

## 7. Accessibility

- Use semantic headings and form elements.
- Associate every input with a label.
- Provide visible keyboard focus.
- Do not rely on color alone to communicate status.
- Use `aria-live="polite"` for request feedback where useful.
- Maintain readable contrast and adequate click targets.
- Ensure dialogs, if introduced, can be opened and closed using the keyboard.

## 8. Frontend Structure

```text
frontend/
  src/
    api/
      http.js
      shipmentApi.js
      trackingApi.js
      notificationApi.js
    components/
      AppShell.jsx
      ServiceStatus.jsx
      StatusBadge.jsx
      TrackingTimeline.jsx
      ShipmentSummary.jsx
      FeedbackMessage.jsx
    features/
      tracking/
        TrackingSearch.jsx
      shipment/
        CreateShipmentForm.jsx
      operator/
        OperatorStatusForm.jsx
      notifications/
        NotificationList.jsx
    styles/
      tokens.css
      global.css
      components.css
    App.jsx
    main.jsx
  .env.example
  package.json
  vite.config.js
```

## 9. Frontend Environment Variables

Example `.env.example`:
```text
VITE_SHIPMENT_API_URL=http://localhost:8081
VITE_TRACKING_API_URL=http://localhost:8082
VITE_NOTIFICATION_API_URL=http://localhost:8083
```

These values are public frontend configuration. Never put database credentials or private keys here.

## 10. Design Acceptance Checklist

- The user can create and track a parcel without reading documentation.
- The tracking ID is easy to copy.
- The current status is visible without scrolling excessively.
- The timeline is chronological and readable.
- Every API-driven view has loading, empty, success, and error states.
- The operator panel is visibly labelled as a demo function.
- Notification failure is not misrepresented as success.
- The layout works at desktop and mobile widths.
