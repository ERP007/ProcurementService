package org.fallguys.procurementservice.adapter.inbound.web.dto;

import org.fallguys.procurementservice.domain.model.PurchaseOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CreatePurchaseOrderResponse(
        String code,
        String vendorCode,
        String warehouseCode,
        LocalDate desiredArrivalDate,
        String status,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt
) {
    public static CreatePurchaseOrderResponse from(PurchaseOrder po) {
        BigDecimal totalAmount = po.getLines().stream()
                .map(line -> line.getLineAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CreatePurchaseOrderResponse(
                po.getCode(),
                po.getVendorCode(),
                po.getWarehouseCode(),
                po.getDesiredArrivalDate(),
                po.getStatus().name(),
                totalAmount,
                "KRW",
                po.getCreation().createdAt()
        );
    }
}
