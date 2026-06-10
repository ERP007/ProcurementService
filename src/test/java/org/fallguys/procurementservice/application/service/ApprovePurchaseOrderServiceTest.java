package org.fallguys.procurementservice.application.service;

import org.fallguys.procurementservice.application.port.inbound.ApprovePurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.outbound.ItemInfo;
import org.fallguys.procurementservice.application.port.outbound.LoadItemPort;
import org.fallguys.procurementservice.application.port.outbound.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.SavePurchaseOrderPort;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ApprovePurchaseOrderServiceTest {

    @Mock private LoadPurchaseOrderPort loadPurchaseOrderPort;
    @Mock private LoadItemPort loadItemPort;
    @Mock private SavePurchaseOrderPort savePurchaseOrderPort;

    @InjectMocks
    private ApprovePurchaseOrderService service;

    private PurchaseOrderLine draftLine;
    private PurchaseOrder draftPoWithLine;
    private PurchaseOrder draftPoEmpty;
    private ItemInfo itemInfo;

    @BeforeEach
    void setUp() {
        Money unitPrice = Money.of(BigDecimal.valueOf(10000));
        draftLine = new PurchaseOrderLine(1L, "SKU-001", null, null, 5, unitPrice);
        itemInfo = new ItemInfo("SKU-001", "브레이크 패드", "EA");

        ProcurementOrderCreation creation = new ProcurementOrderCreation("EMP-001", Instant.parse("2026-06-01T00:00:00Z"));

        draftPoWithLine = new PurchaseOrder(
                "PO-2026-06-0001", "VD-001", "HQ-SE-01",
                PurchaseOrderStatus.DRAFT,
                LocalDate.now().plusDays(7), "메모",
                List.of(draftLine),
                Money.of(BigDecimal.valueOf(50000)),
                creation, null, null, null
        );

        draftPoEmpty = new PurchaseOrder(
                "PO-2026-06-0001", "VD-001", "HQ-SE-01",
                PurchaseOrderStatus.DRAFT,
                LocalDate.now().plusDays(7), "메모",
                List.of(),
                Money.of(BigDecimal.ZERO),
                creation, null, null, null
        );
    }

    // ── 역할 검증 ──────────────────────────────────────────────────────────

    @Test
    void BRANCH_MANAGER_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.approve(UserRole.BRANCH_MANAGER, command()))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderPort, loadItemPort, savePurchaseOrderPort);
    }

    @Test
    void BRANCH_STAFF_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.approve(UserRole.BRANCH_STAFF, command()))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderPort, loadItemPort, savePurchaseOrderPort);
    }

    // ── 발주서 조회 ────────────────────────────────────────────────────────

    @Test
    void 발주서_미존재이면_ResourceNotFoundException_발생() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(UserRole.ADMIN, command()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(loadItemPort, savePurchaseOrderPort);
    }

    // ── 도착 희망일 검증 ───────────────────────────────────────────────────

    @Test
    void 도착희망일_1년_초과이면_BusinessValidationException_발생() {
        PurchaseOrder expiredPo = new PurchaseOrder(
                "PO-2026-06-0001", "VD-001", "HQ-SE-01",
                PurchaseOrderStatus.DRAFT,
                LocalDate.now().plusYears(1).plusDays(1), null,
                List.of(),
                Money.of(BigDecimal.ZERO),
                new ProcurementOrderCreation("EMP-001", Instant.now()), null, null, null
        );
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(expiredPo));

        assertThatThrownBy(() -> service.approve(UserRole.ADMIN, command()))
                .isInstanceOf(BusinessValidationException.class);

        verifyNoInteractions(loadItemPort, savePurchaseOrderPort);
    }

    // ── 상태 검증 (도메인) ─────────────────────────────────────────────────

    @Test
    void DRAFT_아닌_상태이면_도메인에서_BusinessValidationException_발생() {
        PurchaseOrder approvedPo = new PurchaseOrder(
                "PO-2026-06-0001", "VD-001", "HQ-SE-01",
                PurchaseOrderStatus.APPROVED,
                LocalDate.now().plusDays(7), null,
                List.of(),
                Money.of(BigDecimal.ZERO),
                new ProcurementOrderCreation("EMP-001", Instant.now()),
                new ProcurementOrderApproval("EMP-001", Instant.now()), null, null
        );
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(approvedPo));

        assertThatThrownBy(() -> service.approve(UserRole.ADMIN, command()))
                .isInstanceOf(BusinessValidationException.class);

        verifyNoInteractions(loadItemPort, savePurchaseOrderPort);
    }

    // ── 품목 검증 ──────────────────────────────────────────────────────────

    @Test
    void 존재하지_않는_SKU이면_ResourceNotFoundException_발생() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(draftPoWithLine));
        given(loadItemPort.loadAll(List.of("SKU-001"))).willReturn(Map.of());

        assertThatThrownBy(() -> service.approve(UserRole.ADMIN, command()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(savePurchaseOrderPort);
    }

    // ── 성공 ───────────────────────────────────────────────────────────────

    @Test
    void 라인_없는_발주서_승인_성공() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(draftPoEmpty));
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = service.approve(UserRole.ADMIN, command());

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.APPROVED);
        assertThat(result.getApproval()).isNotNull();
        assertThat(result.getApproval().approvedBy()).isEqualTo("EMP-001");
    }

    @Test
    void 승인_성공_스냅샷_갱신됨() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(draftPoWithLine));
        given(loadItemPort.loadAll(List.of("SKU-001"))).willReturn(Map.of("SKU-001", itemInfo));
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = service.approve(UserRole.ADMIN, command());

        PurchaseOrderLine line = result.getLines().get(0);
        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.APPROVED);
        assertThat(line.getItemNameSnapshot()).isEqualTo("브레이크 패드");
        assertThat(line.getUnitSnapshot()).isEqualTo("EA");
        assertThat(line.lineAmount().amount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
    }

    @Test
    void 승인_성공_저장_시_approval_세팅됨() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(draftPoWithLine));
        given(loadItemPort.loadAll(List.of("SKU-001"))).willReturn(Map.of("SKU-001", itemInfo));
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.approve(UserRole.HQ_STAFF, command());

        ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(savePurchaseOrderPort).save(captor.capture());

        PurchaseOrder saved = captor.getValue();
        assertThat(saved.getApproval().approvedBy()).isEqualTo("EMP-001");
        assertThat(saved.getApproval().approvedAt()).isNotNull();
        assertThat(saved.getReceiving()).isNull();
        assertThat(saved.getCancellation()).isNull();
    }

    // ── 픽스처 ────────────────────────────────────────────────────────────

    private ApprovePurchaseOrderCommand command() {
        return new ApprovePurchaseOrderCommand("PO-2026-06-0001", "EMP-001");
    }
}
