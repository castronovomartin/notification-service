package com.cobre.notification.adapter.out.webhook;

import com.cobre.notification.domain.exception.NonRetryableDeliveryException;
import com.cobre.notification.domain.model.DeliveryResult;
import com.cobre.notification.domain.model.DeliveryStatus;
import com.cobre.notification.domain.model.NotificationEvent;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookDeliveryAdapterIT {

    private static final String WEBHOOK_PATH = "/webhook";
    private static final String CLIENT_ID    = "CLIENT001";
    private static final String EVENT_ID     = "EVT001";
    private static final Instant NOW         = Instant.parse("2024-03-15T09:30:22Z");

    // Short timeout so the timeout scenario completes quickly in tests
    private static final int TEST_RESPONSE_TIMEOUT_MS = 500;

    private WireMockServer wireMock;
    private WebhookDeliveryAdapter adapter;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(TEST_RESPONSE_TIMEOUT_MS));

        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Cobre-Source", "notification-service")
                .defaultHeader("X-Cobre-Version", "1.0")
                .build();

        adapter = new WebhookDeliveryAdapter(webClient);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    private String webhookUrl() {
        return "http://localhost:" + wireMock.port() + WEBHOOK_PATH;
    }

    private NotificationEvent event() {
        return new NotificationEvent(
                EVENT_ID, "credit_card_payment", "Payment of $150.00",
                NOW, DeliveryStatus.PENDING, CLIENT_ID, 0, null, NOW, NOW);
    }

    // -----------------------------------------------------------------------
    // Scenario 1 — Success on first attempt
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Scenario 1 — success on first attempt: DeliveryResult.success is true, status 200")
    void scenario1_successOnFirstAttempt() {
        wireMock.stubFor(post(urlEqualTo(WEBHOOK_PATH))
                .willReturn(aResponse().withStatus(200)));

        DeliveryResult result = adapter.deliver(event(), webhookUrl());

        assertThat(result.success()).isTrue();
        assertThat(result.httpStatusCode()).isEqualTo(200);
        assertThat(result.errorMessage()).isNull();
    }

    // -----------------------------------------------------------------------
    // Scenario 2 — Success on third attempt
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Scenario 2 — success on 3rd attempt: first two return 500, third returns 200")
    void scenario2_successOnThirdAttempt() {
        wireMock.stubFor(post(urlEqualTo(WEBHOOK_PATH))
                .inScenario("retry").whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second"));

        wireMock.stubFor(post(urlEqualTo(WEBHOOK_PATH))
                .inScenario("retry").whenScenarioStateIs("second")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("third"));

        wireMock.stubFor(post(urlEqualTo(WEBHOOK_PATH))
                .inScenario("retry").whenScenarioStateIs("third")
                .willReturn(aResponse().withStatus(200)));

        // Simulate three Kafka consumer invocations (one per Kafka message)
        DeliveryResult r1 = adapter.deliver(event(), webhookUrl());
        DeliveryResult r2 = adapter.deliver(event(), webhookUrl());
        DeliveryResult r3 = adapter.deliver(event(), webhookUrl());

        assertThat(r1.success()).isFalse();
        assertThat(r1.httpStatusCode()).isEqualTo(500);
        assertThat(r2.success()).isFalse();
        assertThat(r2.httpStatusCode()).isEqualTo(500);
        assertThat(r3.success()).isTrue();
        assertThat(r3.httpStatusCode()).isEqualTo(200);
    }

    // -----------------------------------------------------------------------
    // Scenario 3 — All retries exhausted
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Scenario 3 — all retries exhausted: 5 consecutive attempts all return failure")
    void scenario3_allRetriesExhausted() {
        wireMock.stubFor(post(urlEqualTo(WEBHOOK_PATH))
                .willReturn(aResponse().withStatus(500)));

        // Simulate five Kafka consumer invocations
        for (int attempt = 1; attempt <= 5; attempt++) {
            DeliveryResult result = adapter.deliver(event(), webhookUrl());
            assertThat(result.success())
                    .as("attempt %d should fail", attempt)
                    .isFalse();
            assertThat(result.httpStatusCode()).isEqualTo(500);
        }
    }

    // -----------------------------------------------------------------------
    // Scenario 4 — Connection timeout
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Scenario 4 — connection timeout: exception propagates so consumer can reschedule")
    void scenario4_connectionTimeout() {
        int delayMs = TEST_RESPONSE_TIMEOUT_MS + 200; // exceeds the 500ms timeout
        wireMock.stubFor(post(urlEqualTo(WEBHOOK_PATH))
                .willReturn(aResponse().withStatus(200).withFixedDelay(delayMs)));

        assertThatThrownBy(() -> adapter.deliver(event(), webhookUrl()))
                .isInstanceOf(RuntimeException.class);
    }

    // -----------------------------------------------------------------------
    // Scenario 5 — Webhook URL unreachable
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Scenario 5 — unreachable server: connection fault propagates as runtime exception")
    void scenario5_unreachableServer() {
        wireMock.stubFor(post(urlEqualTo(WEBHOOK_PATH))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> adapter.deliver(event(), webhookUrl()))
                .isInstanceOf(RuntimeException.class);
    }

    // -----------------------------------------------------------------------
    // Scenario 6 — HTTP 400 non-retryable
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Scenario 6 — HTTP 400 non-retryable: NonRetryableDeliveryException thrown immediately")
    void scenario6_http400NonRetryable() {
        wireMock.stubFor(post(urlEqualTo(WEBHOOK_PATH))
                .willReturn(aResponse().withStatus(400)));

        assertThatThrownBy(() -> adapter.deliver(event(), webhookUrl()))
                .isInstanceOf(NonRetryableDeliveryException.class)
                .hasMessageContaining(EVENT_ID)
                .hasMessageContaining("400");
    }
}
