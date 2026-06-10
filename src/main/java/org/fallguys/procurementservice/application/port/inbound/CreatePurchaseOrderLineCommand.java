package org.fallguys.procurementservice.application.port.inbound;

import java.math.BigDecimal;

public record CreatePurchaseOrderLineCommand(
    String itemSku,
    int quantity,
    BigDecimal unitPrice
) {}