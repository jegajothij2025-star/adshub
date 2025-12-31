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
@Document(indexName = "deviceos")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceOsDocument {

    @Id
    private String deviceosId;     // ✅ MUST be named id

    private String deviceos;
    private String deviceosCode;
}
