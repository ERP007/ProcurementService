package org.fallguys.procurementservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PurchaseOrderLine {
    private Long id;
    private String itemSku;
    private String itemNameSnapshot;
    private String unitSnapshot;
    private int orderQuantity;
    private Money unitPrice;
    private Money lineAmount;
}
