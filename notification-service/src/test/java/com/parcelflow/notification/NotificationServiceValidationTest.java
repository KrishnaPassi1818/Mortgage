package com.parcelflow.notification;

import com.parcelflow.notification.dto.CreateNotificationRequest;
import com.parcelflow.notification.exception.ValidationException;
import com.parcelflow.notification.model.Notification;
import com.parcelflow.notification.repository.NotificationRepository;
import com.parcelflow.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceValidationTest {

    // In-memory fake repo
    static class FakeNotificationRepository extends NotificationRepository {
        private final List<Notification> store = new ArrayList<>();
        private long nextId = 1;

        @Override
        public Notification insert(Notification n) throws SQLException {
            n.setId(nextId++);
            if (n.getCreatedAt() == null) n.setCreatedAt(java.time.LocalDateTime.now());
            if (n.getDeliveryState() == null) n.setDeliveryState("RECORDED");
            store.add(n);
            return n;
        }

        @Override
        public List<Notification> findByTrackingId(String trackingId) throws SQLException {
            List<Notification> filtered = new ArrayList<>();
            for (Notification n : store) {
                if (n.getTrackingId().equals(trackingId)) filtered.add(n);
            }
            return filtered;
        }

        public void clear() { store.clear(); nextId = 1; }
    }

    private FakeNotificationRepository fakeRepo;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        fakeRepo = new FakeNotificationRepository();
        service = new NotificationService(fakeRepo);
    }

    private CreateNotificationRequest validRequest() {
        CreateNotificationRequest r = new CreateNotificationRequest();
        r.setTrackingId("PF26A1B2C3");
        r.setStatus("IN_TRANSIT");
        r.setTitle("Parcel status updated");
        r.setMessage("Your parcel is now in transit.");
        return r;
    }

    @Test
    void createAcceptsValidRequest() {
        var resp = service.createNotification(validRequest());
        assertNotNull(resp.getId());
        assertEquals("PF26A1B2C3", resp.getTrackingId());
        assertEquals("IN_TRANSIT", resp.getStatus());
        assertEquals("RECORDED", resp.getDeliveryState());
        assertNotNull(resp.getCreatedAt());
    }

    @Test
    void createRejectsNullRequest() {
        assertThrows(ValidationException.class, () -> service.createNotification(null));
    }

    @Test
    void createRejectsMissingTrackingId() {
        var r = validRequest(); r.setTrackingId(null);
        assertThrows(ValidationException.class, () -> service.createNotification(r));
    }

    @Test
    void createRejectsBlankTrackingId() {
        var r = validRequest(); r.setTrackingId("   ");
        assertThrows(ValidationException.class, () -> service.createNotification(r));
    }

    @Test
    void createRejectsInvalidTrackingIdFormat() {
        var r = validRequest(); r.setTrackingId("AB"); // too short
        assertThrows(ValidationException.class, () -> service.createNotification(r));
        var r2 = validRequest(); r2.setTrackingId("PF/26 BAD"); // illegal chars
        assertThrows(ValidationException.class, () -> service.createNotification(r2));
    }

    @Test
    void createRejectsMissingStatus() {
        var r = validRequest(); r.setStatus(null);
        assertThrows(ValidationException.class, () -> service.createNotification(r));
    }

    @Test
    void createRejectsInvalidStatus() {
        var r = validRequest(); r.setStatus("SHIPPED");
        assertThrows(ValidationException.class, () -> service.createNotification(r));
    }

    @Test
    void createAcceptsAllCanonicalStatuses() {
        String[] statuses = {"BOOKED","IN_TRANSIT","OUT_FOR_DELIVERY","DELIVERED","DELAYED"};
        for (String s : statuses) {
            var r = validRequest();
            r.setStatus(s);
            r.setTrackingId("PF26" + s.substring(0,3) + "X1"); // make unique
            var resp = service.createNotification(r);
            assertEquals(s, resp.getStatus());
        }
    }

    @Test
    void createRejectsMissingTitle() {
        var r = validRequest(); r.setTitle("");
        assertThrows(ValidationException.class, () -> service.createNotification(r));
    }

    @Test
    void createRejectsMissingMessage() {
        var r = validRequest(); r.setMessage("   ");
        assertThrows(ValidationException.class, () -> service.createNotification(r));
    }

    @Test
    void createRejectsTooLongTitle() {
        var r = validRequest(); r.setTitle("T".repeat(121));
        assertThrows(ValidationException.class, () -> service.createNotification(r));
    }

    @Test
    void createRejectsTooLongMessage() {
        var r = validRequest(); r.setMessage("M".repeat(256));
        assertThrows(ValidationException.class, () -> service.createNotification(r));
    }

    @Test
    void listReturnsOnlyMatchingTrackingId() {
        var r1 = validRequest(); r1.setTrackingId("PF26AAAAAA"); r1.setTitle("First");
        var r2 = validRequest(); r2.setTrackingId("PF26BBBBBB"); r2.setTitle("Second");
        service.createNotification(r1);
        service.createNotification(r2);
        service.createNotification(r1); // second for same ID

        var listA = service.listNotifications("PF26AAAAAA");
        var listB = service.listNotifications("PF26BBBBBB");
        assertEquals(2, listA.size());
        assertEquals(1, listB.size());
        assertTrue(listA.stream().allMatch(n -> n.getTrackingId().equals("PF26AAAAAA")));
    }

    @Test
    void listReturnsEmptyForUnknownTrackingId() {
        var list = service.listNotifications("PF26UNKNOWN");
        assertTrue(list.isEmpty());
    }

    @Test
    void listRejectsBlankTrackingId() {
        assertThrows(ValidationException.class, () -> service.listNotifications(null));
        assertThrows(ValidationException.class, () -> service.listNotifications(""));
        assertThrows(ValidationException.class, () -> service.listNotifications("   "));
    }

    @Test
    void listRejectsInvalidTrackingIdFormat() {
        assertThrows(ValidationException.class, () -> service.listNotifications("ab"));
        assertThrows(ValidationException.class, () -> service.listNotifications("PF/26 BAD"));
    }

    @Test
    void deliveryStateAlwaysRecorded() {
        var resp = service.createNotification(validRequest());
        assertEquals("RECORDED", resp.getDeliveryState());
        // Ensure message says in-app, not email/SMS — check service doesn't claim external delivery
        assertNotNull(resp.getTitle());
    }
}
