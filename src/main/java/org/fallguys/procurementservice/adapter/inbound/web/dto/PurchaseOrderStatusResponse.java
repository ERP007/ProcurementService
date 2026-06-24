package org.fallguys.procurementservice.adapter.inbound.web.dto;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;

import java.math.BigDecimal;

/**
 * 발주 상태 전환(생성·제출·승인·취소·입고) 공통 응답.
 *
 * 상태별 부가 데이터(승인 담당자·입고일·취소 사유 등)는 이력 조회로 확인하며,
 * 전환 응답은 결과 상태와 공통 식별 정보만 반환한다.
 */
public record PurchaseOrderStatusResponse(
        String code,
        String vendorCode,
        String warehouseCode,
        String status,
        BigDecimal totalAmount,
        String currency
) {
    public static PurchaseOrderStatusResponse from(PurchaseOrder order) {
        return new PurchaseOrderStatusResponse(
                order.getCode(),
                order.getVendorCode(),
                order.getWarehouseCode(),
                order.getStatus().name(),
                order.getTotalAmount().amount(),
                "KRW"
        );
    }
}
