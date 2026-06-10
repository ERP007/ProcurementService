package org.fallguys.procurementservice.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.fallguys.procurementservice.domain.model.ProcurementOrderReceiving;

import java.time.Instant;
import java.time.LocalDate;

@Embeddable
public record ReceivingEmbeddable(

        @Column(name = "received_by")
        String receivedBy,

        @Column(name = "received_at")
        Instant receivedAt,

        @Column(name = "received_date")
        LocalDate receivedDate

) {
    public ProcurementOrderReceiving toDomain() {
        return new ProcurementOrderReceiving(receivedBy, receivedAt, receivedDate);
    }

    public static ReceivingEmbeddable from(ProcurementOrderReceiving receiving) {
        if (receiving == null) return null;
        return new ReceivingEmbeddable(receiving.receivedBy(), receiving.receivedAt(), receiving.receivedDate());
    }

    public static ProcurementOrderReceiving toDomain(ReceivingEmbeddable embeddable) {
        if (embeddable == null) return null;
        return embeddable.toDomain();
    }
}
