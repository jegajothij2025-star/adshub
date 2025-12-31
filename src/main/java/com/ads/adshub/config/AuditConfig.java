package com.ads.adshub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

@Configuration
public class AuditConfig {

	@Bean
    public AuditorAware<String> auditorProvider() {
        // In real app, fetch from SecurityContextHolder or logged-in user
        return () -> Optional.of("SYSTEM");
    }
}
