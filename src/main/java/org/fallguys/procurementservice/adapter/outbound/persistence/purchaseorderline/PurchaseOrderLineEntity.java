package org.fallguys.procurementservice.adapter.outbound.persistence.purchaseorderline;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.fallguys.procurementservice.adapter.outbound.persistence.purchaseorder.PurchaseOrderEntity;
import org.fallguys.procurementservice.domain.model.Money;
import org.fallguys.procurementservice.domain.model.purchaseorderline.PurchaseOrderLine;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_lines")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_number", nullable = false)
    private PurchaseOrderEntity purchaseOrder;

    @Column(name = "item_sku", nullable = false)
    private String itemSku;

    @Column(name = "item_name_snapshot")
    private String itemNameSnapshot;

    @Column(name = "unit_snapshot")
    private String unitSnapshot;

    @Column(name = "order_quantity", nullable = false)
    private int orderQuantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineAmount;

    public PurchaseOrderLine toDomain() {
        return new PurchaseOrderLine(id, itemSku, itemNameSnapshot, unitSnapshot,
                orderQuantity, new Money(unitPrice));
    }

    public static PurchaseOrderLineEntity from(PurchaseOrderLine line, PurchaseOrderEntity purchaseOrder) {
        return new PurchaseOrderLineEntity(
                line.getId(),
                purchaseOrder,
                line.getItemSku(),
                line.getItemNameSnapshot(),
                line.getUnitSnapshot(),
                line.getOrderQuantity(),
                line.getUnitPrice().amount(),
                line.lineAmount().amount()
        );
    }
}
