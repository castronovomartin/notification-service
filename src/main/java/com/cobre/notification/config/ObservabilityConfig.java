package com.cobre.notification.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Value("${spring.profiles.active:local}")
    private String environment;

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags("application", "notification-service", "environment", environment)
                .meterFilter(MeterFilter.deny(id ->
                        id.getName().startsWith("jvm.") && id.getName().contains("gc.overhead")));
    }
}
