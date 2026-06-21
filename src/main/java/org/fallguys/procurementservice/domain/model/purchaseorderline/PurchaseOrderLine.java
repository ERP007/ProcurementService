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
}
