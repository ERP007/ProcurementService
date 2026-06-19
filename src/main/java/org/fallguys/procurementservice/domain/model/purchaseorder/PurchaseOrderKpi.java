package org.fallguys.procurementservice.domain.model.purchaseorder;

public record PurchaseOrderKpi(
        long totalCount,
        long draftCount,
        long approvedCount,
        long delayedCount
) {}
