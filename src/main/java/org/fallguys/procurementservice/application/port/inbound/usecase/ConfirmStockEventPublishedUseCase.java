package org.fallguys.procurementservice.application.port.inbound.usecase;

public interface ConfirmStockEventPublishedUseCase {

    // 입고 이벤트 발행 확정(broker ack). saga SENDING→PROCESSING. 멱등.
    void confirmPublished(String purchaseOrderCode);
}
