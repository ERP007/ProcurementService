package org.fallguys.procurementservice.domain.model;

import java.util.List;

public record PurchaseOrderPage(
        List<PurchaseOrderSummary> content,
        int page,
        int size,
        long totalElements
) {
    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < totalPages();
    }
}
