package com.boilerplate.app.controller;

import com.boilerplate.app.config.GatewayRouteConfig;
import com.boilerplate.app.service.GatewayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Gateway controller that routes all requests to backend services.
 * Routes are configured via application.yml.
 */
@Slf4j
@RestController
@RequestMapping("/**")
@RequiredArgsConstructor
public class GatewayController {
    private final GatewayService gatewayService;

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public ResponseEntity<String> route(
        @RequestHeader(required = false) HttpHeaders headers,
        @RequestBody(required = false) String body,
        HttpServletRequest request) {
        
        String requestPath = request.getRequestURI();
        GatewayRouteConfig.Route route = gatewayService.findMatchingRoute(requestPath);
        
        if (route == null) {
            log.warn("No route found for path: {}", requestPath);
            return ResponseEntity.status(404)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"No route found for path: " + requestPath + "\"}");
        }

        HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());
        Object requestBody = (body != null && !body.isEmpty()) ? body : null;
        
        return gatewayService.routeRequest(
            route.getService(),
            requestPath,
            httpMethod,
            headers,
            requestBody,
            request
        );
    }
}
