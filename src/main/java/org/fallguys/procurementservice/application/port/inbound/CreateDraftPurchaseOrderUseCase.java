package org.fallguys.procurementservice.application.port.inbound;

import org.fallguys.procurementservice.domain.model.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.UserRole;

public interface CreateDraftPurchaseOrderUseCase {
    PurchaseOrder createDraft(UserRole role, CreateDraftPurchaseOrderCommand command);
}
