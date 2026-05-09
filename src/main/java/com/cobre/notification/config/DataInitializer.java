package com.cobre.notification.config;

import com.cobre.notification.adapter.out.persistence.NotificationEventJpaEntity;
import com.cobre.notification.adapter.out.persistence.NotificationEventJpaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String SEED_FILE = "data/notification_events.json";

    private final NotificationEventJpaRepository repository;
    private final ObjectMapper objectMapper;

    public DataInitializer(NotificationEventJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (repository.count() > 0) {
            log.info("Notification events already present — skipping seed load");
            return;
        }
        ClassPathResource resource = new ClassPathResource(SEED_FILE);
        try (InputStream is = resource.getInputStream()) {
            List<NotificationEventJpaEntity> events =
                    objectMapper.readValue(is, new TypeReference<>() {});
            repository.saveAll(events);
            log.info("Seeded {} notification events from {}", events.size(), SEED_FILE);
        }
    }
}
