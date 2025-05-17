package com.nipuna.chargepoint.authservice.kafka;

import com.nipuna.chargepoint.authservice.service.ResponseManager;
import com.nipuna.chargepoint.authservice.service.WhitelistStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationConsumer {

    private final WhitelistStore whitelistStore;
    private final KafkaTemplate<String, AuthorizationKafkaResponse> kafkaTemplate;
    private final ResponseManager responseManager;

    @KafkaListener(
            topics = "auth-requests",
            groupId = "auth-service-group",
            containerFactory = "authRequestContainerFactory"
    )
    public void handleAuthorizationRequest(AuthorizationKafkaRequest request) {
        String correlationId = request.getCorrelationId();
        String identifier = request.getIdentifier();
        String status;

        log.info("Received auth request | correlationId={}, identifier={}", correlationId, identifier);

        try {
            if (!whitelistStore.contains(identifier)) {
                status = "Unknown";
            } else if (Boolean.TRUE.equals(whitelistStore.checkAuthorization(identifier))) {
                status = "Accepted";
            } else {
                status = "Rejected";
            }

            AuthorizationKafkaResponse response = new AuthorizationKafkaResponse(correlationId, status);
            kafkaTemplate.send("auth-responses", correlationId, response);

            log.info("Sent auth response | correlationId={}, status={}", correlationId, status);

        } catch (Exception e) {
            log.error("Failed to process auth request | correlationId={}, identifier={}", correlationId, identifier, e);
            AuthorizationKafkaResponse errorResponse = new AuthorizationKafkaResponse(correlationId, "InternalError");
            kafkaTemplate.send("auth-responses", correlationId, errorResponse);
        }
    }

    @KafkaListener(
            topics = "auth-responses",
            groupId = "auth-service-group",
            containerFactory = "authResponseContainerFactory"
    )
    public void handleAuthorizationResponse(AuthorizationKafkaResponse response) {
        String correlationId = response.getCorrelationId();
        String status = response.getAuthorizationStatus();

        log.info("Received auth response | correlationId={}, status={}", correlationId, status);

        responseManager.complete(correlationId, status);
    }
}
