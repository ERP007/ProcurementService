package org.fallguys.procurementservice.application.port.outbound.port;

import org.fallguys.procurementservice.application.port.outbound.model.Executor;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;

public interface InboundStockPort {

    // 입고 이벤트를 outbox에 적재한다(호출자 트랜잭션에 합류).
    void inbound(PurchaseOrder order, Executor executor);
}
