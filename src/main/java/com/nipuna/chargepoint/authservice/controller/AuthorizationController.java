package com.nipuna.chargepoint.authservice.controller;

import com.nipuna.chargepoint.authservice.dto.AuthorizationRequest;
import com.nipuna.chargepoint.authservice.dto.AuthorizationResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthorizationController {

    @PostMapping("/authorize")
    public ResponseEntity<AuthorizationResponse> authorize(@Valid @RequestBody AuthorizationRequest request) {
        String identifier = request.getDriverIdentifier().getId();

        if (identifier.length() < 20 || identifier.length() > 80) {
            return ResponseEntity.ok(new AuthorizationResponse("Invalid"));
        }

        // todo Placeholder for now: need to send it to Kafka
        return ResponseEntity.ok(new AuthorizationResponse("Pending"));
    }
}