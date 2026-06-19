package org.fallguys.procurementservice.adapter.inbound.web.dto;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderKpi;

public record PurchaseOrderKpiResponse(
        long totalCount,
        long draftCount,
        long approvedCount,
        long delayedCount
) {
    public static PurchaseOrderKpiResponse from(PurchaseOrderKpi kpi) {
        return new PurchaseOrderKpiResponse(
                kpi.totalCount(),
                kpi.draftCount(),
                kpi.approvedCount(),
                kpi.delayedCount()
        );
    }
}
