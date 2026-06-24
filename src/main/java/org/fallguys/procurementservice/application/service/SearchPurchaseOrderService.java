package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.query.SearchPurchaseOrderQuery;
import org.fallguys.procurementservice.application.port.inbound.usecase.SearchPurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.outbound.port.SearchPurchaseOrderPort;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.CommonErrorCode;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderPage;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class SearchPurchaseOrderService implements SearchPurchaseOrderUseCase {

    private final SearchPurchaseOrderPort searchPurchaseOrderPort;

    /**
     * 발주서 목록을 검색한다.
     *
     * 흐름:
     * 1) 역할 검증: ADMIN·HQ_MANAGER·HQ_STAFF만 허용.
     * 2) 조회 기간 검증: 시작일 ≤ 종료일, 최대 365일.
     * 3) 조건에 맞는 발주서 목록을 페이지 단위로 조회한다.
     *
     * 트랜잭션: 읽기 전용.
     *
     * 예외:
     * - 허용되지 않은 역할: ForbiddenException (403)
     * - 시작일 > 종료일: BusinessValidationException (400)
     * - 조회 기간 365일 초과: BusinessValidationException (400)
     */
    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderPage search(UserRole role, SearchPurchaseOrderQuery query) {
        if (!role.isHqUser()) {
            throw new ForbiddenException(CommonErrorCode.FORBIDDEN);
        }
        validateDateRange(query.startDate(), query.endDate());
        return searchPurchaseOrderPort.search(query);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessValidationException(
                    ProcurementErrorCode.INVALID_QUERY_DATE_RANGE,
                    "시작일(" + startDate + ")은 종료일(" + endDate + ")보다 이전이어야 합니다."
            );
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days > 365) {
            throw new BusinessValidationException(
                    ProcurementErrorCode.INVALID_QUERY_DATE_PERIOD,
                    "조회 기간은 최대 365일입니다. 현재 요청: " + days + "일"
            );
        }
    }
}
