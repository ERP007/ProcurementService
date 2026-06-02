package org.fallguys.procurementservice.domain.model;

import java.time.Instant;

public record ProcurementOrderReceiving(String receivedBy, Instant receivedAt) {
}
