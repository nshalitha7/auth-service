package com.nipuna.chargepoint.authservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationKafkaResponse {
    private String correlationId;
    private String authorizationStatus;
}
