package org.fallguys.procurementservice.application.service;

import org.fallguys.procurementservice.application.port.inbound.model.PurchaseOrderHistoryEntry;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderStatusHistoriesPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadUsersPort;
import org.fallguys.procurementservice.application.port.outbound.model.UserInfo;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.Money;
import org.fallguys.procurementservice.domain.model.purchaseorder.ProcurementOrderCreation;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.CancellationPayload;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PurchaseOrderStatusHistory;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.ReceivingPayload;
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
    @Mock private LoadPurchaseOrderStatusHistoriesPort loadPurchaseOrderStatusHistoriesPort;
    @Mock private LoadUsersPort loadUsersPort;

    @InjectMocks
    private GetPurchaseOrderHistoriesService service;

    private static final Instant T1 = Instant.parse("2026-05-01T09:00:00Z");
    private static final Instant T2 = Instant.parse("2026-05-02T10:00:00Z");
    private static final Instant T3 = Instant.parse("2026-05-03T11:00:00Z");

    private PurchaseOrder existingOrder;

    @BeforeEach
    void setUp() {
        existingOrder = new PurchaseOrder("PO-2026-05-0001", "VD-01", "WD-01",
                PurchaseOrderStatus.DRAFT, null, null, List.of(),
                Money.of(BigDecimal.ZERO), new ProcurementOrderCreation("EMP-001", T1));
    }

    // ── 역할 검증 ──────────────────────────────────────────────────────────

    @Test
    void BRANCH_MANAGER_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.getHistories(UserRole.BRANCH_MANAGER, "PO-2026-05-0001"))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderPort, loadPurchaseOrderStatusHistoriesPort, loadUsersPort);
    }

    @Test
    void BRANCH_STAFF_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.getHistories(UserRole.BRANCH_STAFF, "PO-2026-05-0001"))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderPort, loadPurchaseOrderStatusHistoriesPort, loadUsersPort);
    }

    // ── 발주서 조회 ────────────────────────────────────────────────────────

    @Test
    void 발주서_미존재이면_ResourceNotFoundException_발생() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getHistories(UserRole.ADMIN, "PO-2026-05-0001"))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(loadPurchaseOrderStatusHistoriesPort, loadUsersPort);
    }

    // ── 성공 ───────────────────────────────────────────────────────────────

    @Test
    void DRAFT_발주서_이력_1건_반환() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(existingOrder));
        given(loadPurchaseOrderStatusHistoriesPort.findByPoCode("PO-2026-05-0001")).willReturn(List.of(
                new PurchaseOrderStatusHistory("PO-2026-05-0001", PurchaseOrderStatus.DRAFT, "EMP-001", null, T1)
        ));
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
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(existingOrder));
        given(loadPurchaseOrderStatusHistoriesPort.findByPoCode("PO-2026-05-0001")).willReturn(List.of(
                new PurchaseOrderStatusHistory("PO-2026-05-0001", PurchaseOrderStatus.APPROVED, "EMP-002", null, T2),
                new PurchaseOrderStatusHistory("PO-2026-05-0001", PurchaseOrderStatus.DRAFT, "EMP-001", null, T1)
        ));
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
    void 취소된_발주서_이력_3건_최신순_정렬_payload_포함() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(existingOrder));
        given(loadPurchaseOrderStatusHistoriesPort.findByPoCode("PO-2026-05-0001")).willReturn(List.of(
                new PurchaseOrderStatusHistory("PO-2026-05-0001", PurchaseOrderStatus.CANCELED, "EMP-002",
                        new CancellationPayload("단순 변심"), T3),
                new PurchaseOrderStatusHistory("PO-2026-05-0001", PurchaseOrderStatus.APPROVED, "EMP-002", null, T2),
                new PurchaseOrderStatusHistory("PO-2026-05-0001", PurchaseOrderStatus.DRAFT, "EMP-001", null, T1)
        ));
        given(loadUsersPort.findByCodes(anyList())).willReturn(Map.of(
                "EMP-001", new UserInfo("EMP-001", "김민재", "구매팀"),
                "EMP-002", new UserInfo("EMP-002", "이영희", "과장")
        ));

        List<PurchaseOrderHistoryEntry> result = service.getHistories(UserRole.ADMIN, "PO-2026-05-0001");

        assertThat(result).hasSize(3);
        assertThat(result.get(0).status()).isEqualTo(PurchaseOrderStatus.CANCELED);
        assertThat(result.get(0).payload()).isEqualTo(new CancellationPayload("단순 변심"));
        assertThat(result.get(1).status()).isEqualTo(PurchaseOrderStatus.APPROVED);
        assertThat(result.get(2).status()).isEqualTo(PurchaseOrderStatus.DRAFT);
    }

    @Test
    void 입고된_발주서_이력_3건_최신순_정렬_payload_포함() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(existingOrder));
        given(loadPurchaseOrderStatusHistoriesPort.findByPoCode("PO-2026-05-0001")).willReturn(List.of(
                new PurchaseOrderStatusHistory("PO-2026-05-0001", PurchaseOrderStatus.RECEIVED, "EMP-003",
                        new ReceivingPayload(LocalDate.of(2026, 5, 3)), T3),
                new PurchaseOrderStatusHistory("PO-2026-05-0001", PurchaseOrderStatus.APPROVED, "EMP-002", null, T2),
                new PurchaseOrderStatusHistory("PO-2026-05-0001", PurchaseOrderStatus.DRAFT, "EMP-001", null, T1)
        ));
        given(loadUsersPort.findByCodes(anyList())).willReturn(Map.of(
                "EMP-001", new UserInfo("EMP-001", "김민재", "구매팀"),
                "EMP-002", new UserInfo("EMP-002", "이영희", "과장"),
                "EMP-003", new UserInfo("EMP-003", "박철수", "창고팀")
        ));

        List<PurchaseOrderHistoryEntry> result = service.getHistories(UserRole.ADMIN, "PO-2026-05-0001");

        assertThat(result).hasSize(3);
        assertThat(result.get(0).status()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(result.get(0).payload()).isEqualTo(new ReceivingPayload(LocalDate.of(2026, 5, 3)));
        assertThat(result.get(1).status()).isEqualTo(PurchaseOrderStatus.APPROVED);
        assertThat(result.get(2).status()).isEqualTo(PurchaseOrderStatus.DRAFT);
    }

    @Test
    void 유저_조회_결과_없으면_changedBy_null() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(existingOrder));
        given(loadPurchaseOrderStatusHistoriesPort.findByPoCode("PO-2026-05-0001")).willReturn(List.of(
                new PurchaseOrderStatusHistory("PO-2026-05-0001", PurchaseOrderStatus.DRAFT, "EMP-001", null, T1)
        ));
        given(loadUsersPort.findByCodes(anyList())).willReturn(Map.of());

        List<PurchaseOrderHistoryEntry> result = service.getHistories(UserRole.ADMIN, "PO-2026-05-0001");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().changedBy()).isNull();
    }
}
