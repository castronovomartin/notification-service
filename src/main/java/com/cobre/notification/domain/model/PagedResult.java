package com.cobre.notification.domain.model;

import java.util.List;

public record PagedResult<T>(List<T> content, long totalElements, int page, int size) {

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }
}
