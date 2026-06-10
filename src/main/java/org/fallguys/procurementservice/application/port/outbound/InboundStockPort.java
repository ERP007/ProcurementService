package org.fallguys.procurementservice.application.port.outbound;

import org.fallguys.procurementservice.domain.model.PurchaseOrder;

public interface InboundStockPort {
    void inbound(PurchaseOrder order);
}
