package org.fallguys.procurementservice.adapter.inbound.web.dto;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderSummary;

import java.math.BigDecimal;
import java.time.Instant;

public record PurchaseOrderSummaryResponse(
        String code,
        String vendorCode,
        String vendorName,
        Instant createdAt,
        int lineCount,
        BigDecimal totalAmount,
        String currency,
        PurchaseOrderStatus status
) {
    public static PurchaseOrderSummaryResponse from(PurchaseOrderSummary summary) {
        return new PurchaseOrderSummaryResponse(
                summary.code(),
                summary.vendorCode(),
                summary.vendorName(),
                summary.createdAt(),
                summary.lineCount(),
                summary.totalAmount().amount(),
                "KRW",
                summary.status()
        );
    }
}
