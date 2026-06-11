package org.fallguys.procurementservice.domain.model;

import java.time.Instant;
import java.time.LocalDate;

public record ProcurementOrderReceiving(String receivedBy, Instant receivedAt, LocalDate receivedDate) {
}
