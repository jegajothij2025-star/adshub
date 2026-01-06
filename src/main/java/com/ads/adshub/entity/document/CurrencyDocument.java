package com.ads.adshub.entity.document;

import lombok.AllArgsConstructor;


import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import com.ads.adshub.entity.Country;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Builder                // ✅ REQUIRED
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "currencies")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyDocument {

    @Id
    private String currencyId;     // ✅ MUST be named id

    private String currencyName;
    private String currencyCode;
    
 // 🔹 Flattened country data
    private Long countryId;
    private String countryName;
    private String countryCode;
}
