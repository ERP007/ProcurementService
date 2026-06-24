package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.query.SearchPurchaseOrderQuery;
import org.fallguys.procurementservice.application.port.inbound.usecase.SearchPurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.outbound.port.LoadVendorPort;
import org.fallguys.procurementservice.application.port.outbound.port.SearchPurchaseOrderPort;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.CommonErrorCode;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderPage;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderSummary;
import org.fallguys.procurementservice.domain.model.purchaseorder.VendorRef;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.fallguys.procurementservice.domain.model.vendor.Vendor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchPurchaseOrderService implements SearchPurchaseOrderUseCase {

    private final SearchPurchaseOrderPort searchPurchaseOrderPort;
    private final LoadVendorPort loadVendorPort;

    /**
     * 발주서 목록을 검색한다.
     *
     * 흐름:
     * 1) 역할 검증: ADMIN·HQ_MANAGER·HQ_STAFF만 허용.
     * 2) 조회 기간 검증: 시작일 ≤ 종료일, 최대 365일.
     * 3) 조건에 맞는 발주서 목록을 페이지 단위로 조회한다.
     * 4) DRAFT 건 공급사명 enrich: DRAFT는 박제(snapshot)가 없어 vendorName이 null이므로,
     *    페이지 내 DRAFT들의 vendor code를 모아 master를 IN 배치로 한 번 조회해 채운다(N+1 회피).
     *    확정 건은 박제값을 그대로 쓰므로 enrich 대상이 아니다.
     *
     * 트랜잭션: 읽기 전용. enrich 값은 응답 표시용이며 DB에 저장하지 않는다.
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
        return enrichDraftVendorNames(searchPurchaseOrderPort.search(query));
    }

    // DRAFT 건의 공급사명을 master IN 배치 조회로 채운다. DRAFT가 없으면 추가 쿼리 없이 원본 반환.
    private PurchaseOrderPage enrichDraftVendorNames(PurchaseOrderPage page) {
        List<String> draftVendorCodes = page.content().stream()
                .filter(s -> s.status() == PurchaseOrderStatus.DRAFT)
                .map(s -> s.vendor().code())
                .distinct()
                .toList();
        if (draftVendorCodes.isEmpty()) {
            return page;
        }

        Map<String, String> nameByCode = loadVendorPort.findAllByCodeIn(draftVendorCodes).stream()
                .collect(Collectors.toMap(Vendor::getCode, Vendor::getName));

        List<PurchaseOrderSummary> enriched = page.content().stream()
                .map(s -> enrichOne(s, nameByCode))
                .toList();

        return new PurchaseOrderPage(
                enriched, page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    // DRAFT면 master에서 찾은 이름으로 VendorRef를 다시 구성한다(없으면 null 유지). 확정 건은 원본 그대로.
    private PurchaseOrderSummary enrichOne(PurchaseOrderSummary summary, Map<String, String> nameByCode) {
        if (summary.status() != PurchaseOrderStatus.DRAFT) {
            return summary;
        }
        VendorRef vendor = new VendorRef(summary.vendor().code(), nameByCode.get(summary.vendor().code()));
        return new PurchaseOrderSummary(
                summary.code(), vendor, summary.createdAt(), summary.lineCount(),
                summary.totalAmount(), summary.status(), summary.progress());
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
