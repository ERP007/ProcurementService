package org.fallguys.procurementservice.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.fallguys.procurementservice.domain.model.ProcurementOrderCancellation;

import java.time.Instant;

@Embeddable
public record CancellationEmbeddable(

        @Column(name = "canceled_by")
        String canceledBy,

        @Column(name = "canceled_at")
        Instant canceledAt,

        @Column(name = "cancel_reason", columnDefinition = "text")
        String cancelReason

) {
    public ProcurementOrderCancellation toDomain() {
        return new ProcurementOrderCancellation(canceledBy, canceledAt, cancelReason);
    }

    public static CancellationEmbeddable from(ProcurementOrderCancellation cancellation) {
        if (cancellation == null) return null;
        return new CancellationEmbeddable(
                cancellation.canceledBy(),
                cancellation.canceledAt(),
                cancellation.cancelReason()
        );
    }

    public static ProcurementOrderCancellation toDomain(CancellationEmbeddable embeddable) {
        if (embeddable == null) return null;
        return embeddable.toDomain();
    }
}
