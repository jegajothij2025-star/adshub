package com.ads.adshub.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder                // ✅ ADD THIS
@NoArgsConstructor
@AllArgsConstructor
public class GenderResponseDTO {


private Long genderId;
private String gender;
private String genderCode;


//public Long getGenderId() { return genderId; }
//public void setGenderId(Long genderId) { this.genderId = genderId; }
//public String getGender() { return gender; }
//public void setGender(String gender) { this.gender = gender; }
//public String getGenderCode() { return genderCode; }
//public void setGenderCode(String genderCode) { this.genderCode = genderCode; }
}
