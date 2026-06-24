package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.usecase.GetPurchaseOrderKpiUseCase;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderKpiPort;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.CommonErrorCode;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderKpi;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPurchaseOrderKpiService implements GetPurchaseOrderKpiUseCase {

    private final LoadPurchaseOrderKpiPort loadPurchaseOrderKpiPort;

    /**
     * 발주 현황 KPI를 조회한다.
     *
     * 흐름:
     * 1) 역할 검증: ADMIN·HQ_MANAGER·HQ_STAFF만 허용.
     * 2) 각 상태별 발주서 수를 집계한다.
     *    - totalCount: CANCELED를 제외한 활성 발주서 수.
     *    - draftCount: DRAFT 상태 수(승인 대기).
     *    - approvedCount: APPROVED 상태 수(도착 예정).
     *
     * 트랜잭션: 읽기 전용. 집계 쿼리 3건을 단일 트랜잭션으로 묶어 일관성 보장.
     *
     * 예외:
     * - 허용되지 않은 역할: ForbiddenException (403)
     */
    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderKpi getKpi(UserRole role) {
        if (!role.isHqUser()) {
            throw new ForbiddenException(CommonErrorCode.FORBIDDEN);
        }
        return loadPurchaseOrderKpiPort.loadKpi();
    }
}
