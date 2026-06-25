package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.usecase.CompleteSagaUseCase;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.PendingStatusChangePort;
import org.fallguys.procurementservice.application.port.outbound.port.PublishUserActivityPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderStatusHistoryPort;
import org.fallguys.procurementservice.application.port.outbound.model.UserActivity;
import org.fallguys.procurementservice.application.port.outbound.model.UserActivityType;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorder.SagaStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompleteSagaService implements CompleteSagaUseCase {

    private final LoadPurchaseOrderPort loadPurchaseOrderPort;
    private final SavePurchaseOrderPort savePurchaseOrderPort;
    private final PendingStatusChangePort pendingStatusChangePort;
    private final SavePurchaseOrderStatusHistoryPort savePurchaseOrderStatusHistoryPort;
    private final PublishUserActivityPort publishUserActivityPort;

    /**
     * 입고 성공 응답 수신 시 saga를 DONE으로 확정하고, staging된 상태 전환을 이력으로 승격한다.
     *
     * 흐름:
     * 1) code로 발주서를 조회한다(없으면 예외 → 리스너 재시도/DLQ 격리. correlationId 누락도 동일 경로).
     * 2) 대상 가드: 진행 중 saga(SENDING/PROCESSING)만 대상. 그 외(NONE/DONE/FAILED)는 skip(멱등).
     * 3) SENDING이면 PROCESSING을 거쳐 DONE으로(응답이 발행 confirm보다 먼저 도착한 경우).
     *    PROCESSING이면 바로 DONE으로 전이한다.
     * 4) staging을 조회해 이력으로 승격(append)하고 staging을 제거한다.
     *    행위 시점(occurredAt)이 history.createdAt으로 보존된다(KPI 타임스탬프 유지).
     *    staging이 없으면(중복 reply로 이미 승격됨 등) 이력은 생략하고 saga만 전진시킨다(방어).
     *
     * 트랜잭션: 쓰기. saga 전이·이력 승격·staging 제거가 한 트랜잭션. 멱등 — 중복 응답/재전송은 무해(skip).
     */
    @Override
    @Transactional
    public void complete(String purchaseOrderCode) {
        PurchaseOrder order = loadPurchaseOrderPort.findByCode(purchaseOrderCode)
                .orElseThrow(() -> new ResourceNotFoundException(ProcurementErrorCode.PURCHASE_ORDER_NOT_FOUND));
        SagaStatus saga = order.getSagaStatus();
        if (!saga.inProgress()) {
            return;
        }
        if (saga == SagaStatus.SENDING) {
            order.markSagaProcessing();
        }
        order.markSagaDone();
        savePurchaseOrderPort.save(order);

        pendingStatusChangePort.findByCode(purchaseOrderCode).ifPresent(pending -> {
            savePurchaseOrderStatusHistoryPort.append(pending.toHistory());
            pendingStatusChangePort.removeByCode(purchaseOrderCode);

            // 사용자 활동: async 입고 확정 시점에 발행. 행위자·행위 시점은 staging 값을 그대로 쓴다. status는 입고.
            publishUserActivityPort.publish(new UserActivity(
                    pending.actor().code(), UserActivityType.RECEIVED,
                    order.getCode(), order.getVendor().nameSnapshot(), pending.occurredAt(),
                    UserActivity.statusLabel(PurchaseOrderStatus.RECEIVED)));
        });
    }
}
