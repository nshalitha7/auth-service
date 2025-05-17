package com.nipuna.chargepoint.authservice.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhitelistStoreTest {
    private final WhitelistStore store = new WhitelistStore();

    @Test
    void shouldReturnTrueForAllowedIdentifier() {
        assertEquals(Boolean.TRUE, store.checkAuthorization("id12345678901234567890"));
    }

    @Test
    void shouldReturnFalseForRejectedIdentifier() {
        assertNotEquals(Boolean.TRUE, store.checkAuthorization("id98765432109876543210987"));
    }

    @Test
    void shouldReturnNullForUnknownIdentifier() {
        assertNull(store.checkAuthorization("unknown-id"));
    }

    @Test
    void containsShouldReturnTrueForKnownId() {
        assertTrue(store.contains("id12345678901234567890"));
    }

    @Test
    void containsShouldReturnFalseForUnknownId() {
        assertFalse(store.contains("unknown-id"));
    }
}