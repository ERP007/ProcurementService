package org.fallguys.procurementservice.application.port.inbound;

import org.fallguys.procurementservice.domain.model.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.UserRole;

public interface CreatePurchaseOrderUseCase {
    PurchaseOrder create(UserRole role, CreatePurchaseOrderCommand command);
}
