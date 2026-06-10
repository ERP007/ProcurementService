package org.fallguys.procurementservice.adapter.outbound.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.fallguys.procurementservice.domain.model.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.PurchaseOrderStatus;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderEntity {

    @Id
    @Column(name = "code", nullable = false)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_code", nullable = false)
    private VendorEntity vendor;

    @Column(name = "warehouse_code", nullable = false)
    private String warehouseCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PurchaseOrderStatus status;

    @Column(name = "desired_arrival_date", nullable = false)
    private LocalDate desiredArrivalDate;

    @Column(name = "memo", columnDefinition = "text")
    private String memo;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderLineEntity> lines = new ArrayList<>();

    @Embedded
    private CreationEmbeddable creation;

    @Embedded
    private ApprovalEmbeddable approval;

    @Embedded
    private ReceivingEmbeddable receiving;

    @Embedded
    private CancellationEmbeddable cancellation;

    public PurchaseOrder toDomain() {
        return new PurchaseOrder(
                code,
                vendor.getCode(),
                warehouseCode,
                status,
                desiredArrivalDate,
                memo,
                lines.stream().map(PurchaseOrderLineEntity::toDomain).toList(),
                creation.toDomain(),
                ApprovalEmbeddable.toDomain(approval),
                ReceivingEmbeddable.toDomain(receiving),
                CancellationEmbeddable.toDomain(cancellation)
        );
    }

    public static PurchaseOrderEntity from(PurchaseOrder po, VendorEntity vendor) {
        PurchaseOrderEntity entity = new PurchaseOrderEntity(
                po.getCode(),
                vendor,
                po.getWarehouseCode(),
                po.getStatus(),
                po.getDesiredArrivalDate(),
                po.getMemo(),
                BigDecimal.ZERO,
                new ArrayList<>(),
                CreationEmbeddable.from(po.getCreation()),
                ApprovalEmbeddable.from(po.getApproval()),
                ReceivingEmbeddable.from(po.getReceiving()),
                CancellationEmbeddable.from(po.getCancellation())
        );
        po.getLines().stream()
                .map(line -> PurchaseOrderLineEntity.from(line, entity))
                .forEach(entity.lines::add);
        entity.totalAmount = computeTotalAmount(entity.lines);
        return entity;
    }

    public PurchaseOrderEntity update(PurchaseOrder po, VendorEntity vendor) {
        this.vendor = vendor;
        this.warehouseCode = po.getWarehouseCode();
        this.status = po.getStatus();
        this.desiredArrivalDate = po.getDesiredArrivalDate();
        this.memo = po.getMemo();
        this.approval = ApprovalEmbeddable.from(po.getApproval());
        this.receiving = ReceivingEmbeddable.from(po.getReceiving());
        this.cancellation = CancellationEmbeddable.from(po.getCancellation());
        this.lines.clear();
        po.getLines().stream()
                .map(line -> PurchaseOrderLineEntity.from(line, this))
                .forEach(this.lines::add);
        this.totalAmount = computeTotalAmount(this.lines);
        return this;
    }

    private static BigDecimal computeTotalAmount(List<PurchaseOrderLineEntity> lines) {
        return lines.stream()
                .map(PurchaseOrderLineEntity::getLineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
