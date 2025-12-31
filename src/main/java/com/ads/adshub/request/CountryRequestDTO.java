package com.ads.adshub.request;

public class CountryRequestDTO {
	
	private String countryName;
	private String countryCode;
	public String getCountryName() {
		return countryName;
	}
	public String getCountryCode() {
		return countryCode;
	}
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}
	
	public void setCountryName(String countryName) {
		this.countryName = countryName;
	}
	
}
