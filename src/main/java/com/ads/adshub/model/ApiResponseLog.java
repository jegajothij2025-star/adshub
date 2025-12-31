package com.ads.adshub.model;


import java.io.Serializable;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiResponseLog<T> implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3094300382509401902L;
	private T body;
	private String message;
	private String errorMessage;
	private String source;
	private long rows;
}

