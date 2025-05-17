package com.nipuna.chargepoint.authservice.controller;

import com.nipuna.chargepoint.authservice.dto.AuthorizationRequest;
import com.nipuna.chargepoint.authservice.dto.AuthorizationResponse;
import com.nipuna.chargepoint.authservice.kafka.AuthorizationKafkaRequest;
import com.nipuna.chargepoint.authservice.kafka.AuthorizationProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthorizationController {

    private final AuthorizationProducer producer;

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

        producer.sendAuthorizationRequest(kafkaRequest);

        // todo
        return ResponseEntity.ok(new AuthorizationResponse("Pending"));
    }
}