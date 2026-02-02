package com.boilerplate.app.model.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseMessage {
    private String message;
    private String partnerCode;
    private String apiKey;
    
    public ResponseMessage() {
    }
    
    public ResponseMessage(String message) {
        this.message = message;
    }
    
    public ResponseMessage(String message, String partnerCode) {
        this.message = message;
        this.partnerCode = partnerCode;
    }
    
    public ResponseMessage(String message, String partnerCode, String apiKey) {
        this.message = message;
        this.partnerCode = partnerCode;
        this.apiKey = apiKey;
    }
}

