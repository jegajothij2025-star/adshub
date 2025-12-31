package com.ads.adshub.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder                // ✅ ADD THIS
@NoArgsConstructor
@AllArgsConstructor

public class EduQualResponseDTO {

	private Long eduqualId;
	private String eduqualName;
	private String eduqualCode;

}