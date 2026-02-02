package com.boilerplate.app.filter;

import com.boilerplate.app.config.GatewayRouteConfig;
import com.boilerplate.app.service.GatewayService;
import com.boilerplate.app.service.OAuth2TokenValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Authentication filter that validates tokens for routes requiring authentication.
 * Routes and public paths are configured via application.yml.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {
    private final GatewayService gatewayService;
    private final OAuth2TokenValidationService tokenValidationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        // Skip authentication for public paths
        if (gatewayService.isPublicPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if route requires authentication
        GatewayRouteConfig.Route route = gatewayService.findMatchingRoute(requestPath);
        if (route == null || !route.isRequiresAuth()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validate token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorizedResponse(response, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        if (!tokenValidationService.validate(token)) {
            sendUnauthorizedResponse(response, "Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
            "error", "Unauthorized",
            "message", message,
            "status", HttpServletResponse.SC_UNAUTHORIZED
        )));
    }
}
