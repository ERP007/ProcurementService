package org.fallguys.procurementservice.application.port.inbound.usecase;

import org.fallguys.procurementservice.application.port.inbound.command.CreatePurchaseOrderCommand;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.UserRole;

public interface CreatePurchaseOrderUseCase {
    PurchaseOrder create(UserRole role, CreatePurchaseOrderCommand command);
}
