package com.boilerplate.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified gateway configuration.
 * All routes and endpoints are managed via YAML properties.
 */
@ConfigurationProperties(prefix = "gateway")
@Getter
@Setter
public class GatewayRouteConfig {
    
    private OAuth2 oauth2 = new OAuth2();
    private List<Route> routes = new ArrayList<>();
    private List<String> publicPaths = new ArrayList<>();

    @Getter
    @Setter
    public static class OAuth2 {
        private String validationEndpoint;
    }

    @Getter
    @Setter
    public static class Route {
        /**
         * Path pattern that matches this route (e.g., /api/accounts/**)
         */
        private String path;
        
        /**
         * Target service ID in Eureka (e.g., service-account)
         */
        private String service;
        
        /**
         * Whether token validation is required for this route
         */
        private boolean requiresAuth = true;
    }
}
