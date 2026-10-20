package com.parcelflow.shipment;

import com.parcelflow.shipment.client.TrackingClient;
import com.parcelflow.shipment.dto.CreateShipmentRequest;
import com.parcelflow.shipment.exception.ValidationException;
import com.parcelflow.shipment.repository.ShipmentRepository;
import com.parcelflow.shipment.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShipmentServiceValidationTest {

    private ShipmentService service;

    @BeforeEach
    void setUp() {
        // Use fake repo to avoid DB
        service = new ShipmentService(new FakeShipmentRepository(), new TrackingClient("http://localhost:9999"));
    }

    // Fake repo that never hits DB — returns empty for lookup, throws unsupported for insert
    static class FakeShipmentRepository extends ShipmentRepository {
        @Override
        public java.util.Optional<com.parcelflow.shipment.model.Shipment> findByTrackingId(String trackingId) {
            return java.util.Optional.empty();
        }
        @Override
        public void insert(com.parcelflow.shipment.model.Shipment s) {
            throw new UnsupportedOperationException("insert not used in validation tests");
        }
    }

    private CreateShipmentRequest validRequest() {
        CreateShipmentRequest r = new CreateShipmentRequest();
        r.setSenderName("Krishna");
        r.setRecipientName("Aarav");
        r.setOrigin("Amritsar");
        r.setDestination("Delhi");
        return r;
    }

    @Test
    void validateAcceptsValidRequest() {
        assertDoesNotThrow(() -> service.validate(validRequest()));
    }

    @Test
    void validateRejectsNullRequest() {
        ValidationException ex = assertThrows(ValidationException.class, () -> service.validate(null));
        assertEquals("VALIDATION_ERROR", ex.getCode());
    }

    @Test
    void validateRejectsMissingSenderName() {
        CreateShipmentRequest r = validRequest();
        r.setSenderName(null);
        assertThrows(ValidationException.class, () -> service.validate(r));
    }

    @Test
    void validateRejectsBlankSenderName() {
        CreateShipmentRequest r = validRequest();
        r.setSenderName("   ");
        assertThrows(ValidationException.class, () -> service.validate(r));
    }

    @Test
    void validateRejectsMissingRecipientName() {
        CreateShipmentRequest r = validRequest();
        r.setRecipientName("");
        assertThrows(ValidationException.class, () -> service.validate(r));
    }

    @Test
    void validateRejectsMissingOrigin() {
        CreateShipmentRequest r = validRequest();
        r.setOrigin(null);
        assertThrows(ValidationException.class, () -> service.validate(r));
    }

    @Test
    void validateRejectsMissingDestination() {
        CreateShipmentRequest r = validRequest();
        r.setDestination("   ");
        assertThrows(ValidationException.class, () -> service.validate(r));
    }

    @Test
    void validateRejectsTooLongSenderName() {
        CreateShipmentRequest r = validRequest();
        r.setSenderName("A".repeat(101));
        ValidationException ex = assertThrows(ValidationException.class, () -> service.validate(r));
        assertTrue(ex.getMessage().contains("100"));
    }

    @Test
    void validateRejectsTooLongOrigin() {
        CreateShipmentRequest r = validRequest();
        r.setOrigin("B".repeat(121));
        assertThrows(ValidationException.class, () -> service.validate(r));
    }

    @Test
    void getShipmentRejectsBlankId() {
        assertThrows(ValidationException.class, () -> service.getShipment(null));
        assertThrows(ValidationException.class, () -> service.getShipment(""));
        assertThrows(ValidationException.class, () -> service.getShipment("   "));
    }

    @Test
    void getShipmentRejectsMalformedId() {
        // Contains slash or spaces -> 400
        assertThrows(ValidationException.class, () -> service.getShipment("PF/26 BAD"));
        assertThrows(ValidationException.class, () -> service.getShipment("a".repeat(33))); // >32
        assertThrows(ValidationException.class, () -> service.getShipment("ab")); // <6
    }

    @Test
    void getShipmentAcceptsValidFormatEvenIfNotExistsHandlingLater() {
        // With fake repo returning empty, should throw NotFound, not ValidationException
        try {
            service.getShipment("PF26A1B2C3");
            fail("Expected NotFoundException");
        } catch (ValidationException ve) {
            fail("Should not throw ValidationException for valid ID: " + ve.getMessage());
        } catch (com.parcelflow.shipment.exception.NotFoundException ne) {
            assertEquals("SHIPMENT_NOT_FOUND", ne.getCode());
        }
    }
}
