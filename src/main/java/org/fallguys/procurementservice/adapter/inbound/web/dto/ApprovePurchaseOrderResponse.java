package org.fallguys.procurementservice.adapter.inbound.web.dto;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ApprovePurchaseOrderResponse(
        String code,
        String vendorCode,
        String warehouseCode,
        LocalDate desiredArrivalDate,
        String status,
        BigDecimal totalAmount,
        String currency,
        Instant approvedAt
) {
    public static ApprovePurchaseOrderResponse from(PurchaseOrder po) {
        return new ApprovePurchaseOrderResponse(
                po.getCode(),
                po.getVendorCode(),
                po.getWarehouseCode(),
                po.getDesiredArrivalDate(),
                po.getStatus().name(),
                po.getTotalAmount().amount(),
                "KRW",
                po.getApproval().approvedAt()
        );
    }
}
