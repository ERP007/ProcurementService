package org.fallguys.procurementservice.application.port.outbound.port;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;

public interface InboundStockPort {
    void inbound(PurchaseOrder order);
}
