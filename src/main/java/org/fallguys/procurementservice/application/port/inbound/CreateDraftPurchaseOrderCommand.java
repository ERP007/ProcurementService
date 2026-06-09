package org.fallguys.procurementservice.application.port.inbound;

import java.time.LocalDate;
import java.util.List;

public record CreateDraftPurchaseOrderCommand(
        String userCode,
        String vendorCode,
        String warehouseCode,
        LocalDate desiredArrivalDate,
        String memo,
        List<CreatePurchaseOrderLineCommand> lines
) {
}
