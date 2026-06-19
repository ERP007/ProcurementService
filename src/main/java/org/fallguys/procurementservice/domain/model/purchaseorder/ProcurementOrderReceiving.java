package org.fallguys.procurementservice.domain.model.purchaseorder;

import java.time.Instant;
import java.time.LocalDate;

public record ProcurementOrderReceiving(String receivedBy, Instant receivedAt, LocalDate receivedDate) {
}
