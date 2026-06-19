package org.fallguys.procurementservice.application.port.outbound.port;

import org.fallguys.procurementservice.application.port.inbound.query.SearchPurchaseOrderQuery;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderPage;

public interface SearchPurchaseOrderPort {
    PurchaseOrderPage search(SearchPurchaseOrderQuery query);
}
