package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.model.PurchaseOrderProgressView;
import org.fallguys.procurementservice.application.port.inbound.usecase.GetPurchaseOrderProgressUseCase;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.domain.exception.CommonErrorCode;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPurchaseOrderProgressService implements GetPurchaseOrderProgressUseCase {

    private final LoadPurchaseOrderPort loadPurchaseOrderPort;

    /**
     * 발주서 진행 상태(폴링)를 조회한다.
     *
     * 흐름:
     * 1) 역할 검증: ADMIN·HQ_MANAGER·HQ_STAFF만 허용(본사 전용 화면).
     * 2) code로 발주서를 조회한다(없으면 404).
     * 3) 진행 뷰를 구성한다. progress(화면 표시값)·pending·outcome은 status·saga 조합으로
     *    백엔드가 파생하며, FAILED일 때만 박제된 실패 사유를 노출한다.
     *
     * 트랜잭션: 읽기 전용. 외부 호출 없이 local DB만 조회 — 폴링에 적합하다.
     *
     * 예외:
     * - 허용되지 않은 역할: ForbiddenException (403)
     * - 발주서 미존재: ResourceNotFoundException (PO-02-04, 404)
     */
    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderProgressView get(UserRole role, String code) {
        if (!role.isHqUser()) {
            throw new ForbiddenException(CommonErrorCode.FORBIDDEN);
        }

        PurchaseOrder order = loadPurchaseOrderPort.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(ProcurementErrorCode.PURCHASE_ORDER_NOT_FOUND));

        return PurchaseOrderProgressView.from(order);
    }
}
