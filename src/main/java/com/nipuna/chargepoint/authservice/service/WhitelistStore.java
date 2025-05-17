package com.nipuna.chargepoint.authservice.service;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WhitelistStore {

    // true = allowed, false = rejected
    private static final Map<String, Boolean> whitelist = Map.of(
            "id12345678901234567890", true,
            "id98765432109876543210987", false
    );

    public Boolean checkAuthorization(String identifier) {
        return whitelist.get(identifier);
    }

    public boolean contains(String identifier) {
        return whitelist.containsKey(identifier);
    }
}
