package com.ads.adshub.util.exception;

import java.nio.file.AccessDeniedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ads.adshub.model.ApiResponse;
import com.ads.adshub.model.ApiResponseUtil;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAllUncaughtExceptions(Exception ex, HttpServletRequest request) {
    	String correlationId = (String) request.getAttribute("correlationId");
        logger.error("Unhandled exception occurred: ", ex);
        ApiResponse<Object> response = ApiResponseUtil.failure(ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(ConfigDataResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> resourceNotFoundException(ConfigDataResourceNotFoundException ex, HttpServletRequest request) {
    	String correlationId = (String) request.getAttribute("correlationId");
        logger.warn("Resource not found: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponseUtil.failure(ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler({
        ElasticsearchException.class,
        co.elastic.clients.transport.TransportException.class,
        IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleElasticsearchExceptions(Exception ex, HttpServletRequest request) {
        String correlationId = (String) request.getAttribute("correlationId");
        HttpStatus status;
        String reason;

        if (ex instanceof ElasticsearchException ese) {
            int statusCode = ese.status(); // primitive int
            status = HttpStatus.resolve(statusCode);
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

            if (status == HttpStatus.NOT_FOUND) {
                reason = "Elasticsearch index not found";
            } else {
                reason = ese.getMessage();
            }
        }else if (ex instanceof co.elastic.clients.transport.TransportException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            reason = "Elasticsearch cluster unavailable";
        } else if (ex instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
            reason = "Invalid Elasticsearch query parameter";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            reason = "Unexpected Elasticsearch error";
        }

        logger.error("Elasticsearch exception: {}", reason, ex);

        ApiResponse<Object> response = ApiResponseUtil.buildResponse(
                null,
                status.value(),
                reason,
                ex.getMessage(),
                "ES",
                0L,
                correlationId
        );

        return ResponseEntity.status(status).body(response);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
    	String correlationId = (String) request.getAttribute("correlationId");
        String errorMsg = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Invalid input data");

        ApiResponse<Object> response = ApiResponseUtil.failure(errorMsg, correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDatabaseErrors(DataIntegrityViolationException ex, HttpServletRequest request) {
    	String correlationId = (String) request.getAttribute("correlationId");
        logger.error("Database constraint violation: ", ex);
        ApiResponse<Object> response = ApiResponseUtil.failure("Database constraint violation", correlationId);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    	String correlationId = (String) request.getAttribute("correlationId");
        ApiResponse<Object> response = ApiResponseUtil.buildResponse(
                null,
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "HTTP method not supported",
                ex.getMessage(),
                "API",
                0L, correlationId
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
    	String correlationId = (String) request.getAttribute("correlationId");
        ApiResponse<Object> response = ApiResponseUtil.buildResponse(
                null,
                HttpStatus.BAD_REQUEST.value(),
                "Malformed JSON request",
                ex.getMessage(),
                "API",
                0L, correlationId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParams(MissingServletRequestParameterException ex, HttpServletRequest request) {
    	String correlationId = (String) request.getAttribute("correlationId");
        String errorMessage = "Missing required parameter: " + ex.getParameterName();
        ApiResponse<Object> response = ApiResponseUtil.buildResponse(
                null,
                HttpStatus.BAD_REQUEST.value(),
                errorMessage,
                ex.getMessage(),
                "API",
                0L, correlationId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
    	String correlationId = (String) request.getAttribute("correlationId");
        ApiResponse<Object> response = ApiResponseUtil.buildResponse(
                null,
                HttpStatus.FORBIDDEN.value(),
                "Access denied",
                ex.getMessage(),
                "SECURITY",
                0L, correlationId
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolations(ConstraintViolationException ex, HttpServletRequest request) {
    	String correlationId = (String) request.getAttribute("correlationId");
        String errorMessage = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .findFirst()
                .orElse("Validation failed");

        ApiResponse<Object> response = ApiResponseUtil.buildResponse(
                null,
                HttpStatus.BAD_REQUEST.value(),
                "Validation error",
                errorMessage,
                "API",
                0L, correlationId
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}

