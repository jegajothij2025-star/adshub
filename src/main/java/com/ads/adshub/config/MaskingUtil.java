package com.ads.adshub.config;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.PostConstruct;

@Component
public class MaskingUtil {

	@Autowired
    private ObjectMapper objectMapper;
	
	private Set<String> globalMaskFields;
	
    @PostConstruct
    public void init() {
        // Scan your root package for all fields annotated with @Mask
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .forPackages("com.app")  // <-- your root package
                        .addScanners(Scanners.FieldsAnnotated)
        );

        Set<Field> fields = reflections.getFieldsAnnotatedWith(Mask.class);
        globalMaskFields = new HashSet<>();
        for (Field field : fields) {
            globalMaskFields.add(field.getName());
        }

        System.out.println("Fields to mask globally: " + globalMaskFields);
    }

    public Set<String> getGlobalMaskFields() {
        return globalMaskFields;
    }
	
	public Object maskObject(Object source) throws IllegalArgumentException, IllegalAccessException {
            JsonNode jsonNode = objectMapper.valueToTree(source);
            applyGlobalMask(source, jsonNode);
            return jsonNode;
    }
	
	private void applyGlobalMask(Object sourceObj, JsonNode targetNode) throws IllegalArgumentException, IllegalAccessException {
	    if (sourceObj == null || targetNode == null || !targetNode.isObject()) return;

	    Iterator<String> fieldNames = targetNode.fieldNames();
	    while (fieldNames.hasNext()) {
	        String jsonField = fieldNames.next();
	        JsonNode childNode = targetNode.get(jsonField);
	        Object value = null;

	        try {
	            Field field = null;
	            // attempt to find the field in sourceObj class if exists
	            field = sourceObj.getClass().getDeclaredField(jsonField);
	            field.setAccessible(true);
	            value = field.get(sourceObj);
	        } catch (NoSuchFieldException ignored) {}

	        // 1️ mask if this field name is globally annotated
	        if (globalMaskFields.contains(jsonField)) {
	            ((ObjectNode) targetNode).put(jsonField, maskValue(childNode.asText()));
	            continue;
	        }

	        // 2️ recurse nested object
	        if (childNode.isObject() && value != null && !isJavaType(value.getClass())) {
	            applyGlobalMask(value, childNode);
	        }

	        // 3️ collections
	        if (childNode.isArray() && value instanceof Collection<?> coll) {
	            int index = 0;
	            for (Object item : coll) {
	                JsonNode arrayNode = childNode.get(index++);
	                if (arrayNode != null && item != null && !isJavaType(item.getClass())) {
	                    applyGlobalMask(item, arrayNode);
	                }
	            }
	        }

	        // 4️ maps
	        if (childNode.isObject() && value instanceof Map<?, ?> map) {
	            for (Map.Entry<?, ?> entry : map.entrySet()) {
	                Object mapVal = entry.getValue();
	                JsonNode mapNode = childNode.get(String.valueOf(entry.getKey()));
	                if (mapNode != null && mapVal != null && !isJavaType(mapVal.getClass())) {
	                    applyGlobalMask(mapVal, mapNode);
	                }
	            }
	        }
	    }
	}

	public static String maskValue(String value) {
	    if (value == null || value.isBlank()) {
	        return value;
	    }
	    return value.replaceAll(".", "X");
	}
	
	private boolean isJavaType(Class<?> cls) {
        return cls.getPackageName().startsWith("java.");
    }
	
	public String maskJsonString(String json) {
        try {
            
            Set<String> maskFields = findMaskableFields(); // global scan

            for (String field : maskFields) {

                json = json.replaceAll(
                    "(\"" + field + "\"\\s*:\\s*)(\"[^\"]*\"|[^,}\\]]+)",
                    "$1\"XXXXXX\""
                );
            }

            return json;

        } catch (Exception e) {
            return "MASKING_ERROR_STRING: " + e.getMessage();
        }
    }
	
	private Set<String> findMaskableFields() {

	    Set<String> maskedFieldNames = new HashSet<>();

	    for (String field : globalMaskFields) {
	        maskedFieldNames.add(field);
	    }

	    return maskedFieldNames;
	}

}

