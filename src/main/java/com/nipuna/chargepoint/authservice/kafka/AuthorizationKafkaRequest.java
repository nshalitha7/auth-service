package com.nipuna.chargepoint.authservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationKafkaRequest {
    private String correlationId;
    private String stationUuid;
    private String identifier;
}
