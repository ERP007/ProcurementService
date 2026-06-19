package org.fallguys.procurementservice.application.port.inbound.usecase;

import org.fallguys.procurementservice.application.port.inbound.query.SearchPurchaseOrderQuery;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderPage;
import org.fallguys.procurementservice.domain.model.UserRole;

public interface SearchPurchaseOrderUseCase {
    PurchaseOrderPage search(UserRole role, SearchPurchaseOrderQuery query);
}
