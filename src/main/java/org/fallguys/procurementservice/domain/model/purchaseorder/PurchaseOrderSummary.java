package org.fallguys.procurementservice.domain.model.purchaseorder;

import org.fallguys.procurementservice.domain.model.Money;

import java.time.Instant;

public record PurchaseOrderSummary(
        String code,
        String vendorCode,
        String vendorName,
        Instant createdAt,
        int lineCount,
        Integer totalQuantity,
        String unit,
        Money totalAmount,
        PurchaseOrderStatus status
) {}
