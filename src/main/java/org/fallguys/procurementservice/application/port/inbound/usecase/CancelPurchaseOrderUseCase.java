package org.fallguys.procurementservice.application.port.inbound.usecase;

import org.fallguys.procurementservice.application.port.inbound.command.CancelPurchaseOrderCommand;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.UserRole;

public interface CancelPurchaseOrderUseCase {
    PurchaseOrder cancel(UserRole role, CancelPurchaseOrderCommand command);
}
