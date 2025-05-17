package com.nipuna.chargepoint.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ResponseManagerTest {

    private ResponseManager manager;

    @BeforeEach
    void setup() {
        manager = new ResponseManager();
        manager.startCleanupTask();
    }

    @Test
    void shouldRegisterAndCompleteFuture() throws ExecutionException, InterruptedException {
        String correlationId = "test-id";
        CompletableFuture<String> future = new CompletableFuture<>();
        manager.register(correlationId, future);

        manager.complete(correlationId, "Accepted");

        assertEquals("Accepted", future.get());
    }

    @Test
    void shouldRemoveFuture() {
        String correlationId = "to-remove";
        CompletableFuture<String> future = new CompletableFuture<>();
        manager.register(correlationId, future);

        manager.remove(correlationId);

        assertFalse(future.isDone());
    }
}