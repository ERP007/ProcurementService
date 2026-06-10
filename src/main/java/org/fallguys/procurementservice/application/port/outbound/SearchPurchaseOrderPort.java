package org.fallguys.procurementservice.application.port.outbound;

import org.fallguys.procurementservice.application.port.inbound.SearchPurchaseOrderQuery;
import org.fallguys.procurementservice.domain.model.PurchaseOrderPage;

public interface SearchPurchaseOrderPort {
    PurchaseOrderPage search(SearchPurchaseOrderQuery query);
}
