package com.cobre.notification.adapter.in.rest;

import com.cobre.notification.adapter.in.rest.mapper.NotificationEventRestMapper;
import com.cobre.notification.config.SecurityConfig;
import com.cobre.notification.domain.exception.EventNotFoundException;
import com.cobre.notification.domain.exception.ReplayNotAllowedException;
import com.cobre.notification.domain.exception.UnauthorizedAccessException;
import com.cobre.notification.domain.model.DeliveryStatus;
import com.cobre.notification.domain.model.NotificationEvent;
import com.cobre.notification.domain.model.NotificationEventFilter;
import com.cobre.notification.domain.model.PageRequest;
import com.cobre.notification.domain.model.PagedResult;
import com.cobre.notification.domain.port.in.NotificationEventUseCase;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationEventController.class)
@Import({SecurityConfig.class, NotificationEventRestMapper.class})
class NotificationEventControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationEventUseCase useCase;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private RateLimiterRegistry rateLimiterRegistry;

    private RateLimiter mockRateLimiter;

    private static final String CLIENT1 = "CLIENT001";
    private static final String EVT_ID  = "EVT001";
    private static final Instant NOW    = Instant.parse("2024-03-15T09:30:22Z");

    private static final SimpleGrantedAuthority READ_SCOPE  =
            new SimpleGrantedAuthority("SCOPE_notification:read");
    private static final SimpleGrantedAuthority WRITE_SCOPE =
            new SimpleGrantedAuthority("SCOPE_notification:write");

    private NotificationEvent sampleEvent() {
        return new NotificationEvent(
                EVT_ID, "credit_card_payment", "Test content",
                NOW, DeliveryStatus.COMPLETED, CLIENT1, 0, NOW, NOW, NOW);
    }

    @BeforeEach
    void setUpRateLimiter() {
        mockRateLimiter = mock(RateLimiter.class);
        when(rateLimiterRegistry.rateLimiter(anyString())).thenReturn(mockRateLimiter);
        // Default: rate limiter permits — invoke the wrapped supplier
        doAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get())
                .when(mockRateLimiter).executeSupplier(any());
    }

    // -----------------------------------------------------------------------
    // GET /notification_events
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("GET /notification_events")
    class FindAll {

        @Test
        @DisplayName("returns 200 with paginated content for authenticated client")
        void happyPath() throws Exception {
            when(useCase.findAll(any(NotificationEventFilter.class), any(PageRequest.class)))
                    .thenReturn(new PagedResult<>(List.of(sampleEvent()), 1L, 0, 20));

            mockMvc.perform(get("/notification_events")
                            .with(jwt().jwt(j -> j.claim("clientId", CLIENT1)).authorities(READ_SCOPE)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].eventId").value(EVT_ID))
                    .andExpect(jsonPath("$.content[0].clientId").value(CLIENT1))
                    .andExpect(jsonPath("$.page.number").value(0))
                    .andExpect(jsonPath("$.page.totalElements").value(1))
                    .andExpect(jsonPath("$.page.totalPages").value(1));
        }

        @Test
        @DisplayName("returns 401 when no JWT is provided")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/notification_events"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 403 when JWT has insufficient scope")
        void wrongScope_returns403() throws Exception {
            mockMvc.perform(get("/notification_events")
                            .with(jwt().jwt(j -> j.claim("clientId", CLIENT1)).authorities(WRITE_SCOPE)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 400 when status param has an invalid enum value")
        void invalidStatus_returns400() throws Exception {
            mockMvc.perform(get("/notification_events")
                            .param("status", "INVALID_STATUS")
                            .with(jwt().jwt(j -> j.claim("clientId", CLIENT1)).authorities(READ_SCOPE)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when from is after to")
        void fromAfterTo_returns400() throws Exception {
            mockMvc.perform(get("/notification_events")
                            .param("from", "2024-03-15T12:00:00Z")
                            .param("to",   "2024-03-15T09:00:00Z")
                            .with(jwt().jwt(j -> j.claim("clientId", CLIENT1)).authorities(READ_SCOPE)))
                    .andExpect(status().isBadRequest());
        }
    }

    // -----------------------------------------------------------------------
    // GET /notification_events/{id}
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("GET /notification_events/{id}")
    class FindById {

        @Test
        @DisplayName("returns 200 with event details for the owning client")
        void happyPath() throws Exception {
            when(useCase.findById(EVT_ID, CLIENT1)).thenReturn(sampleEvent());

            mockMvc.perform(get("/notification_events/" + EVT_ID)
                            .with(jwt().jwt(j -> j.claim("clientId", CLIENT1)).authorities(READ_SCOPE)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventId").value(EVT_ID))
                    .andExpect(jsonPath("$.clientId").value(CLIENT1))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("returns 404 when event does not exist")
        void notFound_returns404() throws Exception {
            when(useCase.findById("UNKNOWN", CLIENT1))
                    .thenThrow(new EventNotFoundException("UNKNOWN"));

            mockMvc.perform(get("/notification_events/UNKNOWN")
                            .with(jwt().jwt(j -> j.claim("clientId", CLIENT1)).authorities(READ_SCOPE)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 403 when event belongs to a different client")
        void wrongClient_returns403() throws Exception {
            when(useCase.findById(EVT_ID, "CLIENT002"))
                    .thenThrow(new UnauthorizedAccessException(EVT_ID, "CLIENT002"));

            mockMvc.perform(get("/notification_events/" + EVT_ID)
                            .with(jwt().jwt(j -> j.claim("clientId", "CLIENT002")).authorities(READ_SCOPE)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 401 when no JWT is provided")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/notification_events/" + EVT_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -----------------------------------------------------------------------
    // POST /notification_events/{id}/replay
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /notification_events/{id}/replay")
    class Replay {

        @Test
        @DisplayName("returns 202 Accepted with replay body for a FAILED event")
        void happyPath_returns202() throws Exception {
            doNothing().when(useCase).replay(EVT_ID, CLIENT1);

            mockMvc.perform(post("/notification_events/" + EVT_ID + "/replay")
                            .with(jwt().jwt(j -> j.claim("clientId", CLIENT1)).authorities(WRITE_SCOPE)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.eventId").value(EVT_ID))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.message").value("Event re-queued for delivery successfully."));
        }

        @Test
        @DisplayName("returns 400 when replaying an event that is not in FAILED status")
        void nonFailedStatus_returns400() throws Exception {
            doThrow(new ReplayNotAllowedException(EVT_ID, DeliveryStatus.COMPLETED))
                    .when(useCase).replay(EVT_ID, CLIENT1);

            mockMvc.perform(post("/notification_events/" + EVT_ID + "/replay")
                            .with(jwt().jwt(j -> j.claim("clientId", CLIENT1)).authorities(WRITE_SCOPE)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 429 when rate limit is exceeded")
        void rateLimitExceeded_returns429() throws Exception {
            RateLimiter realLimiter = RateLimiter.of("test-429", RateLimiterConfig.ofDefaults());
            doThrow(RequestNotPermitted.createRequestNotPermitted(realLimiter))
                    .when(mockRateLimiter).executeSupplier(any());

            mockMvc.perform(post("/notification_events/" + EVT_ID + "/replay")
                            .with(jwt().jwt(j -> j.claim("clientId", CLIENT1)).authorities(WRITE_SCOPE)))
                    .andExpect(status().isTooManyRequests());
        }

        @Test
        @DisplayName("returns 401 when no JWT is provided")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/notification_events/" + EVT_ID + "/replay"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 403 when JWT has read scope but not write scope")
        void wrongScope_returns403() throws Exception {
            mockMvc.perform(post("/notification_events/" + EVT_ID + "/replay")
                            .with(jwt().jwt(j -> j.claim("clientId", CLIENT1)).authorities(READ_SCOPE)))
                    .andExpect(status().isForbidden());
        }
    }
}
