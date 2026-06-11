package org.fallguys.procurementservice.application.port.inbound;

import org.fallguys.procurementservice.domain.model.UserRole;

public interface GetPurchaseOrderUseCase {
    GetPurchaseOrderResult get(UserRole role, String code);
}
