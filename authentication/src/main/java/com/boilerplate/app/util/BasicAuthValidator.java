package com.boilerplate.app.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class BasicAuthValidator {

    public static BasicAuthCredentials validateAndExtract(String authorization) throws BasicAuthException {
        if (authorization == null || !authorization.startsWith("Basic ")) {
            throw new BasicAuthException("Authorization header with Basic authentication is required");
        }

        try {
            // Extract Base64 encoded credentials
            String base64Credentials = authorization.substring("Basic ".length()).trim();
            String credentials = new String(
                Base64.getDecoder().decode(base64Credentials), 
                StandardCharsets.UTF_8
            );
            
            // Split client_id:client_secret
            String[] parts = credentials.split(":", 2);
            if (parts.length != 2) {
                throw new BasicAuthException(
                    "Invalid Basic authentication format. Expected: base64(client_id:client_secret)"
                );
            }
            
            return new BasicAuthCredentials(parts[0], parts[1]);
        } catch (IllegalArgumentException e) {
            throw new BasicAuthException("Invalid Base64 encoding in Authorization header", e);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class BasicAuthCredentials {
        private final String clientId;
        private final String clientSecret;
    }

    @RequiredArgsConstructor
    @Getter
    public static class BasicAuthException extends Exception {
        private final String message;
        
        public BasicAuthException(String message, Throwable cause) {
            super(message, cause);
            this.message = message;
        }
    }
}

