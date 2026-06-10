package org.fallguys.procurementservice.application.port.outbound;

import org.fallguys.procurementservice.domain.model.PurchaseOrderKpi;

import java.time.LocalDate;

public interface LoadPurchaseOrderKpiPort {
    PurchaseOrderKpi loadKpi(LocalDate today);
}
