package com.ads.adshub.config;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AppInterceptor implements HandlerInterceptor {

	private final LogUtil logUtil;

	public AppInterceptor(LogUtil logUtil) {
		this.logUtil = logUtil;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		// Wrap request/response for caching body (required to read multiple times)
		if (!(request instanceof ContentCachingRequestWrapper)) {
			request = new ContentCachingRequestWrapper(request);
		}
		if (!(response instanceof ContentCachingResponseWrapper)) {
			response = new ContentCachingResponseWrapper(response);
		}

		// Extract headers / attributes
		String correlationId = getOrGenerateCorrelationId(request);
		Long appId = parseLong(request.getHeader("X-App-Id"), 1L);
		Integer userId = parseInt(request.getHeader("X-User-Id"), 10);
		String ipAddress = request.getRemoteAddr() != null ? request.getRemoteAddr() : "0.0.0.0";
		Double longitude = parseDouble(request.getHeader("X-Longitude"), 0.0);
		Double latitude = parseDouble(request.getHeader("X-Latitude"), 0.0);
		long startTime = System.currentTimeMillis();

		// Store in request attributes for later use
		request.setAttribute("correlationId", correlationId);
		request.setAttribute("appId", appId);
		request.setAttribute("userId", userId);
		request.setAttribute("ipAddress", ipAddress);
		request.setAttribute("longitude", longitude);
		request.setAttribute("latitude", latitude);
	    request.setAttribute("startTime", startTime);

		// Store in thread-local if using DataContext
		DataContext.setCorrelationId(correlationId);
		DataContext.setAppId(appId);
		DataContext.setUserId(userId);

		// Log request
		logUtil.requestLog(request, requestBody(request), correlationId, appId, userId, ipAddress, longitude, latitude);

		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
	        throws Exception {

	    String correlationId = (String) request.getAttribute("correlationId");
//	    long duration = System.currentTimeMillis() - (Long) request.getAttribute("startTime");
	    int status = response.getStatus();

	    // Only log exceptions here
	    if (ex != null) {
	        logUtil.exceptionHandlerLog(ex, null, correlationId, status);
	    }
	}

	private String getOrGenerateCorrelationId(HttpServletRequest request) {
		String correlationId = request.getHeader("X-Correlation-Id");
		if (correlationId == null || correlationId.isBlank()) {
			String timestamp = LocalDateTime.now()
	                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
			correlationId = "COR-" + UUID.randomUUID()+ "-" + timestamp;
		}
		return correlationId;
	}

	private Double parseDouble(String value, Double defaultValue) {
		try {
			return (value != null) ? Double.parseDouble(value) : defaultValue;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private Long parseLong(String value, Long defaultValue) {
		try {
			return (value != null) ? Long.parseLong(value) : defaultValue;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private Integer parseInt(String value, Integer defaultValue) {
		try {
			return (value != null) ? Integer.parseInt(value) : defaultValue;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private String requestBody(HttpServletRequest request) {
		try {
			ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
			byte[] buf = wrapper.getContentAsByteArray();
			return (buf.length > 0) ? new String(buf, StandardCharsets.UTF_8) : null;
		} catch (Exception e) {
			return null;
		}
	}

	
}

