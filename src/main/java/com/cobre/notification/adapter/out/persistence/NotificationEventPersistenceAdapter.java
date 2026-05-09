package com.cobre.notification.adapter.out.persistence;

import com.cobre.notification.domain.model.NotificationEvent;
import com.cobre.notification.domain.model.NotificationEventFilter;
import com.cobre.notification.domain.model.PageRequest;
import com.cobre.notification.domain.model.PagedResult;
import com.cobre.notification.domain.port.out.NotificationEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class NotificationEventPersistenceAdapter implements NotificationEventRepository {

    private final NotificationEventJpaRepository jpaRepository;
    private final NotificationEventJpaMapper mapper;

    public NotificationEventPersistenceAdapter(
            NotificationEventJpaRepository jpaRepository,
            NotificationEventJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<NotificationEvent> findById(String eventId) {
        return jpaRepository.findById(eventId).map(mapper::toDomain);
    }

    @Override
    public PagedResult<NotificationEvent> findAll(NotificationEventFilter filter, PageRequest pageRequest) {
        Specification<NotificationEventJpaEntity> spec = NotificationEventSpecification.fromFilter(filter);

        // Disambiguate from domain PageRequest
        org.springframework.data.domain.PageRequest springPage =
                org.springframework.data.domain.PageRequest.of(
                        pageRequest.page(),
                        pageRequest.size(),
                        Sort.by(Sort.Direction.DESC, "deliveryDate"));

        Page<NotificationEventJpaEntity> page = jpaRepository.findAll(spec, springPage);

        List<NotificationEvent> content = page.getContent().stream()
                .map(mapper::toDomain)
                .toList();

        return new PagedResult<>(content, page.getTotalElements(), page.getNumber(), page.getSize());
    }

    @Override
    public NotificationEvent save(NotificationEvent event) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(event)));
    }
}
