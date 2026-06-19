package org.fallguys.procurementservice.adapter.inbound.web.dto;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReceivePurchaseOrderResponse(
        String code,
        String vendorCode,
        String warehouseCode,
        LocalDate receivedDate,
        String status,
        BigDecimal totalAmount,
        String currency,
        Instant receivedAt
) {
    public static ReceivePurchaseOrderResponse from(PurchaseOrder order) {
        return new ReceivePurchaseOrderResponse(
                order.getCode(),
                order.getVendorCode(),
                order.getWarehouseCode(),
                order.getReceiving().receivedDate(),
                order.getStatus().name(),
                order.getTotalAmount().amount(),
                "KRW",
                order.getReceiving().receivedAt()
        );
    }
}
