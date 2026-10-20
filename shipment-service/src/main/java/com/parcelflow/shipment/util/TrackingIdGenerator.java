package com.parcelflow.shipment.util;

import java.security.SecureRandom;
import java.time.LocalDate;

/**
 * Generates tracking IDs like PF26A1B2C3
 * Format: PF + YY (2-digit year) + 6-char uppercase alphanumeric
 * Total length = 10 chars. Example: PF26 + A1B2C3
 */
public final class TrackingIdGenerator {

    private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RAND = new SecureRandom();

    private TrackingIdGenerator() {}

    public static String generate() {
        int year = LocalDate.now().getYear() % 100;
        String yy = String.format("%02d", year);
        StringBuilder suffix = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            suffix.append(ALPHANUM.charAt(RAND.nextInt(ALPHANUM.length())));
        }
        return "PF" + yy + suffix;
    }

    /**
     * Validates format PF + 2 digits + 6 alnum (total 10)
     * Used for logging/debugging, not strictly enforced for lookup.
     */
    public static boolean isValidFormat(String trackingId) {
        if (trackingId == null) return false;
        return trackingId.matches("^PF\\d{2}[A-Z0-9]{6}$");
    }
}
