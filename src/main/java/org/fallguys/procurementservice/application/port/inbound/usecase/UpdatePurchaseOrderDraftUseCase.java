package org.fallguys.procurementservice.application.port.inbound.usecase;

import org.fallguys.procurementservice.application.port.inbound.command.UpdatePurchaseOrderDraftCommand;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.UserRole;

public interface UpdatePurchaseOrderDraftUseCase {
    PurchaseOrder update(UserRole role, UpdatePurchaseOrderDraftCommand command);
}
