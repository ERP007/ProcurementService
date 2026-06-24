package org.fallguys.procurementservice.adapter.inbound.web.dto;

import org.fallguys.procurementservice.application.port.inbound.model.GetPurchaseOrderResult;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PurchaseOrderDetailResponse(
        String code,
        VendorInfo vendor,
        WarehouseInfo warehouse,
        PersonInfo approvedBy,
        Instant createdAt,
        String memo,
        String status,
        String progress,
        BigDecimal totalAmount,
        String currency,
        List<LineInfo> lines
) {
    public record VendorInfo(String code, String name) {}
    public record WarehouseInfo(String code, String name) {}
    public record LineInfo(Long id, String sku, String name, String unit, int quantity, BigDecimal unitPrice) {}

    public static PurchaseOrderDetailResponse from(GetPurchaseOrderResult result) {
        PurchaseOrder order = result.order();

        PersonInfo approvedBy = PersonInfo.from(result.approvedBy());

        List<LineInfo> lines = order.getLines().stream()
                .map(l -> new LineInfo(
                        l.getId(),
                        l.getItemSku(),
                        l.getItemNameSnapshot(),
                        l.getUnitSnapshot(),
                        l.getOrderQuantity(),
                        l.getUnitPrice().amount()
                ))
                .toList();

        return new PurchaseOrderDetailResponse(
                order.getCode(),
                new VendorInfo(result.vendorCode(), result.vendorName()),
                new WarehouseInfo(result.warehouseCode(), result.warehouseName()),
                approvedBy,
                order.getCreation().createdAt(),
                order.getMemo(),
                order.getStatus().name(),
                order.progress().name(),
                order.getTotalAmount().amount(),
                "KRW",
                lines
        );
    }
}
