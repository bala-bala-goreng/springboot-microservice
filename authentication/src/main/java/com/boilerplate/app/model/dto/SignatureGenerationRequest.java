package com.boilerplate.app.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignatureGenerationRequest {
    
    @NotBlank(message = "clientId is required")
    private String clientId;
    
    @NotBlank(message = "timestamp is required")
    private String timestamp;
    
    // Optional: if not provided, will be retrieved from database using clientId
    private String privateKey;
}

