package com.boilerplate.app.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestOAuth2Token {
    @NotBlank(message = "grant_type is required")
    @JsonProperty("grant_type")
    private String grantType;
}
