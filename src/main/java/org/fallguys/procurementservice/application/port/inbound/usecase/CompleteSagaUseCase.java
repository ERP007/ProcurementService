package org.fallguys.procurementservice.application.port.inbound.usecase;

public interface CompleteSagaUseCase {

    // 입고 성공 응답 수신. saga → DONE. 멱등(이미 종료면 no-op).
    void complete(String purchaseOrderCode);
}
