package com.ads.adshub.model;

import lombok.AllArgsConstructor;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T body;
    private String error;
    private String source;   // DB / ES
    private String correlationId;
    private String errorMessage;
    private long rows; 
}
