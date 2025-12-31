package com.ads.adshub.entity.document;

import lombok.AllArgsConstructor;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Builder                // ✅ REQUIRED
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "districts")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DistrictDocument {

    @Id
    private String districtId;     // ✅ MUST be named id

    private String districtName;
    private String districtCode;
    private String stateId;
}
