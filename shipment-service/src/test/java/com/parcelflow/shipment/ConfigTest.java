package com.parcelflow.shipment;

import com.parcelflow.shipment.config.Config;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {
    @Test
    void defaultsAreSensible() {
        // Without env overrides, ports should be defaults
        // This test verifies Config doesn't throw
        assertTrue(Config.getPort() > 0);
        assertNotNull(Config.getJdbcUrl());
        assertTrue(Config.getJdbcUrl().startsWith("jdbc:mysql://"));
    }

    @Test
    void jdbcUrlContainsDbName() {
        String url = Config.getJdbcUrl();
        assertTrue(url.contains(Config.getDbName()));
    }
}
