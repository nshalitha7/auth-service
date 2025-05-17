package com.nipuna.chargepoint.authservice.kafka;

import com.nipuna.chargepoint.authservice.service.WhitelistStore;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorizationConsumer {

    private final WhitelistStore whitelistStore;
    private final KafkaTemplate<String, AuthorizationKafkaResponse> kafkaTemplate;

    @KafkaListener(topics = "auth-requests", groupId = "auth-service-group", containerFactory = "authRequestContainerFactory")
    public void handleAuthorizationRequest(AuthorizationKafkaRequest request) {
        String result;

        if (!whitelistStore.contains(request.getIdentifier())) {
            result = "Unknown";
        } else if (Boolean.TRUE.equals(whitelistStore.checkAuthorization(request.getIdentifier()))) {
            result = "Accepted";
        } else {
            result = "Rejected";
        }

        AuthorizationKafkaResponse response = new AuthorizationKafkaResponse(request.getCorrelationId(), result);
        kafkaTemplate.send("auth-responses", request.getCorrelationId(), response);
    }
}
