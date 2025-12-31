package com.ads.adshub.utill;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.GetMappingRequest;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
//import org.elasticsearch.*;


/**
 * Utility to resolve Elasticsearch fields and types.
 */

@Component
public class ElasticFieldUtil {

    private final ElasticsearchClient elasticClient;
    private final Map<String, Map<String, String>> indexFieldTypeCache = new HashMap<>();
    private static final Set<String> keywordRequiredOps = Set.of(
            "equals", "notEquals", "contains", "startsWith", "endsWith", "sort", "equalsDate"
    );

    public ElasticFieldUtil(ElasticsearchClient elasticClient) {
    	 if (elasticClient == null) {
             throw new IllegalArgumentException("ElasticsearchClient cannot be null");
         }
         this.elasticClient = elasticClient;
    }

    /**
     * Lazily load mapping for an index.
     */
    public void loadMapping(String indexName) {
        try {
            GetMappingResponse resp = elasticClient.indices()
                    .getMapping(GetMappingRequest.of(g -> g.index(indexName)));

            // Get the IndexMappingRecord for the index
            Map<String, IndexMappingRecord> mappingsMap = resp.mappings();
            IndexMappingRecord indexMapping = mappingsMap.get(indexName);

            if (indexMapping == null || indexMapping.mappings() == null) {
                throw new RuntimeException("No mappings for index: " + indexName);
            }

            TypeMapping tm = indexMapping.mappings();
            Map<String, Property> props = tm.properties();
            if (props == null) {
                throw new RuntimeException("No properties found for index: " + indexName);
            }

            Map<String, String> fieldTypeMap = new HashMap<>();
            for (Map.Entry<String, Property> entry : props.entrySet()) {
                String field = entry.getKey();
                Property prop = entry.getValue();
                String type = prop._kind().jsonValue();

                // mark text+keyword fields
                if ("text".equals(type) && prop.text() != null && prop.text().fields() != null
                        && prop.text().fields().containsKey("keyword")) {
                    fieldTypeMap.put(field, "text+keyword");
                } else {
                    fieldTypeMap.put(field, type);
                }
            }

            indexFieldTypeCache.put(indexName, fieldTypeMap);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load mapping for index: " + indexName, e);
        }
    }

    /**
     * Resolve field name to use keyword subfield if needed
     */
    public String resolveField(String indexName, String field, String operation) {
        Map<String, String> fieldMap = indexFieldTypeCache.get(indexName);
        if (fieldMap == null) {
            // Lazy load if not yet loaded
            loadMapping(indexName);
            fieldMap = indexFieldTypeCache.get(indexName);
        }

        String type = fieldMap.get(field);
        if ("text+keyword".equals(type) && keywordRequiredOps.contains(operation)) {
            return field + ".keyword";
        }
        return field;
    }

}
