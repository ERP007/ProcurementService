package org.fallguys.procurementservice.domain.model;

import java.time.Instant;
import java.time.LocalDate;

public record PurchaseOrderSummary(
        String code,
        String vendorCode,
        String vendorName,
        Instant createdAt,
        LocalDate desiredArrivalDate,
        int lineCount,
        Integer totalQuantity,
        String unit,
        Money totalAmount,
        PurchaseOrderStatus status
) {}
