package org.fallguys.procurementservice.adapter.outbound.messaging.event;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;

import java.util.List;

/**
 * 입고 요청 이벤트 payload. inventory 입고 명세에 맞춘 wire 구조.
 * executor.name은 도메인에 없으므로 호출측(서비스)이 주입한다.
 */
public record InboundStockPayload(
        String sourceRef,
        String warehouseCode,
        Executor executor,
        List<Line> lines
) {
    public record Executor(String empNo, String name) {}

    public record Line(String sku, int quantity, Long sourceLineNo) {}

    // 도메인 PurchaseOrder + 실행자 정보 → payload. sourceRef=발주 code, sourceLineNo=라인 id.
    public static InboundStockPayload from(PurchaseOrder order, Executor executor) {
        List<Line> lines = order.getLines().stream()
                .map(line -> new Line(line.getItemSku(), line.getOrderQuantity(), line.getId()))
                .toList();
        return new InboundStockPayload(order.getCode(), order.getWarehouseCode(), executor, lines);
    }
}
