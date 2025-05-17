package com.nipuna.chargepoint.authservice.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ResponseManager {
    private final Map<String, TimedFuture> futureMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    @Value("${app.future-expiration-ms:300000}")
    private long expirationMs;

    public void register(String correlationId, CompletableFuture<String> future) {
        futureMap.put(correlationId, new TimedFuture(future));
    }

    public void complete(String correlationId, String result) {
        TimedFuture tf = futureMap.remove(correlationId);
        if (tf != null) {
            tf.getFuture().complete(result);
        } else {
            log.warn("No pending future found for correlationId={}", correlationId);
        }
    }

    public void remove(String correlationId) {
        futureMap.remove(correlationId);
    }

    @PostConstruct
    public void startCleanupTask() {
        scheduler.scheduleAtFixedRate(this::cleanup, 1, 60, TimeUnit.SECONDS);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        int removed = 0;

        Iterator<Map.Entry<String, TimedFuture>> iterator = futureMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, TimedFuture> entry = iterator.next();
            if (now - entry.getValue().getTimestamp() > expirationMs) {
                entry.getValue().getFuture().complete("Expired");
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0) {
            log.info("Cleaned up {} expired correlationId entries", removed);
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }
}
