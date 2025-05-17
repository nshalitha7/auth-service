package com.nipuna.chargepoint.authservice.controller;

import com.nipuna.chargepoint.authservice.dto.AuthorizationRequest;
import com.nipuna.chargepoint.authservice.dto.AuthorizationResponse;
import com.nipuna.chargepoint.authservice.kafka.AuthorizationKafkaRequest;
import com.nipuna.chargepoint.authservice.kafka.AuthorizationProducer;
import com.nipuna.chargepoint.authservice.service.ResponseManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthorizationController {

    private final AuthorizationProducer producer;
    private final ResponseManager responseManager;

    @Value("${app.response-timeout-ms:3000}")
    private long responseTimeoutMs;

    @PostMapping("/authorize")
    public ResponseEntity<AuthorizationResponse> authorize(@Valid @RequestBody AuthorizationRequest request) {
        String identifier = request.getDriverIdentifier().getId();
        if (isInvalidDriverIdentifier(identifier)) {
            log.warn("Invalid identifier length: {}", identifier);
            return ResponseEntity.ok(new AuthorizationResponse("Invalid"));
        }

        return handleKafkaAuthorization(request);
    }

    private boolean isInvalidDriverIdentifier(String identifier) {
        return identifier.length() < 20 || identifier.length() > 80;
    }

    private ResponseEntity<AuthorizationResponse> handleKafkaAuthorization(AuthorizationRequest request) {
        String correlationId = UUID.randomUUID().toString();
        String identifier = request.getDriverIdentifier().getId();

        log.info("Processing request. correlationId={}, identifier={}", correlationId, identifier);

        AuthorizationKafkaRequest kafkaRequest = new AuthorizationKafkaRequest(
                correlationId, request.getStationUuid(), identifier
        );

        CompletableFuture<String> future = new CompletableFuture<>();
        responseManager.register(correlationId, future);
        producer.sendAuthorizationRequest(kafkaRequest);

        try {
            String status = future.get(responseTimeoutMs, TimeUnit.MILLISECONDS);
            log.info("Returning response. correlationId={}, status={}", correlationId, status);
            return ResponseEntity.ok(new AuthorizationResponse(status));
        } catch (TimeoutException e) {
            log.warn("Timeout occurred for correlationId={}", correlationId);
            return respondAndCleanup(correlationId, 504, "Timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting. correlationId={}", correlationId, e);
            return respondAndCleanup(correlationId, 500, "Interrupted");
        } catch (Exception e) {
            log.error("Unexpected error. correlationId={}", correlationId, e);
            return respondAndCleanup(correlationId, 500, "InternalError");
        }
    }

    private ResponseEntity<AuthorizationResponse> respondAndCleanup(String correlationId, int status, String message) {
        responseManager.remove(correlationId);
        return ResponseEntity.status(status).body(new AuthorizationResponse(message));
    }
}