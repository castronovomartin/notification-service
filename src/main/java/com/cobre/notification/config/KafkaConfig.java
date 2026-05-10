package com.cobre.notification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic pendingTopic() {
        return TopicBuilder.name("notifications.pending")
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic retryTopic() {
        return TopicBuilder.name("notifications.retry")
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name("notifications.dlq")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
