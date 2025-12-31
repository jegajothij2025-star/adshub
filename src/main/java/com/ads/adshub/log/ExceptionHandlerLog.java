package com.ads.adshub.log;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class ExceptionHandlerLog extends BaseLog {

	private int statusCode;
    private String error;
    private String message;
    private Object responseBody;
}
