package com.ads.adshub.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CurrencyRequestDTO {
	
	private String currencyName;
	private String currencyCode;
	@NotNull
    @Positive
	private Long countryId;
	public String getCurrencyName() {
		return currencyName;
	}
	public void setCurrencyName(String currencyName) {
		this.currencyName = currencyName;
	}
	public String getCurrencyCode() {
		return currencyCode;
	}
	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}
	public Long getCountryId() {
		return countryId;
	}
	public void setCountryId(Long countryId) {
		this.countryId = countryId;
	}
	
	
	
}
