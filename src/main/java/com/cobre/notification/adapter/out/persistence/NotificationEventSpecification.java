package com.cobre.notification.adapter.out.persistence;

import com.cobre.notification.domain.model.DeliveryStatus;
import com.cobre.notification.domain.model.NotificationEventFilter;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class NotificationEventSpecification {

    private NotificationEventSpecification() {}

    public static Specification<NotificationEventJpaEntity> withClientId(String clientId) {
        return (root, query, cb) ->
                clientId == null ? null : cb.equal(root.get("clientId"), clientId);
    }

    public static Specification<NotificationEventJpaEntity> withStatus(DeliveryStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<NotificationEventJpaEntity> withDeliveryDateFrom(Instant from) {
        return (root, query, cb) ->
                from == null ? null : cb.greaterThanOrEqualTo(root.get("deliveryDate"), from);
    }

    public static Specification<NotificationEventJpaEntity> withDeliveryDateTo(Instant to) {
        return (root, query, cb) ->
                to == null ? null : cb.lessThanOrEqualTo(root.get("deliveryDate"), to);
    }

    public static Specification<NotificationEventJpaEntity> fromFilter(NotificationEventFilter filter) {
        return Specification
                .where(withClientId(filter.clientId()))
                .and(withStatus(filter.status()))
                .and(withDeliveryDateFrom(filter.from()))
                .and(withDeliveryDateTo(filter.to()));
    }
}
