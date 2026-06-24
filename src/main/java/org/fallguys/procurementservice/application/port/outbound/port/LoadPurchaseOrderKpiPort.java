package org.fallguys.procurementservice.application.port.outbound.port;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderKpi;

public interface LoadPurchaseOrderKpiPort {
    PurchaseOrderKpi loadKpi();
}
