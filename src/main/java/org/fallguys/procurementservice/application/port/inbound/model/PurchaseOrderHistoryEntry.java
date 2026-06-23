package org.fallguys.procurementservice.application.port.inbound.model;

import org.fallguys.procurementservice.application.port.outbound.model.UserInfo;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.StatusChangePayload;

import java.time.Instant;

public record PurchaseOrderHistoryEntry(
        PurchaseOrderStatus status,
        UserInfo changedBy,
        StatusChangePayload payload,
        Instant changedAt
) {}
