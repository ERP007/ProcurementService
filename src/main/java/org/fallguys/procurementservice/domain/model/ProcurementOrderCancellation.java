package org.fallguys.procurementservice.domain.model;

import java.time.Instant;

public record ProcurementOrderCancellation(String canceledBy, Instant canceledAt, String cancelReason) {
}
