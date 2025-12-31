package com.ads.adshub.log;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseLog {
	private String logType;
    private String logLevel;
    private Instant timestamp;
    private String correlationId;
}
