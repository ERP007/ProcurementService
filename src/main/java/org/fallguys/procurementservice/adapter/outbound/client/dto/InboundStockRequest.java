package org.fallguys.procurementservice.adapter.outbound.client.dto;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;

import java.util.List;

/**
 * 동기 입고 REST 요청 바디. inventory /internal/inventory/stocks/inbound 명세에 맞춘 wire 구조.
 * async outbox payload와 달리 executor는 포함하지 않는다(엔드포인트 미지원).
 */
public record InboundStockRequest(
        String sourceRef,
        String warehouseCode,
        List<Line> lines
) {
    public record Line(String sku, int quantity, Long sourceLineNo) {}

    // 도메인 PurchaseOrder → 요청. sourceRef=발주 code, sourceLineNo=라인 id.
    public static InboundStockRequest from(PurchaseOrder order) {
        List<Line> lines = order.getLines().stream()
                .map(line -> new Line(line.getItemSku(), line.getOrderQuantity(), line.getId()))
                .toList();
        return new InboundStockRequest(order.getCode(), order.getWarehouse().code(), lines);
    }
}
