package com.boilerplate.app.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ApiKeyRequest {
    @NotBlank(message = "Partner code is required")
    private String partnerCode;

    private String apiKey; // Optional: if not provided, will be auto-generated

    @NotBlank(message = "Secret key is required")
    private String secretKey;

    private String publicKey;
    private String privateKey;
    private LocalDateTime expiresAt;
}

