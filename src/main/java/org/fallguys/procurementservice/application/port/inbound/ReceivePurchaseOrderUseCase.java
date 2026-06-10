package org.fallguys.procurementservice.application.port.inbound;

import org.fallguys.procurementservice.domain.model.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.UserRole;

public interface ReceivePurchaseOrderUseCase {
    PurchaseOrder receive(UserRole role, ReceivePurchaseOrderCommand command);
}
