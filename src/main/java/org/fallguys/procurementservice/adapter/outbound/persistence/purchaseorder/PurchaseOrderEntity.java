package org.fallguys.procurementservice.adapter.outbound.persistence.purchaseorder;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.fallguys.procurementservice.adapter.outbound.persistence.purchaseorderline.PurchaseOrderLineEntity;
import org.fallguys.procurementservice.domain.model.Money;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderSummary;
import org.fallguys.procurementservice.domain.model.purchaseorder.SagaStatus;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
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

    // 공급사·창고 master와의 JPA 연관은 끊고 code + 확정 시점 박제명만 보관한다(타 서비스 소유 데이터로 취급).
    // 변환 로직은 각 Embeddable VO 내부에 둔다.
    @Embedded
    private VendorRefEmbeddable vendor;

    @Embedded
    private WarehouseRefEmbeddable warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PurchaseOrderStatus status;

    @Column(name = "memo", columnDefinition = "text")
    private String memo;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderLineEntity> lines = new ArrayList<>();

    // 목록(summary) 조회 시 lines 컬렉션을 로드하지 않고 라인 개수만 서브쿼리로 계산한다.
    // INSERT/UPDATE에는 포함되지 않는 read-only 파생 값이다(생성자 인자는 무시됨).
    @Formula("(select count(*) from purchase_order_lines l where l.po_number = code)")
    private int lineCount;

    @Embedded
    private CreationEmbeddable creation;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_status", nullable = false)
    private SagaStatus sagaStatus;

    public PurchaseOrder toDomain() {
        return new PurchaseOrder(
                code,
                vendor.toDomain(),
                warehouse.toDomain(),
                status,
                memo,
                lines.stream().map(PurchaseOrderLineEntity::toDomain).toList(),
                new Money(totalAmount),
                creation.toDomain(),
                sagaStatus
        );
    }

    public static PurchaseOrderEntity from(PurchaseOrder po) {
        PurchaseOrderEntity entity = new PurchaseOrderEntity(
                po.getCode(),
                VendorRefEmbeddable.from(po.getVendor()),
                WarehouseRefEmbeddable.from(po.getWarehouse()),
                po.getStatus(),
                po.getMemo(),
                po.getTotalAmount().amount(),
                new ArrayList<>(),
                0, // lineCount: @Formula 파생 값, 조회 시 재계산되므로 무시됨
                CreationEmbeddable.from(po.getCreation()),
                null,
                po.getSagaStatus()
        );
        po.getLines().stream()
                .map(line -> PurchaseOrderLineEntity.from(line, entity))
                .forEach(entity.lines::add);
        return entity;
    }

    public PurchaseOrderSummary toSummary() {
        return new PurchaseOrderSummary(
                code,
                vendor.code(),
                vendor.nameSnapshot(),
                creation.createdAt(),
                lineCount,
                Money.of(totalAmount),
                status
        );
    }

    public PurchaseOrderEntity update(PurchaseOrder po) {
        this.vendor = VendorRefEmbeddable.from(po.getVendor());
        this.warehouse = WarehouseRefEmbeddable.from(po.getWarehouse());
        this.status = po.getStatus();
        this.memo = po.getMemo();
        this.totalAmount = po.getTotalAmount().amount();
        this.sagaStatus = po.getSagaStatus();
        this.lines.clear();
        po.getLines().stream()
                .map(line -> PurchaseOrderLineEntity.from(line, this))
                .forEach(this.lines::add);
        return this;
    }
}
