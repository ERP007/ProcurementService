package org.fallguys.procurementservice.adapter.outbound.persistence.purchaseorder;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.fallguys.procurementservice.domain.model.purchaseorder.ProcurementOrderCreation;

import java.time.Instant;

@Embeddable
public record CreationEmbeddable(

        @Column(name = "created_by", nullable = false)
        String createdBy,

        @Column(name = "created_at", nullable = false)
        Instant createdAt

) {
    public ProcurementOrderCreation toDomain() {
        return new ProcurementOrderCreation(createdBy, createdAt);
    }

    public static CreationEmbeddable from(ProcurementOrderCreation creation) {
        return new CreationEmbeddable(creation.createdBy(), creation.createdAt());
    }
}
