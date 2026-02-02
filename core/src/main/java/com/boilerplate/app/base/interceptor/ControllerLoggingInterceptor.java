package com.boilerplate.app.base.interceptor;

import com.boilerplate.app.base.constant.TraceConstants;
import com.boilerplate.app.base.logging.RequestResponseLogger;
import com.boilerplate.app.base.util.TraceIdUtil;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Base logging interceptor for HTTP requests/responses.
 * 
 * Best Practice:
 * - Spring Boot 3.x with Micrometer Tracing automatically handles trace context propagation
 * - Trace ID is automatically populated in MDC by Micrometer
 * - This interceptor only focuses on logging request/response details
 * 
 * Each service should create a @Component that extends this class.
 */
public class ControllerLoggingInterceptor implements HandlerInterceptor {

    @Autowired(required = false)
    private Tracer tracer;

    @Override
    public boolean preHandle(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull Object handler
    ) {
        long startTime = System.currentTimeMillis();
        request.setAttribute(TraceConstants.REQUEST_ATTRIBUTE_START_TIME, startTime);

        String traceId = getTraceId();
        request.setAttribute(TraceConstants.REQUEST_ATTRIBUTE_TRACE_ID, traceId);

        Map<String, String> requestHeaders = getHeaders(request);
        byte[] requestBody = getRequestBody(request);
        
        RequestResponseLogger.logRequest(
            traceId,
            request.getMethod(),
            request.getRequestURL().toString(),
            requestHeaders,
            requestBody
        );

        return true;
    }

    @Override
    public void postHandle(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull Object handler,
        @Nullable ModelAndView modelAndView
    ) {
        String traceId = (String) request.getAttribute(TraceConstants.REQUEST_ATTRIBUTE_TRACE_ID);
        Long startTime = (Long) request.getAttribute(TraceConstants.REQUEST_ATTRIBUTE_START_TIME);
        long elapsedTime = startTime != null ? System.currentTimeMillis() - startTime : 0;

        Map<String, String> headers = getResponseHeaders(response);
        byte[] body = getResponseBody(response);
        
        RequestResponseLogger.logResponse(traceId, response.getStatus(), headers, body, elapsedTime);
    }

    @Override
    public void afterCompletion(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull Object handler,
        @Nullable Exception ex
    ) {
        if (ex != null) {
            String traceId = getTraceId();
            RequestResponseLogger.logError(
                traceId,
                request.getMethod(),
                request.getRequestURL().toString(),
                ex,
                response.getStatus()
            );
        }
    }

    /**
     * Get trace ID from Micrometer Tracing (automatically populated by Spring Boot).
     * Falls back to MDC if Micrometer is not available.
     */
    private String getTraceId() {
        if (tracer != null && tracer.currentSpan() != null) {
            String traceId = tracer.currentSpan().context().traceId();
            if (traceId != null && !traceId.isEmpty()) {
                return traceId;
            }
        }
        return TraceIdUtil.get();
    }

    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            headers.put(headerName, request.getHeader(headerName));
        });
        return headers;
    }

    private Map<String, String> getResponseHeaders(HttpServletResponse response) {
        Map<String, String> headers = new HashMap<>();
        response.getHeaderNames().forEach(headerName -> {
            headers.put(headerName, response.getHeader(headerName));
        });
        return headers;
    }

    private byte[] getRequestBody(HttpServletRequest request) {
        Object cachedBodyAttr = request.getAttribute(TraceConstants.REQUEST_ATTRIBUTE_CACHED_BODY);
        if (cachedBodyAttr instanceof byte[]) {
            return (byte[]) cachedBodyAttr;
        }
        
        if (request instanceof ContentCachingRequestWrapper cachedRequest) {
            byte[] body = cachedRequest.getContentAsByteArray();
            if (body != null && body.length > 0) {
                return body;
            }
        }
        
        return null;
    }

    private byte[] getResponseBody(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper cachedResponse) {
            return cachedResponse.getContentAsByteArray();
        }
        return null;
    }
}

