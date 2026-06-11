package org.fallguys.procurementservice.adapter.inbound.web.dto;

import org.fallguys.procurementservice.domain.model.PurchaseOrder;

import java.time.Instant;

public record CancelPurchaseOrderResponse(
        String code,
        String status,
        String canceledBy,
        Instant canceledAt,
        String reason
) {
    public static CancelPurchaseOrderResponse from(PurchaseOrder po) {
        return new CancelPurchaseOrderResponse(
                po.getCode(),
                po.getStatus().name(),
                po.getCancellation().canceledBy(),
                po.getCancellation().canceledAt(),
                po.getCancellation().cancelReason()
        );
    }
}
