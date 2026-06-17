package org.fallguys.procurementservice.adapter.inbound.web.dto;

import org.fallguys.procurementservice.application.port.inbound.GetPurchaseOrderResult;
import org.fallguys.procurementservice.domain.model.PurchaseOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderDetailResponse(
        String code,
        VendorInfo vendor,
        WarehouseInfo warehouse,
        PersonInfo approvedBy,
        Instant createdAt,
        LocalDate desiredArrivalDate,
        String memo,
        String status,
        BigDecimal totalAmount,
        String currency,
        List<LineInfo> lines
) {
    public record VendorInfo(String code, String name) {}
    public record WarehouseInfo(String code, String name) {}
    public record LineInfo(Long id, String sku, String name, String unit, int quantity, BigDecimal unitPrice) {}

    public static PurchaseOrderDetailResponse from(GetPurchaseOrderResult result) {
        PurchaseOrder order = result.order();

        PersonInfo approvedBy = PersonInfo.from(result.approvedByUser());

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
                new VendorInfo(result.vendor().getCode(), result.vendor().getName()),
                new WarehouseInfo(result.warehouse().code(), result.warehouse().name()),
                approvedBy,
                order.getCreation().createdAt(),
                order.getDesiredArrivalDate(),
                order.getMemo(),
                order.getStatus().name(),
                order.getTotalAmount().amount(),
                "KRW",
                lines
        );
    }
}
