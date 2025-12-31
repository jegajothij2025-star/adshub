package com.ads.adshub.request;

public class StateRequestDTO {
	
	private String stateName;
	private String stateCode;
	private String countryCode;
	public String getStateName() {
		return stateName;
	}
	public String getCountryCode() {
		return countryCode;
	}
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}
	public void setStateCode(String stateCode) {
		this.stateCode = stateCode;
	}
	public void setStateName(String stateName) {
		this.stateName = stateName;
	}
	public String getStateCode() {
		return stateCode;
	}

}
