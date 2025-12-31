package com.ads.adshub.config;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        JavaTimeModule module = new JavaTimeModule();

        module.addDeserializer(OffsetDateTime.class,
                new JsonDeserializer<OffsetDateTime>() {
                    @Override
                    public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt)
                            throws IOException {
                        String value = p.getText();

                        // Normalize fractional seconds to max 9 digits
                        if (value.contains(".")) {
                            String[] parts = value.split("\\.");
                            String main = parts[0];
                            String rest = parts[1];

                            // Remove Z at end
                            boolean hasZ = rest.endsWith("Z");
                            if (hasZ) rest = rest.substring(0, rest.length() - 1);

                            // Pad to 9 digits
                            if (rest.length() < 9) {
                                rest = String.format("%-9s", rest).replace(' ', '0');
                            } else if (rest.length() > 9) {
                                rest = rest.substring(0, 9);
                            }

                            value = main + "." + rest + (value.endsWith("Z") ? "Z" : "");
                        }

                        return OffsetDateTime.parse(value);
                    }
                });

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}

