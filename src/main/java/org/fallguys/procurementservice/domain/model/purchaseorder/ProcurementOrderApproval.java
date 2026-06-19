package org.fallguys.procurementservice.domain.model.purchaseorder;

import java.time.Instant;

public record ProcurementOrderApproval(String approvedBy, Instant approvedAt) {
}
