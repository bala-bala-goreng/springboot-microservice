package com.boilerplate.app.model.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseError {
    private String responseCode;
    private String responseMessage;
    private Object data;
}
