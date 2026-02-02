package com.boilerplate.app.service;

import com.boilerplate.app.config.GatewayRouteConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for validating OAuth2 tokens.
 * Validates tokens by calling the authentication service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2TokenValidationService {
    private final RestTemplate restTemplate;
    private final GatewayRouteConfig gatewayRouteConfig;

    public boolean validate(String token) {
        String validationEndpoint = gatewayRouteConfig.getOauth2() != null 
            ? gatewayRouteConfig.getOauth2().getValidationEndpoint() 
            : null;
        
        if (validationEndpoint == null || validationEndpoint.isEmpty()) {
            log.error("Token validation endpoint not configured");
            return false;
        }

        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("token", token);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                validationEndpoint,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Boolean valid = (Boolean) response.getBody().get("valid");
                return Boolean.TRUE.equals(valid);
            }

            return false;
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }
}
