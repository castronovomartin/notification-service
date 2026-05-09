package com.cobre.notification.domain.port.out;

import com.cobre.notification.domain.model.Subscription;
import java.util.Optional;

public interface SubscriptionPort {

    Optional<Subscription> findActiveByClientId(String clientId);
}
