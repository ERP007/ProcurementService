package org.fallguys.procurementservice.adapter.inbound.web.dto;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PurchaseOrderSummaryResponse(
        String code,
        String vendorCode,
        String vendorName,
        Instant createdAt,
        LocalDate desiredArrivalDate,
        int lineCount,
        Integer totalQuantity,
        String unit,
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
                summary.desiredArrivalDate(),
                summary.lineCount(),
                summary.totalQuantity(),
                summary.unit(),
                summary.totalAmount().amount(),
                "KRW",
                summary.status()
        );
    }
}
