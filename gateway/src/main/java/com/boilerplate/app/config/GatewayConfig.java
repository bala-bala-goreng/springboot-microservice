package com.boilerplate.app.config;

import io.micrometer.tracing.Tracer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
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

    @Autowired(required = false)
    private Tracer tracer;
    
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
        restTemplate.setMessageConverters(List.of(
            new StringHttpMessageConverter(StandardCharsets.UTF_8),
            new FormHttpMessageConverter(),
            new MappingJackson2HttpMessageConverter()
        ));
        restTemplate.getInterceptors().add(new TracePropagationInterceptor(tracer));
        return restTemplate;
    }
    
    @Slf4j
    private static class TracePropagationInterceptor implements ClientHttpRequestInterceptor {
        private final Tracer tracer;
        
        public TracePropagationInterceptor(Tracer tracer) {
            this.tracer = tracer;
        }
        private static final TextMapSetter<HttpRequest> SETTER = (carrier, key, value) -> {
            if (carrier != null) {
                carrier.getHeaders().add(key, value);
                log.debug("Injected trace header: {} = {}", key, value);
            }
        };
        
        @Override
        public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
        ) throws IOException {
            try {
                if (tracer != null && tracer.currentSpan() != null) {
                    io.micrometer.tracing.Span micrometerSpan = tracer.currentSpan();
                    if (micrometerSpan.context() != null) {
                        String traceId = micrometerSpan.context().traceId();
                        String spanId = micrometerSpan.context().spanId();
                        
                        if (traceId != null && spanId != null && !traceId.isEmpty() && !spanId.isEmpty()) {
                            String normalizedTraceId = normalizeHex(traceId, 32);
                            String normalizedSpanId = normalizeHex(spanId, 16);
                            String traceparent = String.format("00-%s-%s-01", normalizedTraceId, normalizedSpanId);
                            request.getHeaders().set("traceparent", traceparent);
                            log.debug("Trace context injected via Micrometer: traceparent={}, traceId={}, spanId={} for request to: {}", 
                                traceparent, traceId, spanId, request.getURI());
                            return execution.execute(request, body);
                        } else {
                            log.debug("Micrometer span context has null/empty traceId or spanId: traceId={}, spanId={}", traceId, spanId);
                        }
                    } else {
                        log.debug("Micrometer span context is null");
                    }
                } else {
                    log.debug("Micrometer Tracer is null or no current span");
                }
                
                io.opentelemetry.api.OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
                if (openTelemetry != null) {
                    io.opentelemetry.context.propagation.ContextPropagators propagators = 
                        openTelemetry.getPropagators();
                    
                    if (propagators != null && propagators.getTextMapPropagator() != null) {
                        Span currentSpan = Span.current();
                        Context contextToInject = Context.current();
                        
                        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
                            contextToInject = contextToInject.with(currentSpan);
                            log.debug("Using OpenTelemetry span: traceId={}, spanId={}", 
                                currentSpan.getSpanContext().getTraceId(), 
                                currentSpan.getSpanContext().getSpanId());
                        } else {
                            log.debug("No valid OpenTelemetry span found");
                        }
                        
                        propagators.getTextMapPropagator().inject(
                            contextToInject,
                            request,
                            SETTER
                        );
                        
                        String traceparent = request.getHeaders().getFirst("traceparent");
                        if (traceparent != null) {
                            log.debug("Trace context injected via OpenTelemetry: traceparent={} for request to: {}", 
                                traceparent, request.getURI());
                        } else {
                            log.warn("Trace context injection failed: traceparent header not found after injection. OpenTelemetry span valid: {}", 
                                currentSpan != null && currentSpan.getSpanContext().isValid());
                        }
                    } else {
                        log.warn("TextMapPropagator is null, cannot inject trace context");
                    }
                } else {
                    log.warn("OpenTelemetry is null, cannot inject trace context");
                }
            } catch (Exception e) {
                log.error("Failed to inject trace context: {}", e.getMessage(), e);
            }
            return execution.execute(request, body);
        }
        
        private String normalizeHex(String hex, int length) {
            if (hex == null) {
                return "0".repeat(length);
            }
            hex = hex.replaceAll("[^0-9a-fA-F]", "").toLowerCase();
            if (hex.length() == length) {
                return hex;
            } else if (hex.length() > length) {
                return hex.substring(0, length);
            } else {
                return String.format("%-" + length + "s", hex).replace(' ', '0');
            }
        }
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
