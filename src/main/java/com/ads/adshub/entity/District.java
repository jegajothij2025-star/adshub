package com.ads.adshub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "district")
public class District {


@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "district_id")
private Long districtId;

@NotBlank
@Column(name = "district_name", nullable = false)
private String districtName;

@NotBlank
@Column(name = "district_code", nullable = false, unique = true)
private String districtCode;

@NotBlank
@Column(name = "state_id", nullable = false, unique = true)
private String stateId;

/*
 * public Long getDistrictId() { return districtId; }
 * 
 * public void setDistrictId(Long districtId) { this.districtId = districtId; }
 * 
 * public String getDistrictName() { return districtName; }
 * 
 * public void setDistrictName(String districtName) { this.districtName =
 * districtName; }
 * 
 * public String getDistrictCode() { return districtCode; }
 * 
 * public void setDistrictCode(String districtCode) { this.districtCode =
 * districtCode; }
 * 
 * public String getStateId() { return stateId; }
 * 
 * public void setStateId(String stateId) { this.stateId = stateId; }
 */


}
