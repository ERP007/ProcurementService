package org.fallguys.procurementservice.application.port.inbound.usecase;

public interface CompensateInboundUseCase {

    // 입고 실패 응답 수신. status 롤백 + saga FAILED + 이력 기록. 멱등(이미 종료면 no-op).
    void compensate(String purchaseOrderCode, String errorCode, String errorMessage);
}
