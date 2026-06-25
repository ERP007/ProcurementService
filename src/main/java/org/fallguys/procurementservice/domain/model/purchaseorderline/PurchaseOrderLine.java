package org.fallguys.procurementservice.domain.model.purchaseorderline;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.fallguys.procurementservice.domain.model.Money;

@Getter
@AllArgsConstructor
public class PurchaseOrderLine {
    private Long id;
    private String itemSku;
    private String itemNameSnapshot;
    private String unitSnapshot;
    private int orderQuantity;
    private Money unitPrice;

    public Money lineAmount() {
        return unitPrice.multiply(orderQuantity);
    }

    // DRAFT는 라인 스냅샷이 비어 있어 조회 시 live 품목값으로 채운다(영속화 X, 표시 용도).
    public void enrichSnapshot(String itemName, String unit) {
        this.itemNameSnapshot = itemName;
        this.unitSnapshot = unit;
    }
}
