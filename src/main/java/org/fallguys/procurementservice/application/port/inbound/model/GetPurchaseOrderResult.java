package org.fallguys.procurementservice.application.port.inbound.model;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.ActorRef;

/**
 * vendorName·warehouseName은 상태에 따라 결정된 표시명: 확정 건은 박제 snapshot, DRAFT는 live 조회값.
 * approvedBy는 APPROVED 이력에 박제된 행위자 스냅샷(없으면 null).
 */
public record GetPurchaseOrderResult(
        PurchaseOrder order,
        String vendorCode,
        String vendorName,
        String warehouseCode,
        String warehouseName,
        ActorRef approvedBy
) {}
