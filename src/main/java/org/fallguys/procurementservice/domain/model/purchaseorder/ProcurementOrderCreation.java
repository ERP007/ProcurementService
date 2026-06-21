package org.fallguys.procurementservice.domain.model.purchaseorder;

import java.time.Instant;

public record ProcurementOrderCreation(String createdBy, Instant createdAt) {
}
