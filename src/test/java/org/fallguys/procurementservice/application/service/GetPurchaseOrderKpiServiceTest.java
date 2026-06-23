package org.fallguys.procurementservice.application.service;

import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderKpiPort;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderKpi;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GetPurchaseOrderKpiServiceTest {

    @Mock private LoadPurchaseOrderKpiPort loadPurchaseOrderKpiPort;

    @InjectMocks
    private GetPurchaseOrderKpiService service;

    // ── 역할 검증 ──────────────────────────────────────────────────────────

    @Test
    void BRANCH_MANAGER_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.getKpi(UserRole.BRANCH_MANAGER))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderKpiPort);
    }

    @Test
    void BRANCH_STAFF_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.getKpi(UserRole.BRANCH_STAFF))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderKpiPort);
    }

    // ── 성공 ───────────────────────────────────────────────────────────────

    @Test
    void ADMIN_역할이면_KPI_반환() {
        PurchaseOrderKpi kpi = new PurchaseOrderKpi(128, 3, 12, 4);
        given(loadPurchaseOrderKpiPort.loadKpi(any(LocalDate.class))).willReturn(kpi);

        PurchaseOrderKpi result = service.getKpi(UserRole.ADMIN);

        assertThat(result.totalCount()).isEqualTo(128);
        assertThat(result.draftCount()).isEqualTo(3);
        assertThat(result.approvedCount()).isEqualTo(12);
        assertThat(result.delayedCount()).isEqualTo(4);
        verify(loadPurchaseOrderKpiPort).loadKpi(any(LocalDate.class));
    }

    @Test
    void HQ_MANAGER_역할이면_KPI_반환() {
        PurchaseOrderKpi kpi = new PurchaseOrderKpi(50, 1, 5, 0);
        given(loadPurchaseOrderKpiPort.loadKpi(any(LocalDate.class))).willReturn(kpi);

        PurchaseOrderKpi result = service.getKpi(UserRole.HQ_MANAGER);

        assertThat(result.totalCount()).isEqualTo(50);
        verify(loadPurchaseOrderKpiPort).loadKpi(any(LocalDate.class));
    }

    @Test
    void HQ_STAFF_역할이면_KPI_반환() {
        PurchaseOrderKpi kpi = new PurchaseOrderKpi(10, 0, 3, 1);
        given(loadPurchaseOrderKpiPort.loadKpi(any(LocalDate.class))).willReturn(kpi);

        PurchaseOrderKpi result = service.getKpi(UserRole.HQ_STAFF);

        assertThat(result.totalCount()).isEqualTo(10);
        verify(loadPurchaseOrderKpiPort).loadKpi(any(LocalDate.class));
    }
}
