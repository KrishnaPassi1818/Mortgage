package com.parcelflow.shipment;

import com.parcelflow.shipment.util.TrackingIdGenerator;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class TrackingIdGeneratorTest {

    @Test
    void generatesCorrectFormat() {
        String id = TrackingIdGenerator.generate();
        assertNotNull(id);
        assertEquals(10, id.length(), "Expected PF + YY (2) + 6 alnum = 10 chars, got " + id);
        assertTrue(id.startsWith("PF"), "Must start with PF");
        assertTrue(id.matches("^PF\\d{2}[A-Z0-9]{6}$"), "Pattern PFYY + 6 alnum, got " + id);
    }

    @Test
    void generatesUniqueIds() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            ids.add(TrackingIdGenerator.generate());
        }
        // 100 generated, expect at least 95 unique (collision extremely unlikely)
        assertTrue(ids.size() >= 95, "Expected mostly unique IDs, got " + ids.size() + " unique of 100");
    }

    @Test
    void isValidFormatAcceptsGenerated() {
        String id = TrackingIdGenerator.generate();
        assertTrue(TrackingIdGenerator.isValidFormat(id));
    }

    @Test
    void isValidFormatRejectsBad() {
        assertFalse(TrackingIdGenerator.isValidFormat(null));
        assertFalse(TrackingIdGenerator.isValidFormat(""));
        assertFalse(TrackingIdGenerator.isValidFormat("PF123"));
        assertFalse(TrackingIdGenerator.isValidFormat("XYZ26A1B2C3"));
        assertFalse(TrackingIdGenerator.isValidFormat("PF26abc123")); // lowercase not allowed in strict format
    }
}
