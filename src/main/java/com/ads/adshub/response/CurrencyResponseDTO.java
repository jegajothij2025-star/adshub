package com.ads.adshub.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder                // ✅ ADD THIS
@NoArgsConstructor
@AllArgsConstructor

public class CurrencyResponseDTO {

	private Long currencyId;
	
	private String currencyName;
    private String currencyCode;
	
	private Long countryId;
	private String countryName;
	private String countryCode;

}