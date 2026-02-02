package com.boilerplate.app.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenRequest {
    
    @NotBlank(message = "grant_type is required")
    private String grantType;
    
    private String authorization;
    
    public boolean isClientCredentials() {
        return "client_credentials".equals(grantType);
    }
}
