package org.fallguys.procurementservice.adapter.inbound.web.dto;

import org.fallguys.procurementservice.application.port.inbound.model.PurchaseOrderHistoryEntry;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.CancellationPayload;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.ReceivingPayload;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.StatusChangePayload;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderHistoryResponse(
        String status,
        PersonInfo changedBy,
        Instant changedAt,
        PayloadInfo payload
) {
    // 상태별 부가 데이터. 부가 데이터가 없는 전이(DRAFT·APPROVED)는 null.
    public record PayloadInfo(LocalDate receivedDate, String cancelReason) {}

    public static PurchaseOrderHistoryResponse from(PurchaseOrderHistoryEntry entry) {
        return new PurchaseOrderHistoryResponse(
                entry.status().name(),
                PersonInfo.from(entry.changedBy()),
                entry.changedAt(),
                toPayloadInfo(entry.payload())
        );
    }

    public static List<PurchaseOrderHistoryResponse> listFrom(List<PurchaseOrderHistoryEntry> entries) {
        return entries.stream().map(PurchaseOrderHistoryResponse::from).toList();
    }

    private static PayloadInfo toPayloadInfo(StatusChangePayload payload) {
        return switch (payload) {
            case null -> null;
            case ReceivingPayload r -> new PayloadInfo(r.receivedDate(), null);
            case CancellationPayload c -> new PayloadInfo(null, c.cancelReason());
        };
    }
}
