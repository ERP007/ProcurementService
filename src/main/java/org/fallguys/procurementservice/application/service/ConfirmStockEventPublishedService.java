package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.usecase.ConfirmStockEventPublishedUseCase;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderPort;
import org.fallguys.procurementservice.domain.model.purchaseorder.SagaStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfirmStockEventPublishedService implements ConfirmStockEventPublishedUseCase {

    private final LoadPurchaseOrderPort loadPurchaseOrderPort;
    private final SavePurchaseOrderPort savePurchaseOrderPort;

    /**
     * 입고 이벤트 발행 확정(broker ack) 시 saga를 PROCESSING으로 전이한다.
     *
     * 흐름:
     * 1) code로 발주서를 조회한다(없으면 방어적 no-op).
     * 2) saga가 SENDING일 때만 markSagaProcessing()을 호출한다(멱등: 재발행/중복 ack 무해).
     * 3) 저장한다.
     *
     * 트랜잭션: 쓰기. relay가 outbox PUBLISHED를 찍기 "전에" 먼저 커밋되어야 한다
     * (중간 실패 시 outbox는 PENDING으로 남고, 폴러가 재발행→이 메서드가 멱등 수렴).
     */
    @Override
    @Transactional
    public void confirmPublished(String purchaseOrderCode) {
        loadPurchaseOrderPort.findByCode(purchaseOrderCode).ifPresent(order -> {
            if (order.getSagaStatus() == SagaStatus.SENDING) {
                order.markSagaProcessing();
                savePurchaseOrderPort.save(order);
            }
        });
    }
}
