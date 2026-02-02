package com.boilerplate.app.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartnerRequest {
    @NotBlank(message = "Partner code is required")
    private String partnerCode; // Client ID

    @NotBlank(message = "Partner name is required")
    private String partnerName; // Name

    // B2B Authentication fields (optional)
    private String clientSecret; // Client Secret

    private String partnerPublicKey; // Partner Public Key

    private String paymentNotifyUrl; // Payment Notify URL

    // API Key fields (consolidated from ApiKey document)
    private String apiKey; // API Key (Client ID) - optional, auto-generated if not provided
    
    private String publicKey; // Public key for RSA authentication
    
    private String privateKey; // Private key for RSA authentication
    
    private java.time.LocalDateTime apiKeyExpiresAt; // API key expiration date

    private String createdBy; // User/system who created this partner
}

