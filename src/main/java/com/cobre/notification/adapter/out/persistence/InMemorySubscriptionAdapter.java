package com.cobre.notification.adapter.out.persistence;

import com.cobre.notification.domain.model.Subscription;
import com.cobre.notification.domain.port.out.SubscriptionPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Placeholder adapter until the Subscription persistence layer is implemented.
 * Returns empty for all lookups, causing events to be marked SKIPPED by the domain service.
 */
@Component
class InMemorySubscriptionAdapter implements SubscriptionPort {

    @Override
    public Optional<Subscription> findActiveByClientId(String clientId) {
        return Optional.empty();
    }
}
