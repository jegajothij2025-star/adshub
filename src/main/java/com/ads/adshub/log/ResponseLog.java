package com.ads.adshub.log;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class ResponseLog extends BaseLog {
	private int statusCode;
    private long responseTimeMs;
    private Object responseBody;
}
