package org.fallguys.procurementservice.application.service;

import org.fallguys.procurementservice.application.port.inbound.command.CancelPurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderPort;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.*;
import org.fallguys.procurementservice.domain.model.purchaseorder.*;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CancelPurchaseOrderServiceTest {

    @Mock private LoadPurchaseOrderPort loadPurchaseOrderPort;
    @Mock private SavePurchaseOrderPort savePurchaseOrderPort;

    @InjectMocks
    private CancelPurchaseOrderService service;

    private PurchaseOrder draftPo;
    private PurchaseOrder approvedPo;

    @BeforeEach
    void setUp() {
        ProcurementOrderCreation creation = new ProcurementOrderCreation("EMP-001", Instant.parse("2026-06-01T00:00:00Z"));

        draftPo = new PurchaseOrder(
                "PO-2026-06-0001", "VD-001", "HQ-SE-01",
                PurchaseOrderStatus.DRAFT,
                LocalDate.now().plusDays(7), "메모",
                List.of(),
                Money.of(BigDecimal.ZERO),
                creation, null, null, null
        );

        approvedPo = new PurchaseOrder(
                "PO-2026-06-0001", "VD-001", "HQ-SE-01",
                PurchaseOrderStatus.APPROVED,
                LocalDate.now().plusDays(7), "메모",
                List.of(),
                Money.of(BigDecimal.ZERO),
                creation,
                new ProcurementOrderApproval("EMP-001", Instant.now()),
                null, null
        );
    }

    // ── 역할 검증 ──────────────────────────────────────────────────────────

    @Test
    void HQ_STAFF_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.cancel(UserRole.HQ_STAFF, command()))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderPort, savePurchaseOrderPort);
    }

    @Test
    void BRANCH_MANAGER_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.cancel(UserRole.BRANCH_MANAGER, command()))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderPort, savePurchaseOrderPort);
    }

    @Test
    void BRANCH_STAFF_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.cancel(UserRole.BRANCH_STAFF, command()))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderPort, savePurchaseOrderPort);
    }

    // ── 발주서 조회 ────────────────────────────────────────────────────────

    @Test
    void 발주서_미존재이면_ResourceNotFoundException_발생() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(UserRole.ADMIN, command()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(savePurchaseOrderPort);
    }

    // ── 상태 검증 (도메인) ─────────────────────────────────────────────────

    @Test
    void RECEIVED_상태이면_도메인에서_BusinessValidationException_발생() {
        PurchaseOrder receivedPo = new PurchaseOrder(
                "PO-2026-06-0001", "VD-001", "HQ-SE-01",
                PurchaseOrderStatus.RECEIVED,
                LocalDate.now().plusDays(7), null,
                List.of(),
                Money.of(BigDecimal.ZERO),
                new ProcurementOrderCreation("EMP-001", Instant.now()),
                new ProcurementOrderApproval("EMP-001", Instant.now()),
                new ProcurementOrderReceiving("EMP-001", Instant.now(), LocalDate.now()),
                null
        );
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(receivedPo));

        assertThatThrownBy(() -> service.cancel(UserRole.ADMIN, command()))
                .isInstanceOf(BusinessValidationException.class);

        verifyNoInteractions(savePurchaseOrderPort);
    }

    @Test
    void CANCELED_상태이면_도메인에서_BusinessValidationException_발생() {
        PurchaseOrder canceledPo = new PurchaseOrder(
                "PO-2026-06-0001", "VD-001", "HQ-SE-01",
                PurchaseOrderStatus.CANCELED,
                LocalDate.now().plusDays(7), null,
                List.of(),
                Money.of(BigDecimal.ZERO),
                new ProcurementOrderCreation("EMP-001", Instant.now()),
                null, null,
                new ProcurementOrderCancellation("EMP-001", Instant.now(), "이전 취소 사유")
        );
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(canceledPo));

        assertThatThrownBy(() -> service.cancel(UserRole.ADMIN, command()))
                .isInstanceOf(BusinessValidationException.class);

        verifyNoInteractions(savePurchaseOrderPort);
    }

    // ── 성공 ───────────────────────────────────────────────────────────────

    @Test
    void DRAFT_상태_취소_성공() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(draftPo));
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = service.cancel(UserRole.ADMIN, command());

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELED);
        assertThat(result.getCancellation()).isNotNull();
        assertThat(result.getCancellation().canceledBy()).isEqualTo("EMP-001");
        assertThat(result.getCancellation().cancelReason()).isEqualTo("재발주 예정");
        assertThat(result.getCancellation().canceledAt()).isNotNull();
    }

    @Test
    void APPROVED_상태_취소_성공() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(approvedPo));
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = service.cancel(UserRole.HQ_MANAGER, command());

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELED);
        assertThat(result.getCancellation().canceledBy()).isEqualTo("EMP-001");
    }

    @Test
    void 취소_성공_저장_시_cancellation_세팅됨() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(draftPo));
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.cancel(UserRole.ADMIN, command());

        ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(savePurchaseOrderPort).save(captor.capture());

        PurchaseOrder saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELED);
        assertThat(saved.getCancellation().canceledBy()).isEqualTo("EMP-001");
        assertThat(saved.getCancellation().cancelReason()).isEqualTo("재발주 예정");
        assertThat(saved.getReceiving()).isNull();
    }

    // ── 픽스처 ────────────────────────────────────────────────────────────

    private CancelPurchaseOrderCommand command() {
        return new CancelPurchaseOrderCommand("PO-2026-06-0001", "EMP-001", "재발주 예정");
    }
}
