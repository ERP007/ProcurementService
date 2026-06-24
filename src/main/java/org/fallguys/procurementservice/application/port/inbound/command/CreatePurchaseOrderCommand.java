package org.fallguys.procurementservice.application.port.inbound.command;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;

import java.util.List;

public record CreatePurchaseOrderCommand(
        String userCode,
        String userName,
        String userPosition,
        String vendorCode,
        String warehouseCode,
        String memo,
        List<PurchaseOrderLineCommand> lines,
        PurchaseOrderStatus status
) {}
