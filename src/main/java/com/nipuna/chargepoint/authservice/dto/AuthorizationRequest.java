package com.nipuna.chargepoint.authservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthorizationRequest {

    @NotBlank(message = "Station UUID is required")
    private String stationUuid;

    @Valid
    private DriverIdentifier driverIdentifier;
}
