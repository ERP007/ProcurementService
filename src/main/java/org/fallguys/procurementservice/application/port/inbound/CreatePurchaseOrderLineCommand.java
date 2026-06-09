package org.fallguys.procurementservice.application.port.inbound;

import java.math.BigDecimal;

public record CreatePurchaseOrderLineCommand(
    String itemCode,
    int quantity,
    BigDecimal unitPrice
) {}