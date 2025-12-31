package com.ads.adshub.log;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RequestLog extends BaseLog {

	private String httpMethod;
	private String url;
	private Map<String, String> requestHeaders;
	@JsonIgnore
	private Object requestBody;

	private Long appId;
	private Integer userId;
	private String ipAddress;
	private Double longitude;
	private Double latitude;
}

