package org.fallguys.procurementservice.application.port.inbound.command;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;

import java.time.LocalDate;
import java.util.List;

public record CreatePurchaseOrderCommand(
        String userCode,
        String vendorCode,
        String warehouseCode,
        LocalDate desiredArrivalDate,
        String memo,
        List<PurchaseOrderLineCommand> lines,
        PurchaseOrderStatus status
) {}
