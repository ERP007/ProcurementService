package org.fallguys.procurementservice.application.port.inbound;

import org.fallguys.procurementservice.application.port.outbound.UserInfo;
import org.fallguys.procurementservice.domain.model.PurchaseOrderStatus;

import java.time.Instant;

public record PurchaseOrderHistoryEntry(
        PurchaseOrderStatus status,
        UserInfo changedBy,
        Instant changedAt
) {}
