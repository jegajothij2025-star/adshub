package com.ads.adshub.config;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class ResponseCaptureAdvice implements ResponseBodyAdvice<Object> {

    private final LogUtil logUtil;

    public ResponseCaptureAdvice(LogUtil logUtil) {
        this.logUtil = logUtil;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Apply to all responses
        return true;
    }

    @SuppressWarnings("unchecked")
	@Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        // Extract correlationId if available
        String correlationId = (String) request.getHeaders().getFirst("X-Correlation-Id");
        if (correlationId == null && request instanceof org.springframework.http.server.ServletServerHttpRequest servletRequest) {
            correlationId = (String) servletRequest.getServletRequest().getAttribute("correlationId");
        }

        long startTime = 0L;
        if (request instanceof org.springframework.http.server.ServletServerHttpRequest servletRequest) {
            Object start = servletRequest.getServletRequest().getAttribute("startTime");
            if (start instanceof Long l) startTime = l;
        }

        long duration = (startTime > 0) ? System.currentTimeMillis() - startTime : 0;

        int status = (response instanceof org.springframework.http.server.ServletServerHttpResponse servletResponse)
                ? servletResponse.getServletResponse().getStatus()
                : 200;

        // Create ResponseEntity-like wrapper
        ResponseEntity<Object> responseEntity = (body instanceof ResponseEntity<?> re)
                ? (ResponseEntity<Object>) re
                : ResponseEntity.status(status).body(body);

        // Call your existing logger
        try {
			logUtil.responseLog(status, duration, responseEntity, correlationId);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return body; // let the response proceed unchanged
    }
}
