package org.fallguys.procurementservice.application.service;

import org.fallguys.procurementservice.application.port.inbound.PurchaseOrderSortField;
import org.fallguys.procurementservice.application.port.inbound.SearchPurchaseOrderQuery;
import org.fallguys.procurementservice.application.port.inbound.SortDirection;
import org.fallguys.procurementservice.application.port.outbound.SearchPurchaseOrderPort;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.model.PurchaseOrderPage;
import org.fallguys.procurementservice.domain.model.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SearchPurchaseOrderServiceTest {

    @Mock private SearchPurchaseOrderPort searchPurchaseOrderPort;

    @InjectMocks
    private SearchPurchaseOrderService service;

    // ── 역할 검증 ──────────────────────────────────────────────────────────

    @Test
    void BRANCH_MANAGER_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.search(UserRole.BRANCH_MANAGER, query()))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(searchPurchaseOrderPort);
    }

    @Test
    void BRANCH_STAFF_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.search(UserRole.BRANCH_STAFF, query()))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(searchPurchaseOrderPort);
    }

    // ── 날짜 범위 검증 ─────────────────────────────────────────────────────

    @Test
    void 시작일이_종료일보다_이후이면_BusinessValidationException_발생() {
        SearchPurchaseOrderQuery q = queryWithDates(
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 1)
        );

        assertThatThrownBy(() -> service.search(UserRole.ADMIN, q))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("시작일");

        verifyNoInteractions(searchPurchaseOrderPort);
    }

    @Test
    void 조회_기간이_365일_초과이면_BusinessValidationException_발생() {
        SearchPurchaseOrderQuery q = queryWithDates(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2026, 6, 2)
        );

        assertThatThrownBy(() -> service.search(UserRole.ADMIN, q))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("365일");

        verifyNoInteractions(searchPurchaseOrderPort);
    }

    @Test
    void 조회_기간이_정확히_365일이면_통과() {
        LocalDate start = LocalDate.of(2025, 6, 2);
        LocalDate end = LocalDate.of(2026, 6, 2);
        SearchPurchaseOrderQuery q = queryWithDates(start, end);
        PurchaseOrderPage expected = new PurchaseOrderPage(List.of(), 1, 20, 0);
        given(searchPurchaseOrderPort.search(q)).willReturn(expected);

        PurchaseOrderPage result = service.search(UserRole.ADMIN, q);

        assertThat(result).isEqualTo(expected);
    }

    // ── 성공 ───────────────────────────────────────────────────────────────

    @Test
    void ADMIN_역할_정상_조회() {
        PurchaseOrderPage expected = new PurchaseOrderPage(List.of(), 1, 20, 0);
        given(searchPurchaseOrderPort.search(query())).willReturn(expected);

        PurchaseOrderPage result = service.search(UserRole.ADMIN, query());

        assertThat(result).isEqualTo(expected);
        verify(searchPurchaseOrderPort).search(query());
    }

    @Test
    void HQ_MANAGER_역할_정상_조회() {
        PurchaseOrderPage expected = new PurchaseOrderPage(List.of(), 1, 20, 42);
        given(searchPurchaseOrderPort.search(query())).willReturn(expected);

        PurchaseOrderPage result = service.search(UserRole.HQ_MANAGER, query());

        assertThat(result.totalElements()).isEqualTo(42);
    }

    @Test
    void HQ_STAFF_역할_정상_조회() {
        PurchaseOrderPage expected = new PurchaseOrderPage(List.of(), 1, 20, 0);
        given(searchPurchaseOrderPort.search(query())).willReturn(expected);

        service.search(UserRole.HQ_STAFF, query());

        verify(searchPurchaseOrderPort).search(query());
    }

    // ── 픽스처 ────────────────────────────────────────────────────────────

    private SearchPurchaseOrderQuery query() {
        return queryWithDates(LocalDate.now().minusDays(90), LocalDate.now());
    }

    private SearchPurchaseOrderQuery queryWithDates(LocalDate startDate, LocalDate endDate) {
        return new SearchPurchaseOrderQuery(
                null,
                List.of(PurchaseOrderStatus.DRAFT, PurchaseOrderStatus.APPROVED,
                        PurchaseOrderStatus.RECEIVED),
                null,
                startDate,
                endDate,
                PurchaseOrderSortField.CREATED_AT,
                SortDirection.DESC,
                20,
                1
        );
    }
}
