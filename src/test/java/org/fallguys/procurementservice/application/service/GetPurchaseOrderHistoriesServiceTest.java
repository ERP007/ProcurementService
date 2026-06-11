package org.fallguys.procurementservice.application.service;

import org.fallguys.procurementservice.application.port.inbound.PurchaseOrderHistoryEntry;
import org.fallguys.procurementservice.application.port.outbound.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.LoadUsersPort;
import org.fallguys.procurementservice.application.port.outbound.UserInfo;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.Money;
import org.fallguys.procurementservice.domain.model.ProcurementOrderApproval;
import org.fallguys.procurementservice.domain.model.ProcurementOrderCancellation;
import org.fallguys.procurementservice.domain.model.ProcurementOrderCreation;
import org.fallguys.procurementservice.domain.model.ProcurementOrderReceiving;
import org.fallguys.procurementservice.domain.model.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GetPurchaseOrderHistoriesServiceTest {

    @Mock private LoadPurchaseOrderPort loadPurchaseOrderPort;
    @Mock private LoadUsersPort loadUsersPort;

    @InjectMocks
    private GetPurchaseOrderHistoriesService service;

    private static final Instant T1 = Instant.parse("2026-05-01T09:00:00Z");
    private static final Instant T2 = Instant.parse("2026-05-02T10:00:00Z");
    private static final Instant T3 = Instant.parse("2026-05-03T11:00:00Z");

    private ProcurementOrderCreation creation;
    private ProcurementOrderApproval approval;
    private ProcurementOrderCancellation cancellation;
    private ProcurementOrderReceiving receiving;

    @BeforeEach
    void setUp() {
        creation = new ProcurementOrderCreation("EMP-001", T1);
        approval = new ProcurementOrderApproval("EMP-002", T2);
        cancellation = new ProcurementOrderCancellation("EMP-002", T3, "단순 변심");
        receiving = new ProcurementOrderReceiving("EMP-003", T3, LocalDate.of(2026, 5, 3));
    }

    // ── 역할 검증 ──────────────────────────────────────────────────────────

    @Test
    void BRANCH_MANAGER_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.getHistories(UserRole.BRANCH_MANAGER, "PO-2026-05-0001"))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderPort, loadUsersPort);
    }

    @Test
    void BRANCH_STAFF_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.getHistories(UserRole.BRANCH_STAFF, "PO-2026-05-0001"))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderPort, loadUsersPort);
    }

    // ── 발주서 조회 ────────────────────────────────────────────────────────

    @Test
    void 발주서_미존재이면_ResourceNotFoundException_발생() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getHistories(UserRole.ADMIN, "PO-2026-05-0001"))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(loadUsersPort);
    }

    // ── 성공 ───────────────────────────────────────────────────────────────

    @Test
    void DRAFT_발주서_이력_1건_반환() {
        PurchaseOrder order = draftPo();
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(order));
        given(loadUsersPort.findByCodes(List.of("EMP-001")))
                .willReturn(Map.of("EMP-001", new UserInfo("EMP-001", "김민재", "구매팀")));

        List<PurchaseOrderHistoryEntry> result = service.getHistories(UserRole.HQ_STAFF, "PO-2026-05-0001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(result.get(0).changedAt()).isEqualTo(T1);
        assertThat(result.get(0).changedBy().code()).isEqualTo("EMP-001");
    }

    @Test
    void 승인된_발주서_이력_2건_최신순_정렬() {
        PurchaseOrder order = approvedPo();
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(order));
        given(loadUsersPort.findByCodes(anyList())).willReturn(Map.of(
                "EMP-001", new UserInfo("EMP-001", "김민재", "구매팀"),
                "EMP-002", new UserInfo("EMP-002", "이영희", "과장")
        ));

        List<PurchaseOrderHistoryEntry> result = service.getHistories(UserRole.HQ_MANAGER, "PO-2026-05-0001");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).status()).isEqualTo(PurchaseOrderStatus.APPROVED);
        assertThat(result.get(0).changedAt()).isEqualTo(T2);
        assertThat(result.get(1).status()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(result.get(1).changedAt()).isEqualTo(T1);
    }

    @Test
    void 취소된_발주서_이력_3건_최신순_정렬() {
        PurchaseOrder order = canceledPo();
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(order));
        given(loadUsersPort.findByCodes(anyList())).willReturn(Map.of(
                "EMP-001", new UserInfo("EMP-001", "김민재", "구매팀"),
                "EMP-002", new UserInfo("EMP-002", "이영희", "과장")
        ));

        List<PurchaseOrderHistoryEntry> result = service.getHistories(UserRole.ADMIN, "PO-2026-05-0001");

        assertThat(result).hasSize(3);
        assertThat(result.get(0).status()).isEqualTo(PurchaseOrderStatus.CANCELED);
        assertThat(result.get(1).status()).isEqualTo(PurchaseOrderStatus.APPROVED);
        assertThat(result.get(2).status()).isEqualTo(PurchaseOrderStatus.DRAFT);
    }

    @Test
    void 입고된_발주서_이력_3건_최신순_정렬() {
        PurchaseOrder order = receivedPo();
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(order));
        given(loadUsersPort.findByCodes(anyList())).willReturn(Map.of(
                "EMP-001", new UserInfo("EMP-001", "김민재", "구매팀"),
                "EMP-002", new UserInfo("EMP-002", "이영희", "과장"),
                "EMP-003", new UserInfo("EMP-003", "박철수", "창고팀")
        ));

        List<PurchaseOrderHistoryEntry> result = service.getHistories(UserRole.ADMIN, "PO-2026-05-0001");

        assertThat(result).hasSize(3);
        assertThat(result.get(0).status()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(result.get(1).status()).isEqualTo(PurchaseOrderStatus.APPROVED);
        assertThat(result.get(2).status()).isEqualTo(PurchaseOrderStatus.DRAFT);
    }

    @Test
    void 유저_조회_결과_없으면_changedBy_null() {
        PurchaseOrder order = draftPo();
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(order));
        given(loadUsersPort.findByCodes(anyList())).willReturn(Map.of());

        List<PurchaseOrderHistoryEntry> result = service.getHistories(UserRole.ADMIN, "PO-2026-05-0001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).changedBy()).isNull();
    }

    // ── 픽스처 ────────────────────────────────────────────────────────────

    private PurchaseOrder draftPo() {
        return new PurchaseOrder("PO-2026-05-0001", "VD-01", "WD-01",
                PurchaseOrderStatus.DRAFT, null, null, List.of(),
                Money.of(BigDecimal.ZERO), creation, null, null, null);
    }

    private PurchaseOrder approvedPo() {
        return new PurchaseOrder("PO-2026-05-0001", "VD-01", "WD-01",
                PurchaseOrderStatus.APPROVED, null, null, List.of(),
                Money.of(BigDecimal.ZERO), creation, approval, null, null);
    }

    private PurchaseOrder canceledPo() {
        return new PurchaseOrder("PO-2026-05-0001", "VD-01", "WD-01",
                PurchaseOrderStatus.CANCELED, null, null, List.of(),
                Money.of(BigDecimal.ZERO), creation, approval, null, cancellation);
    }

    private PurchaseOrder receivedPo() {
        return new PurchaseOrder("PO-2026-05-0001", "VD-01", "WD-01",
                PurchaseOrderStatus.RECEIVED, null, null, List.of(),
                Money.of(BigDecimal.ZERO), creation, approval, receiving, null);
    }
}
