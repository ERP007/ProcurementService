package org.fallguys.procurementservice.application.port.inbound;

import org.fallguys.procurementservice.application.port.outbound.UserInfo;
import org.fallguys.procurementservice.application.port.outbound.WarehouseInfo;
import org.fallguys.procurementservice.domain.model.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.Vendor;

public record GetPurchaseOrderResult(
        PurchaseOrder order,
        Vendor vendor,
        WarehouseInfo warehouse,
        UserInfo approvedByUser
) {}
