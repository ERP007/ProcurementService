package org.fallguys.procurementservice.application.port.inbound.command;

import java.math.BigDecimal;

public record PurchaseOrderLineCommand(
    String itemSku,
    int quantity,
    BigDecimal unitPrice
) {}