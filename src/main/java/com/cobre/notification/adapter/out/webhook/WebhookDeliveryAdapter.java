package com.cobre.notification.adapter.out.webhook;

import com.cobre.notification.domain.exception.NonRetryableDeliveryException;
import com.cobre.notification.domain.model.DeliveryResult;
import com.cobre.notification.domain.model.NotificationEvent;
import com.cobre.notification.domain.port.out.WebhookDeliveryPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Component
public class WebhookDeliveryAdapter implements WebhookDeliveryPort {

    private final WebClient webClient;

    public WebhookDeliveryAdapter(@Qualifier("webhookWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public DeliveryResult deliver(NotificationEvent event, String webhookUrl) {
        Instant start = Instant.now();

        WebhookPayload payload = new WebhookPayload(
                event.getEventId(),
                event.getEventType(),
                event.getContent(),
                event.getDeliveryDate(),
                event.getClientId(),
                event.getRetryCount(),
                start);

        ResponseEntity<Void> response = webClient.post()
                .uri(webhookUrl)
                .header("X-Cobre-Event-Id", event.getEventId())
                .header("X-Cobre-Client-Id", event.getClientId())
                .bodyValue(payload)
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is4xxClientError()) {
                        int code = resp.statusCode().value();
                        return resp.releaseBody().then(
                                Mono.error(new NonRetryableDeliveryException(event.getEventId(), code)));
                    }
                    return resp.toBodilessEntity();
                })
                .block();

        boolean success = response != null && response.getStatusCode().is2xxSuccessful();
        int statusCode  = response != null ? response.getStatusCode().value() : 0;
        String error    = success ? null : "HTTP " + statusCode;

        return new DeliveryResult(success, statusCode, error,
                Duration.between(start, Instant.now()), Instant.now());
    }
}
