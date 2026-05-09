package com.cobre.notification.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationEventJpaRepository
        extends JpaRepository<NotificationEventJpaEntity, String>,
                JpaSpecificationExecutor<NotificationEventJpaEntity> {
}
