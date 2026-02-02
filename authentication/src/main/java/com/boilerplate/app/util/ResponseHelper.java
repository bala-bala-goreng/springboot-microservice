package com.boilerplate.app.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

public class ResponseHelper {

    /**
     * Creates an error response with a single error message
     * 
     * @param status HTTP status code
     * @param message Error message
     * @return ResponseEntity with error response
     */
    public static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Creates an error response with multiple error fields
     * 
     * @param status HTTP status code
     * @param errorMap Map containing error fields
     * @return ResponseEntity with error response
     */
    public static ResponseEntity<Map<String, Object>> error(HttpStatus status, Map<String, Object> errorMap) {
        return ResponseEntity.status(status).body(errorMap);
    }

    /**
     * Creates a success response with a message
     * 
     * @param message Success message
     * @return ResponseEntity with success response
     */
    public static ResponseEntity<Map<String, Object>> success(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a success response with status code and custom data
     * 
     * @param status HTTP status code
     * @param data Response data
     * @return ResponseEntity with success response
     */
    public static ResponseEntity<Map<String, Object>> success(HttpStatus status, Map<String, Object> data) {
        return ResponseEntity.status(status).body(data);
    }

    /**
     * Creates a 400 Bad Request error response
     * 
     * @param message Error message
     * @return ResponseEntity with 400 status
     */
    public static ResponseEntity<Map<String, Object>> badRequest(String message) {
        return error(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Creates a 401 Unauthorized error response
     * 
     * @param message Error message
     * @return ResponseEntity with 401 status
     */
    public static ResponseEntity<Map<String, Object>> unauthorized(String message) {
        return error(HttpStatus.UNAUTHORIZED, message);
    }

    /**
     * Creates a 409 Conflict error response
     * 
     * @param message Error message
     * @return ResponseEntity with 409 status
     */
    public static ResponseEntity<Map<String, Object>> conflict(String message) {
        return error(HttpStatus.CONFLICT, message);
    }

    /**
     * Creates a 500 Internal Server Error response
     * 
     * @param message Error message
     * @return ResponseEntity with 500 status
     */
    public static ResponseEntity<Map<String, Object>> internalServerError(String message) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public static ResponseEntity<Map<String, Object>> success(Object response) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> responseMap = mapper.convertValue(response, new TypeReference<>() {});
        return ResponseEntity.ok(responseMap);
    }

    public static ResponseEntity<Map<String, Object>> error(Object response, HttpStatus status){
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> responseMap = mapper.convertValue(response, new TypeReference<>() {});
        return ResponseEntity.status(status).body(responseMap);
    }
}

