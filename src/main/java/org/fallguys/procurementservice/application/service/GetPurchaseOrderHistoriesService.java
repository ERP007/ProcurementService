package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.usecase.GetPurchaseOrderHistoriesUseCase;
import org.fallguys.procurementservice.application.port.inbound.model.PurchaseOrderHistoryEntry;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderStatusHistoriesPort;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.CommonErrorCode;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PurchaseOrderStatusHistory;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPurchaseOrderHistoriesService implements GetPurchaseOrderHistoriesUseCase {

    private final LoadPurchaseOrderPort loadPurchaseOrderPort;
    private final LoadPurchaseOrderStatusHistoriesPort loadPurchaseOrderStatusHistoriesPort;

    /**
     * 발주서의 상태 변경 이력을 최신순으로 조회한다.
     *
     * 흐름:
     * 1) 역할 검증: ADMIN·HQ_MANAGER·HQ_STAFF만 허용.
     * 2) code로 발주서 존재를 확인한다(없으면 404).
     * 3) 이력 테이블에서 상태 변경 이력을 최신순으로 조회한다.
     * 4) 이력 행에 박제된 행위자 스냅샷으로 응답 항목을 조립한다(외부 호출 0, 당시 값 보존).
     *
     * 트랜잭션: 읽기 전용.
     *
     * 예외:
     * - 허용되지 않은 역할: ForbiddenException (403)
     * - 발주서 미존재: ResourceNotFoundException (PO-02-04, 404)
     */
    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderHistoryEntry> getHistories(UserRole role, String code) {
        if (!role.isHqUser()) {
            throw new ForbiddenException(CommonErrorCode.FORBIDDEN);
        }

        loadPurchaseOrderPort.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(ProcurementErrorCode.PURCHASE_ORDER_NOT_FOUND));

        List<PurchaseOrderStatusHistory> histories = loadPurchaseOrderStatusHistoriesPort.findByPoCode(code);

        return histories.stream()
                .map(history -> new PurchaseOrderHistoryEntry(
                        history.status(),
                        history.actor(),
                        history.payload(),
                        history.createdAt()
                ))
                .toList();
    }
}
