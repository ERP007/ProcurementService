package org.fallguys.procurementservice.application.port.inbound;

import org.fallguys.procurementservice.domain.model.PurchaseOrderPage;
import org.fallguys.procurementservice.domain.model.UserRole;

public interface SearchPurchaseOrderUseCase {
    PurchaseOrderPage search(UserRole role, SearchPurchaseOrderQuery query);
}
