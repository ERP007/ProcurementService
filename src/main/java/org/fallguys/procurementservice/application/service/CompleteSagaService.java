package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.usecase.CompleteSagaUseCase;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderPort;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.SagaStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompleteSagaService implements CompleteSagaUseCase {

    private final LoadPurchaseOrderPort loadPurchaseOrderPort;
    private final SavePurchaseOrderPort savePurchaseOrderPort;

    /**
     * 입고 성공 응답 수신 시 saga를 DONE으로 확정한다.
     *
     * 흐름:
     * 1) code로 발주서를 조회한다(없으면 예외 → 리스너 재시도/DLQ 격리. correlationId 누락도 동일 경로).
     * 2) 멱등 가드: 이미 DONE/FAILED(종료)거나 NONE(미시작)이면 skip.
     * 3) SENDING이면 PROCESSING을 거쳐 DONE으로(응답이 발행 confirm보다 먼저 도착한 경우).
     *    PROCESSING이면 바로 DONE으로 전이한다.
     *
     * 트랜잭션: 쓰기. 멱등 — 중복 응답/재전송은 무해(skip).
     */
    @Override
    @Transactional
    public void complete(String purchaseOrderCode) {
        PurchaseOrder order = loadPurchaseOrderPort.findByCode(purchaseOrderCode)
                .orElseThrow(() -> new ResourceNotFoundException(ProcurementErrorCode.PURCHASE_ORDER_NOT_FOUND));
        SagaStatus saga = order.getSagaStatus();
        if (saga == SagaStatus.DONE || saga == SagaStatus.FAILED || saga == SagaStatus.NONE) {
            return;
        }
        if (saga == SagaStatus.SENDING) {
            order.markSagaProcessing();
        }
        order.markSagaDone();
        savePurchaseOrderPort.save(order);
    }
}
