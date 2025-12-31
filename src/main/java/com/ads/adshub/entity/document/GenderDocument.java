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
@Document(indexName = "genders")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GenderDocument {

    @Id
    private String genderId;     // ✅ MUST be named id

    private String gender;
    private String genderCode;
}
