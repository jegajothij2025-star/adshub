package com.ads.adshub.config;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.ads.adshub.model.ApiResponse;
import com.ads.adshub.model.ApiResponseLog;
import com.ads.adshub.log.CodeLog;
import com.ads.adshub.log.ExceptionHandlerLog;
import com.ads.adshub.log.MethodLog;
import com.ads.adshub.log.RequestLog;
import com.ads.adshub.log.ResponseLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LogUtil {

	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	public MaskingUtil maskingUtil;

	public void requestLog(HttpServletRequest request, Object body, String correlationId, Long appId, Integer userId,
			String ipAddress, Double longitude, Double latitude) throws IllegalArgumentException, IllegalAccessException {
		RequestLog requestLog = new RequestLog();
		requestLog.setLogType("HTTP_REQUEST");
		requestLog.setLogLevel("INFO");
		requestLog.setTimestamp(Instant.now());
		requestLog.setCorrelationId(correlationId);
		requestLog.setHttpMethod(request.getMethod());
		requestLog.setUrl(request.getRequestURI());
		requestLog.setRequestHeaders(Map.of("Content-Type", "application/json"));
		if (body instanceof Exception) {
		    requestLog.setRequestBody(((Exception) body).toString());
//		    String maskedBody = maskAnnotatedFields(body);
//		    requestLog.setRequestBody(maskedBody);

		}
		else {
			Object maskedBody = maskingUtil.maskObject(body);
			requestLog.setRequestBody(maskedBody);
		}
		requestLog.setAppId(appId);
		requestLog.setUserId(userId);
		requestLog.setIpAddress(ipAddress);
		requestLog.setLongitude(longitude);
		requestLog.setLatitude(latitude);
		logAsString(requestLog);
	}

	public void responseLog(int status, long duration, Object body, String correlationId) throws IllegalArgumentException, IllegalAccessException {
	    ResponseLog responseLog = new ResponseLog();
	    responseLog.setLogType("HTTP_RESPONSE");
	    responseLog.setLogLevel("INFO");
	    responseLog.setTimestamp(Instant.now());
	    responseLog.setCorrelationId(correlationId);
	    responseLog.setStatusCode(status);
	    responseLog.setResponseTimeMs(duration);

	    Object actualBody;

	    if (body instanceof ResponseEntity<?> responseEntity) {
	        Object responseEntityBody = responseEntity.getBody();
	        if (responseEntityBody instanceof ApiResponse<?> apiResp) {
	            actualBody = processApiResponseBody(apiResp);
	        } else {
	            actualBody = responseEntityBody != null ? responseEntityBody : "null";
	        }
	    } else if (body instanceof ApiResponse<?> apiResp) {
	        actualBody = processApiResponseBody(apiResp);
	    } else {
	        actualBody = body != null ? body : "null";
	    }

	    responseLog.setResponseBody(actualBody);
	    logAsString(responseLog);
	}
	
	
	public <T> ApiResponseLog<Object> processApiResponseBody(ApiResponse<T> response) throws IllegalArgumentException, IllegalAccessException {

	    if (response == null) {
	        return ApiResponseLog.builder()
	                .body(null)
	                .errorMessage(null)
	                .source(null)
	                .rows(0)
	                .build();
	    }

	    Object maskedBody;
	    T body = response.getBody();

	    if (body == null) {
	        maskedBody = null;

	    } else if (body instanceof java.util.List<?> list) {

	        if (list.isEmpty()) {
	            maskedBody = java.util.Collections.emptyList();
	        } else {
	        	String typeName = list.get(0).getClass().getSimpleName();
	            maskedBody = "List<" + typeName + ">";
	        }

	    } else {
	        // Single DTO masking
	        maskedBody = maskingUtil.maskObject(body);
	    }

	    return ApiResponseLog.builder()
	            .body(maskedBody)
	            .errorMessage(response.getErrorMessage())
	            .source(response.getSource())
	            .rows(response.getRows())
	            .build();
	}

	

	public void methodEntryLog(String type, String method, String message, String correlationId) {
		MethodLog methodEntryLog = new MethodLog();
		methodEntryLog.setLogType("METHOD_ENTRY");
		methodEntryLog.setLogLevel("INFO");
		methodEntryLog.setTimestamp(Instant.now());
		methodEntryLog.setCorrelationId(correlationId);
		methodEntryLog.setMethod(method);
		methodEntryLog.setMessage(message);
		logAsString(methodEntryLog);
	}

	public void methodExitLog(String type, String method, String message, String correlationId) {
		MethodLog methodExitLog = new MethodLog();
		methodExitLog.setLogType("METHOD_EXIT");
		methodExitLog.setLogLevel("INFO");
		methodExitLog.setTimestamp(Instant.now());
		methodExitLog.setCorrelationId(correlationId);
		methodExitLog.setMethod(method);
		methodExitLog.setMessage(message);
		logAsString(methodExitLog);
	}

	private void logAsString(Object logObject) {
	    try {
	        String json = objectMapper.writeValueAsString(logObject);
	        log.info("Log object: {}", json);
	    } catch (JsonProcessingException e) {
	        log.error("Failed to convert logObject to JSON", e);
	    }
	}


	public void exceptionHandlerLog(Exception ex, Object body, String correlationId, int statusCode) {
		ExceptionHandlerLog exceptionHandlerLog = new ExceptionHandlerLog();
		exceptionHandlerLog.setLogType("EXCEPTION");
		exceptionHandlerLog.setLogLevel("ERROR");
		exceptionHandlerLog.setTimestamp(Instant.now());
		exceptionHandlerLog.setCorrelationId(correlationId);
		exceptionHandlerLog.setStatusCode(statusCode);
		exceptionHandlerLog.setError(ex.getClass().getSimpleName());
		exceptionHandlerLog.setMessage(ex.getMessage());
		exceptionHandlerLog.setResponseBody(body);
		String logAsString = null;
		try {
			logAsString = objectMapper.writeValueAsString(exceptionHandlerLog);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		log.error("Log object: {}", logAsString);
	}
	
	public void codeExecuteLog(String type, String method, String message, String correlationId) {
		CodeLog code_Log = new CodeLog();
		code_Log.setLogType(type);
		code_Log.setLogLevel("INFO");
		code_Log.setTimestamp(Instant.now());
		code_Log.setCorrelationId(correlationId);
		code_Log.setMethod(method);
		code_Log.setMessage(maskingUtil.maskJsonString(message));
		logAsString(code_Log);
	}
	


}
