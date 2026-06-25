package org.fallguys.procurementservice.application.port.outbound.port;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;

public interface SyncInboundStockPort {

    // 입고를 동기 REST로 inventory에 호출한다(호출자 트랜잭션 내, 부하 테스트용).
    void inbound(PurchaseOrder order);
}
