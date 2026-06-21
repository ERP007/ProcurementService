package org.fallguys.procurementservice.application.port.inbound.model;

import org.fallguys.procurementservice.application.port.outbound.model.UserInfo;
import org.fallguys.procurementservice.application.port.outbound.model.WarehouseInfo;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.vendor.Vendor;

public record GetPurchaseOrderResult(
        PurchaseOrder order,
        Vendor vendor,
        WarehouseInfo warehouse,
        UserInfo approvedByUser
) {}
