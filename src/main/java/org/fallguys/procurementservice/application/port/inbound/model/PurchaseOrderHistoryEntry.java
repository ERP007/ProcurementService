package org.fallguys.procurementservice.application.port.inbound.model;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.ActorRef;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.StatusChangePayload;

import java.time.Instant;

// changedBy는 행위 시점에 박제된 행위자 스냅샷(외부 호출 없이 이력 행에서 바로 구성).
public record PurchaseOrderHistoryEntry(
        PurchaseOrderStatus status,
        ActorRef changedBy,
        StatusChangePayload payload,
        Instant changedAt
) {}
