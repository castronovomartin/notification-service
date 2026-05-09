package com.cobre.notification.domain.port.in;

import com.cobre.notification.domain.model.NotificationEvent;
import com.cobre.notification.domain.model.NotificationEventFilter;
import com.cobre.notification.domain.model.PageRequest;
import com.cobre.notification.domain.model.PagedResult;

public interface NotificationEventUseCase {

    NotificationEvent findById(String eventId, String clientId);

    PagedResult<NotificationEvent> findAll(NotificationEventFilter filter, PageRequest pageRequest);

    void replay(String eventId, String clientId);
}
