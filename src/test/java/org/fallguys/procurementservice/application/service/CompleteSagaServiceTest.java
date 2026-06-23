package org.fallguys.procurementservice.application.service;

import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderPort;
import org.fallguys.procurementservice.domain.model.Money;
import org.fallguys.procurementservice.domain.model.purchaseorder.ProcurementOrderCreation;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorder.SagaStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompleteSagaServiceTest {

    @Mock private LoadPurchaseOrderPort loadPurchaseOrderPort;
    @Mock private SavePurchaseOrderPort savePurchaseOrderPort;

    @InjectMocks
    private CompleteSagaService service;

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
    void PROCESSING이면_DONE으로_확정() {
        PurchaseOrder order = order(PurchaseOrderStatus.RECEIVED, SagaStatus.PROCESSING);
        given(loadPurchaseOrderPort.findByCode(CODE)).willReturn(Optional.of(order));

        service.complete(CODE);

        ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(savePurchaseOrderPort).save(captor.capture());
        assertThat(captor.getValue().getSagaStatus()).isEqualTo(SagaStatus.DONE);
    }

    @Test
    void SENDING이면_PROCESSING_거쳐_DONE으로_확정() {
        PurchaseOrder order = order(PurchaseOrderStatus.RECEIVED, SagaStatus.SENDING);
        given(loadPurchaseOrderPort.findByCode(CODE)).willReturn(Optional.of(order));

        service.complete(CODE);

        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.DONE);
        verify(savePurchaseOrderPort).save(any());
    }

    @Test
    void 이미_DONE이면_멱등_skip() {
        given(loadPurchaseOrderPort.findByCode(CODE))
                .willReturn(Optional.of(order(PurchaseOrderStatus.RECEIVED, SagaStatus.DONE)));

        service.complete(CODE);

        verify(savePurchaseOrderPort, never()).save(any());
    }

    @Test
    void 이미_FAILED이면_skip() {
        given(loadPurchaseOrderPort.findByCode(CODE))
                .willReturn(Optional.of(order(PurchaseOrderStatus.APPROVED, SagaStatus.FAILED)));

        service.complete(CODE);

        verify(savePurchaseOrderPort, never()).save(any());
    }

    @Test
    void saga_NONE이면_skip() {
        given(loadPurchaseOrderPort.findByCode(CODE))
                .willReturn(Optional.of(order(PurchaseOrderStatus.APPROVED, SagaStatus.NONE)));

        service.complete(CODE);

        verify(savePurchaseOrderPort, never()).save(any());
    }

    @Test
    void 발주서_미존재이면_no_op() {
        given(loadPurchaseOrderPort.findByCode(CODE)).willReturn(Optional.empty());

        service.complete(CODE);

        verify(savePurchaseOrderPort, never()).save(any());
    }
}
