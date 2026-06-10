package org.fallguys.procurementservice.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "po_number_sequences")
@Getter
@NoArgsConstructor
public class PoNumberSequenceEntity {

    @Id
    private LocalDate seqDate;

    @Column(nullable = false)
    private int lastSeq;
}
