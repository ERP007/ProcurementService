package org.fallguys.procurementservice.domain.model;

import java.time.Instant;

public record ProcurementOrderCreation(String createdBy, Instant createdAt) {
}
