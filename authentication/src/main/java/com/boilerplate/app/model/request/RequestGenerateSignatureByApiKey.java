package com.boilerplate.app.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestGenerateSignatureByApiKey {
    @NotBlank(message = "API Key is required")
    private String apiKey;
    
    @NotBlank(message = "Timestamp is required")
    private String timestamp;
}

