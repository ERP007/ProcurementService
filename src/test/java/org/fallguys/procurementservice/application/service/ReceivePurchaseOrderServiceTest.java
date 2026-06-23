package org.fallguys.procurementservice.application.service;

import org.fallguys.procurementservice.application.port.inbound.command.ReceivePurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.outbound.port.InboundStockPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadWarehousePort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderStatusHistoryPort;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.Money;
import org.fallguys.procurementservice.domain.model.purchaseorder.ProcurementOrderCreation;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PurchaseOrderStatusHistory;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.ReceivingPayload;
import org.fallguys.procurementservice.domain.model.UserRole;
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
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ReceivePurchaseOrderServiceTest {

    @Mock private LoadPurchaseOrderPort loadPurchaseOrderPort;
    @Mock private LoadWarehousePort loadWarehousePort;
    @Mock private InboundStockPort inboundStockPort;
    @Mock private SavePurchaseOrderPort savePurchaseOrderPort;
    @Mock private SavePurchaseOrderStatusHistoryPort savePurchaseOrderStatusHistoryPort;

    @InjectMocks
    private ReceivePurchaseOrderService service;

    private PurchaseOrder approvedPo;
    private PurchaseOrder draftPo;

    @BeforeEach
    void setUp() {
        ProcurementOrderCreation creation = new ProcurementOrderCreation("EMP-001", Instant.parse("2026-05-01T09:00:00Z"));

        approvedPo = new PurchaseOrder(
                "PO-2026-05-0001", "VD-01", "WD-01",
                PurchaseOrderStatus.APPROVED,
                LocalDate.of(2026, 5, 24), null,
                List.of(),
                Money.of(BigDecimal.valueOf(6000000)),
                creation
        );

        draftPo = new PurchaseOrder(
                "PO-2026-05-0001", "VD-01", "WD-01",
                PurchaseOrderStatus.DRAFT,
                LocalDate.of(2026, 5, 24), null,
                List.of(),
                Money.of(BigDecimal.ZERO),
                creation
        );
    }

    // ── 역할 검증 ──────────────────────────────────────────────────────────

    @Test
    void BRANCH_MANAGER_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.receive(UserRole.BRANCH_MANAGER, command()))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderPort, loadWarehousePort, inboundStockPort, savePurchaseOrderPort);
    }

    @Test
    void BRANCH_STAFF_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.receive(UserRole.BRANCH_STAFF, command()))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderPort, loadWarehousePort, inboundStockPort, savePurchaseOrderPort);
    }

    // ── 발주서 조회 ────────────────────────────────────────────────────────

    @Test
    void 발주서_미존재이면_ResourceNotFoundException_발생() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.receive(UserRole.ADMIN, command()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(loadWarehousePort, inboundStockPort, savePurchaseOrderPort);
    }

    // ── 창고 검증 ──────────────────────────────────────────────────────────

    @Test
    void 창고_비활성이면_BusinessValidationException_발생() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(approvedPo));
        willThrow(new BusinessValidationException(
                org.fallguys.procurementservice.domain.exception.ProcurementErrorCode.WAREHOUSE_INACTIVE))
                .given(loadWarehousePort).verifyActive("WD-01");

        assertThatThrownBy(() -> service.receive(UserRole.ADMIN, command()))
                .isInstanceOf(BusinessValidationException.class);

        verifyNoInteractions(inboundStockPort, savePurchaseOrderPort);
    }

    // ── 상태 검증 (도메인) ─────────────────────────────────────────────────

    @Test
    void APPROVED_아닌_상태이면_도메인에서_BusinessValidationException_발생() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(draftPo));

        assertThatThrownBy(() -> service.receive(UserRole.ADMIN, command()))
                .isInstanceOf(BusinessValidationException.class);

        verifyNoInteractions(inboundStockPort, savePurchaseOrderPort);
    }

    // ── 성공 ───────────────────────────────────────────────────────────────

    @Test
    void 입고_성공_상태_RECEIVED로_변경() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(approvedPo));
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = service.receive(UserRole.ADMIN, command());

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);

        ArgumentCaptor<PurchaseOrderStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(PurchaseOrderStatusHistory.class);
        verify(savePurchaseOrderStatusHistoryPort).append(historyCaptor.capture());

        PurchaseOrderStatusHistory history = historyCaptor.getValue();
        assertThat(history.status()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(history.actorCode()).isEqualTo("EMP-001");
        assertThat(history.payload()).isEqualTo(new ReceivingPayload(LocalDate.of(2026, 5, 24)));
    }

    @Test
    void 입고_성공_inboundStockPort_호출됨() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(approvedPo));
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.receive(UserRole.HQ_MANAGER, command());

        ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(inboundStockPort).inbound(captor.capture(), any());
        assertThat(captor.getValue().getCode()).isEqualTo("PO-2026-05-0001");
    }

    @Test
    void 재고_서비스_실패시_저장_안됨() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(approvedPo));
        willThrow(new org.fallguys.procurementservice.domain.exception.ExternalServiceException(
                "ER-502", "일시적으로 서비스를 이용할 수 없습니다.", null))
                .given(inboundStockPort).inbound(any(), any());

        assertThatThrownBy(() -> service.receive(UserRole.ADMIN, command()))
                .isInstanceOf(org.fallguys.procurementservice.domain.exception.ExternalServiceException.class);

        verifyNoInteractions(savePurchaseOrderPort);
    }

    // ── 픽스처 ────────────────────────────────────────────────────────────

    private ReceivePurchaseOrderCommand command() {
        return new ReceivePurchaseOrderCommand("PO-2026-05-0001", "EMP-001", "홍길동", LocalDate.of(2026, 5, 24));
    }
}
