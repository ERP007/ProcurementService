package org.fallguys.procurementservice.domain.model;

public record PurchaseOrderKpi(
        long totalCount,
        long draftCount,
        long approvedCount,
        long delayedCount
) {}
