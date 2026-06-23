package org.fallguys.procurementservice.application.port.inbound.usecase;

import org.fallguys.procurementservice.application.port.inbound.model.GetPurchaseOrderResult;
import org.fallguys.procurementservice.domain.model.UserRole;

public interface GetPurchaseOrderUseCase {
    GetPurchaseOrderResult get(UserRole role, String code);
}
