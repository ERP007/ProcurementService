package org.fallguys.procurementservice.application.port.inbound.usecase;

import org.fallguys.procurementservice.application.port.inbound.command.ReceivePurchaseOrderCommand;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.UserRole;

public interface ReceivePurchaseOrderUseCase {
    PurchaseOrder receive(UserRole role, ReceivePurchaseOrderCommand command);
}
