package com.parcelflow.notification;

import com.parcelflow.notification.config.Config;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {
    @Test
    void defaultsAreSensible() {
        assertTrue(Config.getPort() > 0);
        assertNotNull(Config.getJdbcUrl());
    }
}
