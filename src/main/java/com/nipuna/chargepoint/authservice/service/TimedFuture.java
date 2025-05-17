package com.nipuna.chargepoint.authservice.service;

import lombok.Getter;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Getter
public class TimedFuture {
    private final CompletableFuture<String> future;
    private final long timestamp;

    public TimedFuture(CompletableFuture<String> future) {
        this.future = future;
        this.timestamp = Instant.now().toEpochMilli();
    }

}
