package com.cobre.notification.domain.port.out;

import com.cobre.notification.domain.model.NotificationEvent;
import com.cobre.notification.domain.model.NotificationEventFilter;
import com.cobre.notification.domain.model.PageRequest;
import com.cobre.notification.domain.model.PagedResult;
import java.util.Optional;

public interface NotificationEventRepository {

    Optional<NotificationEvent> findById(String eventId);

    PagedResult<NotificationEvent> findAll(NotificationEventFilter filter, PageRequest pageRequest);

    NotificationEvent save(NotificationEvent event);
}
