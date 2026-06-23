package org.fallguys.procurementservice.application.port.inbound.usecase;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderKpi;
import org.fallguys.procurementservice.domain.model.UserRole;

public interface GetPurchaseOrderKpiUseCase {
    PurchaseOrderKpi getKpi(UserRole role);
}
