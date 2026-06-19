package org.fallguys.procurementservice.application.port.inbound.command;

import java.time.LocalDate;
import java.util.List;

public record UpdatePurchaseOrderDraftCommand(
        String code,
        String vendorCode,
        String warehouseCode,
        LocalDate desiredArrivalDate,
        String memo,
        List<PurchaseOrderLineCommand> lines
) {}
