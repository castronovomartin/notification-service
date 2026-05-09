package com.cobre.notification.adapter.in.rest.dto;

import java.util.List;

public record PagedNotificationEventResponse(
        List<NotificationEventResponse> content,
        PageMetadata page) {

    public record PageMetadata(
            int number,
            int size,
            long totalElements,
            int totalPages) {
    }
}
