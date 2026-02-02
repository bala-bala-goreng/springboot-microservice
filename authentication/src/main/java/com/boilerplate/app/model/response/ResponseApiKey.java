package com.boilerplate.app.model.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResponseApiKey {
    private String id;
    private String apiKey;
    private String partnerCode;
    private LocalDateTime expiresAt;
}

