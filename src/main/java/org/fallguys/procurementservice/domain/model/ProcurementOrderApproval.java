package org.fallguys.procurementservice.domain.model;

import java.time.Instant;

public record ProcurementOrderApproval(String approvedBy, Instant approvedAt) {
}
