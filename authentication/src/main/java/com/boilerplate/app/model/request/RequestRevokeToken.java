package com.boilerplate.app.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestRevokeToken {
    @NotBlank(message = "Token is required")
    private String token;
}

