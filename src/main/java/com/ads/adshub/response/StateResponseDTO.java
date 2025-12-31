package com.ads.adshub.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder                // ✅ ADD THIS
@NoArgsConstructor
@AllArgsConstructor

public class StateResponseDTO {

	private Long stateId;
	private String stateName;
	private String stateCode;
	private String countryCode;

}

