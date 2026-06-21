package org.fallguys.procurementservice.application.port.outbound.port;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderKpi;

import java.time.LocalDate;

public interface LoadPurchaseOrderKpiPort {
    PurchaseOrderKpi loadKpi(LocalDate today);
}
