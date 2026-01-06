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
@Document(indexName = "professions")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfessionDocument {

    @Id
    private String professionId;     // ✅ MUST be named id

    private String professionName;
}
