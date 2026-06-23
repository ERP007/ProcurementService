package org.fallguys.procurementservice.application.service;

import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderStatusHistoryPort;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.Money;
import org.fallguys.procurementservice.domain.model.purchaseorder.ProcurementOrderCreation;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorder.SagaStatus;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PurchaseOrderStatusHistory;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CompensateInboundServiceTest {

    @Mock private LoadPurchaseOrderPort loadPurchaseOrderPort;
    @Mock private SavePurchaseOrderPort savePurchaseOrderPort;
    @Mock private SavePurchaseOrderStatusHistoryPort savePurchaseOrderStatusHistoryPort;

    @InjectMocks
    private CompensateInboundService service;

    private static final String CODE = "PO-2026-05-0001";

    private PurchaseOrder order(PurchaseOrderStatus status, SagaStatus saga) {
        return new PurchaseOrder(
                CODE, "VD-01", "WD-01", status,
                LocalDate.of(2026, 5, 24), null, List.of(),
                Money.of(BigDecimal.ZERO),
                new ProcurementOrderCreation("EMP-001", Instant.parse("2026-05-01T09:00:00Z")),
                saga
        );
    }

    @Test
    void 활성_saga이면_롤백_FAILED_이력기록() {
        PurchaseOrder order = order(PurchaseOrderStatus.RECEIVED, SagaStatus.PROCESSING);
        given(loadPurchaseOrderPort.findByCode(CODE)).willReturn(Optional.of(order));
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.compensate(CODE, "INV-001", "재고 부족");

        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.APPROVED);
        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.FAILED);

        ArgumentCaptor<PurchaseOrderStatusHistory> captor =
                ArgumentCaptor.forClass(PurchaseOrderStatusHistory.class);
        verify(savePurchaseOrderStatusHistoryPort).append(captor.capture());
        PurchaseOrderStatusHistory history = captor.getValue();
        assertThat(history.status()).isEqualTo(PurchaseOrderStatus.APPROVED);
        assertThat(history.actorCode()).isEqualTo("SYSTEM");
        assertThat(history.payload()).isNull();
    }

    @Test
    void 이미_DONE이면_멱등_skip() {
        given(loadPurchaseOrderPort.findByCode(CODE))
                .willReturn(Optional.of(order(PurchaseOrderStatus.RECEIVED, SagaStatus.DONE)));

        service.compensate(CODE, "INV-001", "재고 부족");

        verify(savePurchaseOrderPort, never()).save(any());
        verifyNoInteractions(savePurchaseOrderStatusHistoryPort);
    }

    @Test
    void 이미_FAILED이면_skip() {
        given(loadPurchaseOrderPort.findByCode(CODE))
                .willReturn(Optional.of(order(PurchaseOrderStatus.APPROVED, SagaStatus.FAILED)));

        service.compensate(CODE, "INV-001", "재고 부족");

        verify(savePurchaseOrderPort, never()).save(any());
        verifyNoInteractions(savePurchaseOrderStatusHistoryPort);
    }

    @Test
    void saga_NONE이면_skip() {
        given(loadPurchaseOrderPort.findByCode(CODE))
                .willReturn(Optional.of(order(PurchaseOrderStatus.RECEIVED, SagaStatus.NONE)));

        service.compensate(CODE, "INV-001", "재고 부족");

        verify(savePurchaseOrderPort, never()).save(any());
        verifyNoInteractions(savePurchaseOrderStatusHistoryPort);
    }

    @Test
    void 발주서_미존재이면_예외_던져_DLQ로() {
        given(loadPurchaseOrderPort.findByCode(CODE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.compensate(CODE, "INV-001", "재고 부족"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(savePurchaseOrderPort, never()).save(any());
        verifyNoInteractions(savePurchaseOrderStatusHistoryPort);
    }
}
