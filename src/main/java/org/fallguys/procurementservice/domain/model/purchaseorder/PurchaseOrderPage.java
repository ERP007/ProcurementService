package org.fallguys.procurementservice.domain.model.purchaseorder;

import java.util.List;

public record PurchaseOrderPage(
        List<PurchaseOrderSummary> content,
        int page,
        int size,
        long totalElements,
        long totalPages
) {
    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < totalPages();
    }
}
