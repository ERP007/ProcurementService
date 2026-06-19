package org.fallguys.procurementservice.domain.model.purchaseorder;

import java.time.Instant;

public record ProcurementOrderCancellation(String canceledBy, Instant canceledAt, String cancelReason) {
}
