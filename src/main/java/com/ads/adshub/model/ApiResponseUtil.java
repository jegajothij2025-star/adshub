package com.ads.adshub.model;

public class ApiResponseUtil {

    /* ================= SUCCESS ================= */

    public static <T> ApiResponse<T> success(
            T body,
            String message,
            String errorMessage,
            String source,
            long rows,
            String correlationId
    ) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setBody(body);
        response.setMessage(message);
        response.setErrorMessage(errorMessage);
        response.setSource(source);
        response.setRows(rows);
        response.setCorrelationId(correlationId);
        return response;
    }

    /* ================= FAILURE (SIMPLE) ================= */

    public static <T> ApiResponse<T> failure(
            String errorMessage,
            String correlationId
    ) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setBody(null);
        response.setMessage("Request failed");
        response.setErrorMessage(errorMessage);
        response.setSource("API");
        response.setRows(0);
        response.setCorrelationId(correlationId);
        return response;
    }

    /* ================= FAILURE (FULL CONTROL) ================= */

    public static <T> ApiResponse<T> buildResponse(
            T body,
            int status,
            String message,
            String errorMessage,
            String source,
            long rows,
            String correlationId
    ) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(status >= 200 && status < 300);
        response.setBody(body);
        response.setMessage(message);
        response.setErrorMessage(errorMessage);
        response.setSource(source);
        response.setRows(rows);
        response.setCorrelationId(correlationId);
        return response;
    }
}
