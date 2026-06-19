package org.fallguys.procurementservice.adapter.outbound.persistence.purchaseorder;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.fallguys.procurementservice.domain.model.purchaseorder.ProcurementOrderApproval;

import java.time.Instant;

@Embeddable
public record ApprovalEmbeddable(

        @Column(name = "approved_by")
        String approvedBy,

        @Column(name = "approved_at")
        Instant approvedAt

) {
    public ProcurementOrderApproval toDomain() {
        return new ProcurementOrderApproval(approvedBy, approvedAt);
    }

    public static ApprovalEmbeddable from(ProcurementOrderApproval approval) {
        if (approval == null) return null;
        return new ApprovalEmbeddable(approval.approvedBy(), approval.approvedAt());
    }

    public static ProcurementOrderApproval toDomain(ApprovalEmbeddable embeddable) {
        if (embeddable == null) return null;
        return embeddable.toDomain();
    }
}
