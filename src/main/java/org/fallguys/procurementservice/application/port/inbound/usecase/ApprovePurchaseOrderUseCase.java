package org.fallguys.procurementservice.application.port.inbound.usecase;

import org.fallguys.procurementservice.application.port.inbound.command.ApprovePurchaseOrderCommand;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.UserRole;

public interface ApprovePurchaseOrderUseCase {
    PurchaseOrder approve(UserRole role, ApprovePurchaseOrderCommand command);
}
