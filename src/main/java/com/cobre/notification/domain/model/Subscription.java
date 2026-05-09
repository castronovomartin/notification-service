package com.cobre.notification.domain.model;

import java.time.Instant;
import java.util.Set;

public class Subscription {

    private final String subscriptionId;
    private final String clientId;
    private final String webhookUrl;
    private final Set<String> eventTypes;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    public Subscription(
            String subscriptionId,
            String clientId,
            String webhookUrl,
            Set<String> eventTypes,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
        if (webhookUrl == null || !webhookUrl.startsWith("https://")) {
            throw new IllegalArgumentException(
                    "webhookUrl must use HTTPS scheme, got: " + webhookUrl);
        }
        this.subscriptionId = subscriptionId;
        this.clientId = clientId;
        this.webhookUrl = webhookUrl;
        this.eventTypes = Set.copyOf(eventTypes);
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // --- Behaviour --------------------------------------------------------

    public boolean accepts(String eventType) {
        return eventTypes.isEmpty() || eventTypes.contains(eventType);
    }

    // --- Getters ----------------------------------------------------------

    public String getSubscriptionId()    { return subscriptionId; }
    public String getClientId()          { return clientId; }
    public String getWebhookUrl()        { return webhookUrl; }
    public Set<String> getEventTypes()   { return eventTypes; }
    public boolean isActive()            { return active; }
    public Instant getCreatedAt()        { return createdAt; }
    public Instant getUpdatedAt()        { return updatedAt; }
}
