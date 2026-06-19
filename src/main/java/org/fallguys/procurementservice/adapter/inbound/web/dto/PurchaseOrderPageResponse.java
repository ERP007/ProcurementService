package org.fallguys.procurementservice.adapter.inbound.web.dto;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderPage;

import java.util.List;

public record PurchaseOrderPageResponse(
        List<PurchaseOrderSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext
) {
    public static PurchaseOrderPageResponse from(PurchaseOrderPage page) {
        return new PurchaseOrderPageResponse(
                page.content().stream().map(PurchaseOrderSummaryResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.hasPrevious(),
                page.hasNext()
        );
    }
}
