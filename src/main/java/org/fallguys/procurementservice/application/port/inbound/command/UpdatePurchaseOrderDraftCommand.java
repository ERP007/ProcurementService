package org.fallguys.procurementservice.application.port.inbound.command;

import java.util.List;

public record UpdatePurchaseOrderDraftCommand(
        String code,
        String userCode,
        String vendorCode,
        String warehouseCode,
        String memo,
        List<PurchaseOrderLineCommand> lines
) {}
