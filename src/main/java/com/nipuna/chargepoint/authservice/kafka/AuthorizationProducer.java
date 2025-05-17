package com.nipuna.chargepoint.authservice.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorizationProducer {

    private final KafkaTemplate<String, AuthorizationKafkaRequest> kafkaTemplate;

    public void sendAuthorizationRequest(AuthorizationKafkaRequest request) {
        kafkaTemplate.send("auth-requests", request.getCorrelationId(), request);
    }
}
