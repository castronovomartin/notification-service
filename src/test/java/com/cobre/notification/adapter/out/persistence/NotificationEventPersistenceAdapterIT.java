package com.cobre.notification.adapter.out.persistence;

import com.cobre.notification.config.DataInitializer;
import com.cobre.notification.domain.model.DeliveryStatus;
import com.cobre.notification.domain.model.NotificationEvent;
import com.cobre.notification.domain.model.NotificationEventFilter;
import com.cobre.notification.domain.model.PageRequest;
import com.cobre.notification.domain.model.PagedResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({NotificationEventPersistenceAdapter.class, NotificationEventJpaMapper.class})
class NotificationEventPersistenceAdapterIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine");

    // ObjectMapper shared across DataInitializer tests — mirrors Spring Boot's auto-config
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private NotificationEventPersistenceAdapter adapter;

    @Autowired
    private NotificationEventJpaRepository jpaRepository;

    // -----------------------------------------------------------------------
    // Fixtures
    // -----------------------------------------------------------------------

    private static final String C1   = "CLIENT001";
    private static final String C2   = "CLIENT002";
    private static final Instant BASE = Instant.parse("2024-01-01T00:00:00Z");

    private NotificationEvent event(String id, String clientId, DeliveryStatus status, Instant date) {
        return new NotificationEvent(
                id, "credit_card_payment", "Test event",
                date, status, clientId, 0, null, date, date);
    }

    // -----------------------------------------------------------------------
    // save + findById
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("save persists all fields; findById retrieves the exact same event")
    void saveAndFindById_roundTrip() {
        adapter.save(event("EVT-RT", C1, DeliveryStatus.PENDING, BASE));

        Optional<NotificationEvent> result = adapter.findById("EVT-RT");

        assertThat(result).isPresent();
        NotificationEvent found = result.get();
        assertThat(found.getEventId()).isEqualTo("EVT-RT");
        assertThat(found.getClientId()).isEqualTo(C1);
        assertThat(found.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(found.getDeliveryDate()).isEqualTo(BASE);
        assertThat(found.getRetryCount()).isEqualTo(0);
        assertThat(found.getLastAttemptAt()).isNull();
    }

    @Test
    @DisplayName("findById returns empty when event does not exist")
    void findById_returnsEmpty_whenNotFound() {
        assertThat(adapter.findById("DOES-NOT-EXIST")).isEmpty();
    }

    @Test
    @DisplayName("save on an existing ID performs an update")
    void save_updatesExistingEvent() {
        NotificationEvent evt = event("EVT-UPD", C1, DeliveryStatus.PENDING, BASE);
        adapter.save(evt);

        evt.markRetrying(BASE.plusSeconds(5));
        adapter.save(evt);

        Optional<NotificationEvent> updated = adapter.findById("EVT-UPD");
        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus()).isEqualTo(DeliveryStatus.RETRYING);
        assertThat(updated.get().getRetryCount()).isEqualTo(1);
        assertThat(updated.get().getLastAttemptAt()).isEqualTo(BASE.plusSeconds(5));
    }

    // -----------------------------------------------------------------------
    // findAll — filter by clientId
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("findAll — filter by clientId")
    class FilterByClientId {

        @Test
        @DisplayName("returns only events belonging to the specified clientId")
        void onlyMatchingClientReturned() {
            adapter.save(event("F-C1-1", C1, DeliveryStatus.COMPLETED, BASE));
            adapter.save(event("F-C1-2", C1, DeliveryStatus.FAILED,    BASE.plusSeconds(60)));
            adapter.save(event("F-C2-1", C2, DeliveryStatus.PENDING,   BASE.plusSeconds(120)));

            NotificationEventFilter filter = new NotificationEventFilter(C1, null, null, null);
            PagedResult<NotificationEvent> result = adapter.findAll(filter, new PageRequest(0, 10));

            assertThat(result.totalElements()).isEqualTo(2);
            assertThat(result.content()).allMatch(e -> e.getClientId().equals(C1));
        }
    }

    // -----------------------------------------------------------------------
    // findAll — filter by status
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("findAll — filter by status")
    class FilterByStatus {

        @Test
        @DisplayName("returns only events with the specified status")
        void onlyMatchingStatusReturned() {
            adapter.save(event("F-S-COMP-1", C1, DeliveryStatus.COMPLETED, BASE));
            adapter.save(event("F-S-FAIL-1", C1, DeliveryStatus.FAILED,    BASE.plusSeconds(60)));
            adapter.save(event("F-S-COMP-2", C1, DeliveryStatus.COMPLETED, BASE.plusSeconds(120)));

            NotificationEventFilter filter =
                    new NotificationEventFilter(C1, null, null, DeliveryStatus.COMPLETED);
            PagedResult<NotificationEvent> result = adapter.findAll(filter, new PageRequest(0, 10));

            assertThat(result.totalElements()).isEqualTo(2);
            assertThat(result.content()).allMatch(e -> e.getStatus() == DeliveryStatus.COMPLETED);
        }
    }

    // -----------------------------------------------------------------------
    // findAll — filter by date range
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("findAll — filter by date range")
    class FilterByDateRange {

        @Test
        @DisplayName("returns only events whose deliveryDate falls within [from, to]")
        void onlyEventsInsideRangeReturned() {
            Instant before = BASE;
            Instant inRange1 = BASE.plusSeconds(3_600);
            Instant inRange2 = BASE.plusSeconds(7_200);
            Instant after    = BASE.plusSeconds(14_400);

            adapter.save(event("F-D-BEFORE", C1, DeliveryStatus.PENDING, before));
            adapter.save(event("F-D-IN-1",   C1, DeliveryStatus.PENDING, inRange1));
            adapter.save(event("F-D-IN-2",   C1, DeliveryStatus.PENDING, inRange2));
            adapter.save(event("F-D-AFTER",  C1, DeliveryStatus.PENDING, after));

            Instant from = BASE.plusSeconds(1_800);
            Instant to   = BASE.plusSeconds(10_800);
            NotificationEventFilter filter = new NotificationEventFilter(C1, from, to, null);
            PagedResult<NotificationEvent> result = adapter.findAll(filter, new PageRequest(0, 10));

            assertThat(result.totalElements()).isEqualTo(2);
            assertThat(result.content()).allMatch(e ->
                    !e.getDeliveryDate().isBefore(from) && !e.getDeliveryDate().isAfter(to));
        }
    }

    // -----------------------------------------------------------------------
    // findAll — pagination + sort order
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("findAll — pagination and sort order")
    class PaginationAndSort {

        @Test
        @DisplayName("returns correct page size and total elements across multiple pages")
        void correctPageSizeAndTotalElements() {
            for (int i = 0; i < 7; i++) {
                adapter.save(event("PAGE-" + i, C1, DeliveryStatus.PENDING,
                        BASE.plusSeconds(i * 60L)));
            }

            NotificationEventFilter filter = new NotificationEventFilter(C1, null, null, null);
            PagedResult<NotificationEvent> p0 = adapter.findAll(filter, new PageRequest(0, 3));
            PagedResult<NotificationEvent> p1 = adapter.findAll(filter, new PageRequest(1, 3));
            PagedResult<NotificationEvent> p2 = adapter.findAll(filter, new PageRequest(2, 3));

            assertThat(p0.totalElements()).isEqualTo(7);
            assertThat(p0.totalPages()).isEqualTo(3);
            assertThat(p0.content()).hasSize(3);
            assertThat(p1.content()).hasSize(3);
            assertThat(p2.content()).hasSize(1);
        }

        @Test
        @DisplayName("results are sorted by deliveryDate descending (most recent first)")
        void resultsSortedByDeliveryDateDesc() {
            adapter.save(event("SORT-OLD",    C1, DeliveryStatus.PENDING, BASE));
            adapter.save(event("SORT-MIDDLE", C1, DeliveryStatus.PENDING, BASE.plusSeconds(3_600)));
            adapter.save(event("SORT-NEW",    C1, DeliveryStatus.PENDING, BASE.plusSeconds(7_200)));

            NotificationEventFilter filter = new NotificationEventFilter(C1, null, null, null);
            PagedResult<NotificationEvent> result = adapter.findAll(filter, new PageRequest(0, 10));

            assertThat(result.content().get(0).getEventId()).isEqualTo("SORT-NEW");
            assertThat(result.content().get(1).getEventId()).isEqualTo("SORT-MIDDLE");
            assertThat(result.content().get(2).getEventId()).isEqualTo("SORT-OLD");
        }
    }

    // -----------------------------------------------------------------------
    // DataInitializer
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("DataInitializer")
    class DataInitializerTests {

        @Test
        @DisplayName("loads all seed events from JSON when the database is empty")
        void loadsSeedDataOnEmptyDatabase() throws Exception {
            jpaRepository.deleteAll();

            new DataInitializer(jpaRepository, OBJECT_MAPPER).run(null);

            assertThat(jpaRepository.count()).isEqualTo(8);
        }

        @Test
        @DisplayName("skips seed load when the database already contains events")
        void skipsSeedLoadWhenDatabaseAlreadyHasData() throws Exception {
            adapter.save(event("ALREADY-EXISTS", C1, DeliveryStatus.PENDING, BASE));
            long countBefore = jpaRepository.count();

            new DataInitializer(jpaRepository, OBJECT_MAPPER).run(null);

            assertThat(jpaRepository.count()).isEqualTo(countBefore);
        }
    }
}
