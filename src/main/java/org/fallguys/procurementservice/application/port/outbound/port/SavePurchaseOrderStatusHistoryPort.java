package org.fallguys.procurementservice.application.port.outbound.port;

import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PurchaseOrderStatusHistory;

public interface SavePurchaseOrderStatusHistoryPort {
    void append(PurchaseOrderStatusHistory history);
}
