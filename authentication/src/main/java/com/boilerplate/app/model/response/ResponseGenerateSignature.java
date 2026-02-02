package com.boilerplate.app.model.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseGenerateSignature {
    private String signature;
    private String stringToSign;
    private String clientId;
    private String timestamp;
    private String algorithm;
    private String encoding;
    private String signatureType;
    private String authenticationMethod;
    private String apiKey;
}

