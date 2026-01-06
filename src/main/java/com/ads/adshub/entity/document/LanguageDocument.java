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
@Document(indexName = "languages")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LanguageDocument {

    @Id
    private String languageId;     // ✅ MUST be named id

    private String languageName;
    private String languageCode;
}
