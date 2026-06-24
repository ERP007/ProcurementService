package org.fallguys.procurementservice.adapter.inbound.web.dto;

import org.fallguys.procurementservice.application.port.inbound.model.PurchaseOrderProgressView;
import org.fallguys.procurementservice.domain.model.purchaseorder.ProgressOutcome;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderProgress;

public record PurchaseOrderProgressResponse(
        String code,
        PurchaseOrderProgress progress,
        boolean pending,
        ProgressOutcome outcome,
        String failureReason
) {
    public static PurchaseOrderProgressResponse from(PurchaseOrderProgressView view) {
        return new PurchaseOrderProgressResponse(
                view.code(),
                view.progress(),
                view.pending(),
                view.outcome(),
                view.failureReason()
        );
    }
}
