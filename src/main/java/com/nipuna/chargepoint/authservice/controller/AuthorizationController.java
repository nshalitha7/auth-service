package com.nipuna.chargepoint.authservice.controller;

import com.nipuna.chargepoint.authservice.dto.AuthorizationRequest;
import com.nipuna.chargepoint.authservice.dto.AuthorizationResponse;
import com.nipuna.chargepoint.authservice.kafka.AuthorizationKafkaRequest;
import com.nipuna.chargepoint.authservice.kafka.AuthorizationProducer;
import com.nipuna.chargepoint.authservice.service.ResponseManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthorizationController {

    private final AuthorizationProducer producer;
    private final ResponseManager responseManager;

    @PostMapping("/authorize")
    public ResponseEntity<AuthorizationResponse> authorize(@Valid @RequestBody AuthorizationRequest request) {
        String identifier = request.getDriverIdentifier().getId();

        if (identifier.length() < 20 || identifier.length() > 80) {
            return ResponseEntity.ok(new AuthorizationResponse("Invalid"));
        }

        String correlationId = UUID.randomUUID().toString();
        AuthorizationKafkaRequest kafkaRequest = new AuthorizationKafkaRequest(
                correlationId,
                request.getStationUuid(),
                identifier
        );

        CompletableFuture<String> future = new CompletableFuture<>();
        responseManager.register(correlationId, future);
        producer.sendAuthorizationRequest(kafkaRequest);

        try {
            String status = future.get(3, TimeUnit.SECONDS); // timeout
            return ResponseEntity.ok(new AuthorizationResponse(status));
        } catch (TimeoutException e) {
            responseManager.remove(correlationId); // cleanup
            return ResponseEntity.status(504).body(new AuthorizationResponse("Timeout"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // re-interrupt
            responseManager.remove(correlationId); // cleanup
            return ResponseEntity.status(500).body(new AuthorizationResponse("Interrupted"));
        } catch (Exception e) {
            responseManager.remove(correlationId); // cleanup
            return ResponseEntity.status(500).body(new AuthorizationResponse("InternalError"));
        }
    }
}