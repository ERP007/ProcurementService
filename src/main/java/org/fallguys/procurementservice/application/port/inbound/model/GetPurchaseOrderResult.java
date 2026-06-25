package org.fallguys.procurementservice.application.port.inbound.model;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.ActorRef;

/**
 * order는 표시명까지 채워진 상태: 확정 건은 박제 snapshot, DRAFT는 서비스에서 vendor·warehouse·라인을 live로 채운 값.
 * 따라서 vendor·warehouse·라인 표시명은 order에서 바로 읽는다.
 * approvedBy는 APPROVED 이력에 박제된 행위자 스냅샷(없으면 null).
 */
public record GetPurchaseOrderResult(
        PurchaseOrder order,
        ActorRef approvedBy
) {}
