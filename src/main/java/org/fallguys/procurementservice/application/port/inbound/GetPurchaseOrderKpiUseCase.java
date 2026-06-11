package org.fallguys.procurementservice.application.port.inbound;

import org.fallguys.procurementservice.domain.model.PurchaseOrderKpi;
import org.fallguys.procurementservice.domain.model.UserRole;

public interface GetPurchaseOrderKpiUseCase {
    PurchaseOrderKpi getKpi(UserRole role);
}
