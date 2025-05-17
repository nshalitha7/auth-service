package com.nipuna.chargepoint.authservice.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ResponseManager {

    private final Map<String, CompletableFuture<String>> futures = new ConcurrentHashMap<>();

    public void register(String correlationId, CompletableFuture<String> future) {
        futures.put(correlationId, future);
    }

    public void complete(String correlationId, String result) {
        CompletableFuture<String> future = futures.remove(correlationId);
        if (future != null) {
            future.complete(result);
        }
    }

    public void remove(String correlationId) {
        futures.remove(correlationId);
    }
}
