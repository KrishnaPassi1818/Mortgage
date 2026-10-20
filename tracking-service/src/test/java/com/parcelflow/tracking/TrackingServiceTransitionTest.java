package com.parcelflow.tracking;

import com.parcelflow.tracking.client.NotificationClient;
import com.parcelflow.tracking.client.ShipmentClient;
import com.parcelflow.tracking.dto.UpdateStatusRequest;
import com.parcelflow.tracking.exception.ConflictException;
import com.parcelflow.tracking.exception.NotFoundException;
import com.parcelflow.tracking.exception.ValidationException;
import com.parcelflow.tracking.model.TrackingEvent;
import com.parcelflow.tracking.repository.TrackingRepository;
import com.parcelflow.tracking.service.TrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class TrackingServiceTransitionTest {

    // In-memory fake repository that mimics DB transaction behavior
    static class FakeTrackingRepository extends TrackingRepository {
        private final Map<String, String> statusMap = new ConcurrentHashMap<>();
        private final Map<String, List<TrackingEvent>> eventsMap = new ConcurrentHashMap<>();
        private long nextId = 1;

        @Override
        public Optional<String> getCurrentStatus(String trackingId) {
            return Optional.ofNullable(statusMap.get(trackingId));
        }

        @Override
        public List<TrackingEvent> findEvents(String trackingId) {
            return new ArrayList<>(eventsMap.getOrDefault(trackingId, List.of()));
        }

        @Override
        public void updateStatusAtomically(String trackingId, String newStatus, String location, String note) {
            // Simulate atomic transaction: update both together
            statusMap.put(trackingId, newStatus);
            TrackingEvent e = new TrackingEvent();
            e.setId(nextId++);
            e.setTrackingId(trackingId);
            e.setStatus(newStatus);
            e.setLocation(location);
            e.setNote(note);
            e.setEventTime(LocalDateTime.now());
            eventsMap.computeIfAbsent(trackingId, k -> new ArrayList<>()).add(e);
        }

        void clear() {
            statusMap.clear();
            eventsMap.clear();
            nextId = 1;
        }
    }

    // Fake clients
    static class FakeShipmentClient extends ShipmentClient {
        private final Set<String> knownShipments = new HashSet<>();
        boolean shouldTimeout = false;

        FakeShipmentClient(String... ids) {
            super("http://localhost:9999");
            knownShipments.addAll(Arrays.asList(ids));
        }

        @Override
        public void verifyExists(String trackingId) {
            if (shouldTimeout) throw new com.parcelflow.tracking.exception.DownstreamException("DOWNSTREAM_UNAVAILABLE", "Shipment service timeout", 503);
            if (!knownShipments.contains(trackingId)) {
                throw new NotFoundException("SHIPMENT_NOT_FOUND", "No shipment was found");
            }
        }

        void addShipment(String id) { knownShipments.add(id); }
    }

    static class FakeNotificationClient extends NotificationClient {
        boolean shouldFail = false;
        int callCount = 0;
        String lastTrackingId, lastStatus;

        FakeNotificationClient() { super("http://localhost:9999"); }

        @Override
        public boolean recordNotification(String trackingId, String status, String location) {
            callCount++;
            lastTrackingId = trackingId;
            lastStatus = status;
            return !shouldFail;
        }
    }

    private FakeTrackingRepository fakeRepo;
    private FakeShipmentClient fakeShipment;
    private FakeNotificationClient fakeNotification;
    private TrackingService service;

    @BeforeEach
    void setUp() {
        fakeRepo = new FakeTrackingRepository();
        fakeShipment = new FakeShipmentClient("PF26A1B2C3", "PF26TEST01");
        fakeNotification = new FakeNotificationClient();
        service = new TrackingService(fakeRepo, fakeShipment, fakeNotification);
    }

    private UpdateStatusRequest req(String status, String location, String note) {
        UpdateStatusRequest r = new UpdateStatusRequest();
        r.setStatus(status);
        r.setLocation(location);
        r.setNote(note);
        return r;
    }

    @Test
    void initialBookedAllowedWhenNoStatus() {
        var resp = service.updateStatus("PF26A1B2C3", req("BOOKED", "Amritsar", "Shipment registered"));
        assertEquals("BOOKED", resp.getCurrentStatus());
        assertEquals(1, resp.getEvents().size());
        assertEquals("BOOKED", resp.getEvents().get(0).getStatus());
        assertTrue(resp.getNotificationRecorded());
        assertEquals(1, fakeNotification.callCount);
    }

    @Test
    void initialNonBookedRejected() {
        assertThrows(ConflictException.class, () -> service.updateStatus("PF26A1B2C3", req("IN_TRANSIT", null, null)));
    }

    @Test
    void validTransitionsBookedToInTransit() {
        service.updateStatus("PF26A1B2C3", req("BOOKED", null, null));
        var resp = service.updateStatus("PF26A1B2C3", req("IN_TRANSIT", "Ludhiana", "Departed"));
        assertEquals("IN_TRANSIT", resp.getCurrentStatus());
        assertEquals(2, resp.getEvents().size());
    }

    @Test
    void validTransitionsBookedToDelayed() {
        service.updateStatus("PF26A1B2C3", req("BOOKED", null, null));
        var resp = service.updateStatus("PF26A1B2C3", req("DELAYED", null, "Weather delay"));
        assertEquals("DELAYED", resp.getCurrentStatus());
    }

    @Test
    void validInTransitToOutForDelivery() {
        service.updateStatus("PF26A1B2C3", req("BOOKED", null, null));
        service.updateStatus("PF26A1B2C3", req("IN_TRANSIT", null, null));
        var resp = service.updateStatus("PF26A1B2C3", req("OUT_FOR_DELIVERY", null, null));
        assertEquals("OUT_FOR_DELIVERY", resp.getCurrentStatus());
    }

    @Test
    void validOutForDeliveryToDeliveredTerminal() {
        service.updateStatus("PF26A1B2C3", req("BOOKED", null, null));
        service.updateStatus("PF26A1B2C3", req("IN_TRANSIT", null, null));
        service.updateStatus("PF26A1B2C3", req("OUT_FOR_DELIVERY", null, null));
        var resp = service.updateStatus("PF26A1B2C3", req("DELIVERED", null, null));
        assertEquals("DELIVERED", resp.getCurrentStatus());
        // Further transition after DELIVERED should fail
        assertThrows(ConflictException.class, () -> service.updateStatus("PF26A1B2C3", req("IN_TRANSIT", null, null)));
    }

    @Test
    void delayedToInTransitAllowed() {
        service.updateStatus("PF26A1B2C3", req("BOOKED", null, null));
        service.updateStatus("PF26A1B2C3", req("DELAYED", null, null));
        var resp = service.updateStatus("PF26A1B2C3", req("IN_TRANSIT", null, null));
        assertEquals("IN_TRANSIT", resp.getCurrentStatus());
    }

    @Test
    void invalidBookedToOutForDeliveryRejected() {
        service.updateStatus("PF26A1B2C3", req("BOOKED", null, null));
        assertThrows(ConflictException.class, () -> service.updateStatus("PF26A1B2C3", req("OUT_FOR_DELIVERY", null, null)));
    }

    @Test
    void invalidBookedToDeliveredRejected() {
        service.updateStatus("PF26A1B2C3", req("BOOKED", null, null));
        assertThrows(ConflictException.class, () -> service.updateStatus("PF26A1B2C3", req("DELIVERED", null, null)));
    }

    @Test
    void duplicateBookedRejected() {
        service.updateStatus("PF26A1B2C3", req("BOOKED", null, null));
        assertThrows(ConflictException.class, () -> service.updateStatus("PF26A1B2C3", req("BOOKED", null, null)));
    }

    @Test
    void unknownShipmentReturns404() {
        assertThrows(NotFoundException.class, () -> service.updateStatus("PF99NOTFOUND", req("BOOKED", null, null)));
        assertThrows(NotFoundException.class, () -> service.getTracking("PF99NOTFOUND"));
    }

    @Test
    void invalidStatusReturns400() {
        assertThrows(ValidationException.class, () -> service.updateStatus("PF26A1B2C3", req("SHIPPED", null, null)));
        assertThrows(ValidationException.class, () -> service.updateStatus("PF26A1B2C3", req("", null, null)));
    }

    @Test
    void notificationFailureDoesNotRollbackStatus() {
        fakeNotification.shouldFail = true;
        var resp = service.updateStatus("PF26A1B2C3", req("BOOKED", "Amritsar", "Shipment registered"));
        assertEquals("BOOKED", resp.getCurrentStatus());
        assertFalse(resp.getNotificationRecorded(), "Should report partial success");
        assertEquals(1, resp.getEvents().size());
        // Verify DB still has committed status despite notification failure
        assertEquals("BOOKED", fakeRepo.getCurrentStatus("PF26A1B2C3").orElse(null));
        assertEquals(1, fakeRepo.findEvents("PF26A1B2C3").size());
    }

    @Test
    void getTrackingReturnsHistoryOldestToNewest() {
        service.updateStatus("PF26A1B2C3", req("BOOKED", "Amritsar", "Shipment registered"));
        service.updateStatus("PF26A1B2C3", req("IN_TRANSIT", "Ludhiana", "Departed"));
        service.updateStatus("PF26A1B2C3", req("OUT_FOR_DELIVERY", null, null));

        var resp = service.getTracking("PF26A1B2C3");
        assertEquals("OUT_FOR_DELIVERY", resp.getCurrentStatus());
        assertEquals(3, resp.getEvents().size());
        assertEquals("BOOKED", resp.getEvents().get(0).getStatus());
        assertEquals("IN_TRANSIT", resp.getEvents().get(1).getStatus());
        assertEquals("OUT_FOR_DELIVERY", resp.getEvents().get(2).getStatus());
    }

    @Test
    void getTrackingForShipmentWithNoTrackingReturns404() {
        // Shipment exists but no tracking events yet
        // Our fake has shipment but no status -> getTracking should 404
        assertThrows(NotFoundException.class, () -> service.getTracking("PF26TEST01"));
    }

    @Test
    void locationAndNoteTruncatedToNullIfBlank() {
        var r = req("BOOKED", "   ", "   ");
        service.updateStatus("PF26A1B2C3", r);
        var events = fakeRepo.findEvents("PF26A1B2C3");
        assertNull(events.get(0).getLocation());
        assertNull(events.get(0).getNote());
    }

    @Test
    void downstreamTimeoutReturns503() {
        fakeShipment.shouldTimeout = true;
        assertThrows(com.parcelflow.tracking.exception.DownstreamException.class, () -> service.updateStatus("PF26A1B2C3", req("BOOKED", null, null)));
    }
}
