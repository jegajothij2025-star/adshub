package com.ads.adshub.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder                // ✅ ADD THIS
@NoArgsConstructor
@AllArgsConstructor

public class DeviceOsResponseDTO {

	private Long deviceosId;
	private String deviceos;
	private String deviceosCode;

}