package org.fallguys.procurementservice.adapter.outbound.persistence.purchaseorder;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.fallguys.procurementservice.adapter.outbound.persistence.purchaseorderline.PurchaseOrderLineEntity;
import org.fallguys.procurementservice.adapter.outbound.persistence.vendor.VendorEntity;
import org.fallguys.procurementservice.domain.model.Money;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
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

    public PurchaseOrder toDomain() {
        return new PurchaseOrder(
                code,
                vendor.getCode(),
                warehouseCode,
                status,
                desiredArrivalDate,
                memo,
                lines.stream().map(PurchaseOrderLineEntity::toDomain).toList(),
                new Money(totalAmount),
                creation.toDomain()
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
                po.getTotalAmount().amount(),
                new ArrayList<>(),
                CreationEmbeddable.from(po.getCreation())
        );
        po.getLines().stream()
                .map(line -> PurchaseOrderLineEntity.from(line, entity))
                .forEach(entity.lines::add);
        return entity;
    }

    public PurchaseOrderEntity update(PurchaseOrder po, VendorEntity vendor) {
        this.vendor = vendor;
        this.warehouseCode = po.getWarehouseCode();
        this.status = po.getStatus();
        this.desiredArrivalDate = po.getDesiredArrivalDate();
        this.memo = po.getMemo();
        this.totalAmount = po.getTotalAmount().amount();
        this.lines.clear();
        po.getLines().stream()
                .map(line -> PurchaseOrderLineEntity.from(line, this))
                .forEach(this.lines::add);
        return this;
    }
}
