package com.boilerplate.app.service;

import com.boilerplate.app.config.GatewayRouteConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayService {
    private final RestTemplate restTemplate;
    private final GatewayRouteConfig gatewayRouteConfig;

    /**
     * Find matching route for the given path.
     * Returns the route configuration if found, null otherwise.
     */
    public GatewayRouteConfig.Route findMatchingRoute(String path) {
        if (gatewayRouteConfig.getRoutes() == null || gatewayRouteConfig.getRoutes().isEmpty()) {
            return null;
        }

        return gatewayRouteConfig.getRoutes().stream()
            .filter(route -> route.getPath() != null && matchesPath(path, route.getPath()))
            .max((r1, r2) -> Integer.compare(r1.getPath().length(), r2.getPath().length()))
            .orElse(null);
    }

    /**
     * Check if path matches the pattern (supports ** wildcard).
     */
    private boolean matchesPath(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return path.equals(pattern) || path.startsWith(pattern + "/");
    }

    /**
     * Check if path is public (doesn't require authentication).
     */
    public boolean isPublicPath(String path) {
        if (gatewayRouteConfig.getPublicPaths() == null) {
            return false;
        }
        return gatewayRouteConfig.getPublicPaths().stream()
            .anyMatch(publicPath -> path.startsWith(publicPath));
    }

    public ResponseEntity<String> routeRequest(
        String serviceId,
        String path,
        HttpMethod method,
        HttpHeaders headers,
        Object body,
        HttpServletRequest request
    ) {
        try {
            String pathValue = path != null ? path : "";
            String queryString = request != null ? request.getQueryString() : null;
            if (queryString != null && !queryString.isEmpty()) {
                pathValue += "?" + queryString;
            }
            
            // Build target URL using service name - @LoadBalanced RestTemplate will resolve via Eureka
            String targetUrl = "http://" + serviceId + pathValue;
            log.debug("Routing {} {} to service {} at {}", method, request != null ? request.getRequestURI() : path, serviceId, targetUrl);

            HttpHeaders requestHeaders = copyHeaders(headers, request);
            Object requestBody = body;
            String contentType = request != null ? request.getContentType() : null;
            if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
                if (body instanceof String && StringUtils.hasText((String) body)) {
                    MultiValueMap<String, String> formData = parseFormData((String) body);
                    requestBody = formData;
                    requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                }
            } else if (contentType != null && contentType.contains("application/json")) {
                requestHeaders.setContentType(MediaType.APPLICATION_JSON);
            }

            HttpEntity<?> requestEntity = new HttpEntity<>(requestBody, requestHeaders);
            HttpMethod httpMethod = method != null ? method : HttpMethod.GET;
            ResponseEntity<String> response = restTemplate.exchange(
                targetUrl,
                httpMethod,
                requestEntity,
                String.class
            );

            HttpHeaders filteredHeaders = filterResponseHeaders(response.getHeaders());

            return ResponseEntity
                .status(response.getStatusCode())
                .headers(filteredHeaders)
                .body(response.getBody());

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            HttpHeaders responseHeaders = filterResponseHeaders(e.getResponseHeaders());

            if (!responseBody.isEmpty() && responseHeaders.getContentType() == null) {
                responseHeaders.setContentType(MediaType.APPLICATION_JSON);
                responseHeaders.setContentLength(responseBody.getBytes(StandardCharsets.UTF_8).length);
            }

            return ResponseEntity
                .status(e.getStatusCode())
                .headers(responseHeaders)
                .body(responseBody);
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("No instances available")) {
                log.error("No instances available for service {}", serviceId);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("{\"error\":\"Service unavailable\",\"message\":\"No instances available for service " + serviceId + "\"}");
            }
            log.error("Error routing to service {}: {}", serviceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"error\":\"Gateway error\",\"message\":\"" + e.getMessage() + "\"}");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Connection error routing to service {}: {}", serviceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("{\"error\":\"Service unavailable\",\"message\":\"Connection failed\"}");
        } catch (Exception e) {
            log.error("Unexpected error routing to service {}: {}", serviceId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"error\":\"Gateway error\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    private HttpHeaders filterResponseHeaders(HttpHeaders originalHeaders) {
        HttpHeaders filteredHeaders = new HttpHeaders();

        if (originalHeaders != null) {
            originalHeaders.forEach((key, values) -> {
                String lowerKey = key.toLowerCase();
                if (!lowerKey.equals("content-length") &&
                    !lowerKey.equals("transfer-encoding") &&
                    !lowerKey.equals("connection") &&
                    !lowerKey.equals("host") &&
                    !lowerKey.equals("keep-alive")) {
                    filteredHeaders.put(key, values);
                }
            });
        }

        return filteredHeaders;
    }

    private HttpHeaders copyHeaders(HttpHeaders existingHeaders, HttpServletRequest request) {
        HttpHeaders headers = existingHeaders != null ? new HttpHeaders(existingHeaders) : new HttpHeaders();

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String lowerHeaderName = headerName.toLowerCase();
            if (!lowerHeaderName.equals("host") &&
                !lowerHeaderName.equals("content-length") &&
                !lowerHeaderName.equals("connection") &&
                !lowerHeaderName.equals("transfer-encoding")) {
                Enumeration<String> headerValues = request.getHeaders(headerName);
                while (headerValues.hasMoreElements()) {
                    String headerValue = headerValues.nextElement();
                    if (!headers.containsKey(headerName)) {
                        headers.add(headerName, headerValue);
                    } else {
                        headers.set(headerName, headerValue);
                    }
                }
            }
        }

        headers.remove("host");
        headers.remove("connection");
        headers.remove("transfer-encoding");

        return headers;
    }

    private MultiValueMap<String, String> parseFormData(String formDataString) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        if (formDataString == null || formDataString.isEmpty()) {
            return formData;
        }

        String[] pairs = formDataString.split("&");
        for (String pair : pairs) {
            if (pair == null || pair.isEmpty()) {
                continue;
            }
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2 && keyValue[0] != null) {
                String value = keyValue[1] != null ? keyValue[1] : "";
                formData.add(keyValue[0], value);
            } else if (keyValue.length == 1 && keyValue[0] != null) {
                formData.add(keyValue[0], "");
            }
        }
        return formData;
    }
}
