package com.nipuna.chargepoint.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DriverIdentifier {
    @NotBlank(message = "Identifier cannot be blank")
    private String id;
}