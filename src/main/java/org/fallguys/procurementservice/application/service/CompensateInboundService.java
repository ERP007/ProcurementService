package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fallguys.procurementservice.application.port.inbound.usecase.CompensateInboundUseCase;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderStatusHistoryPort;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.SagaStatus;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PurchaseOrderStatusHistory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompensateInboundService implements CompensateInboundUseCase {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final LoadPurchaseOrderPort loadPurchaseOrderPort;
    private final SavePurchaseOrderPort savePurchaseOrderPort;
    private final SavePurchaseOrderStatusHistoryPort savePurchaseOrderStatusHistoryPort;

    /**
     * 입고 실패 응답 수신 시 입고를 보상한다.
     *
     * 흐름:
     * 1) code로 발주서를 조회한다(없으면 예외 → 리스너 재시도/DLQ 격리. correlationId 누락도 동일 경로).
     * 2) 대상 가드: 진행 중 saga(SENDING/PROCESSING)만 보상한다. 그 외(NONE/DONE/FAILED)는 skip.
     *    멱등(중복 실패 응답 무해) + 안전(saga 비대상 주문 오염 방지).
     * 3) 도메인 compensateInbound(): RECEIVED→APPROVED 롤백 + saga FAILED.
     * 4) 저장 후 보상 이력을 기록한다(status=되돌린 APPROVED, actor=SYSTEM, payload=null).
     *    실패 사유(errorCode/메시지)는 이력이 아니라 WARN 로그로만 남긴다.
     *
     * 트랜잭션: 쓰기. 롤백·저장·이력 적재가 한 트랜잭션. 멱등 — 중복 실패 응답은 skip.
     */
    @Override
    @Transactional
    public void compensate(String purchaseOrderCode, String errorCode, String errorMessage) {
        PurchaseOrder order = loadPurchaseOrderPort.findByCode(purchaseOrderCode)
                .orElseThrow(() -> new ResourceNotFoundException(ProcurementErrorCode.PURCHASE_ORDER_NOT_FOUND));
        SagaStatus saga = order.getSagaStatus();
        if (saga != SagaStatus.SENDING && saga != SagaStatus.PROCESSING) {
            return;
        }

        order.compensateInbound();
        PurchaseOrder saved = savePurchaseOrderPort.save(order);

        savePurchaseOrderStatusHistoryPort.append(PurchaseOrderStatusHistory.of(
                saved.getCode(), saved.getStatus(), SYSTEM_ACTOR, Instant.now()));

        log.warn("입고 보상 수행 poCode={} status={} errorCode={} errorMessage={}",
                saved.getCode(), saved.getStatus(), errorCode, errorMessage);
    }
}
