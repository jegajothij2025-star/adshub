package com.ads.adshub.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder                // ✅ ADD THIS
@NoArgsConstructor
@AllArgsConstructor

public class DistrictResponseDTO {

	private Long districtId;
	private String districtName;
	private String districtCode;
	private String stateId;

}
