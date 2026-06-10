package org.fallguys.procurementservice.adapter.inbound.web.dto;

import org.fallguys.procurementservice.application.port.inbound.PurchaseOrderHistoryEntry;

import java.time.Instant;
import java.util.List;

public record PurchaseOrderHistoryResponse(
        String status,
        PersonInfo changedBy,
        Instant changedAt
) {
    public static PurchaseOrderHistoryResponse from(PurchaseOrderHistoryEntry entry) {
        return new PurchaseOrderHistoryResponse(
                entry.status().name(),
                PersonInfo.from(entry.changedBy()),
                entry.changedAt()
        );
    }

    public static List<PurchaseOrderHistoryResponse> listFrom(List<PurchaseOrderHistoryEntry> entries) {
        return entries.stream().map(PurchaseOrderHistoryResponse::from).toList();
    }
}
