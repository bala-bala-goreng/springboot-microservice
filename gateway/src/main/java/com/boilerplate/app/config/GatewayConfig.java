package com.boilerplate.app.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Gateway configuration for RestTemplate with LoadBalancer support.
 * Enables service discovery via Eureka.
 */
@Configuration
public class GatewayConfig {
    @Value("${gateway.http-client.max-total-connections:200}")
    private int maxTotalConnections;

    @Value("${gateway.http-client.max-connections-per-route:50}")
    private int maxConnectionsPerRoute;

    @Value("${gateway.http-client.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${gateway.http-client.read-timeout:30000}")
    private int readTimeout;

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
        restTemplate.setMessageConverters(List.of(
            new StringHttpMessageConverter(StandardCharsets.UTF_8),
            new FormHttpMessageConverter(),
            new MappingJackson2HttpMessageConverter()
        ));
        return restTemplate;
    }

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxTotalConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.of(connectTimeout, TimeUnit.MILLISECONDS))
            .setResponseTimeout(Timeout.of(readTimeout, TimeUnit.MILLISECONDS))
            .build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .evictIdleConnections(Timeout.of(30, TimeUnit.SECONDS))
            .evictExpiredConnections()
            .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
