package org.fallguys.procurementservice.domain.model.purchaseorder;

import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PurchaseOrderSagaTest {

    private PurchaseOrder orderWith(PurchaseOrderStatus status) {
        return new PurchaseOrder(
                "PO-2026-05-0001", "VD-01", "WD-01",
                status,
                LocalDate.of(2026, 5, 24), null,
                List.of(),
                Money.of(BigDecimal.ZERO),
                new ProcurementOrderCreation("EMP-001", Instant.parse("2026-05-01T09:00:00Z"))
        );
    }

    // ── receive: 입고 트리거 ────────────────────────────────────────────────

    @Test
    void receive_APPROVED면_RECEIVED_SENDING으로_전이() {
        PurchaseOrder order = orderWith(PurchaseOrderStatus.APPROVED);

        order.receive();

        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.SENDING);
    }

    @Test
    void receive_APPROVED_아니면_예외() {
        PurchaseOrder order = orderWith(PurchaseOrderStatus.DRAFT);

        assertThatThrownBy(order::receive)
                .isInstanceOf(BusinessValidationException.class)
                .hasFieldOrPropertyWithValue("code", "PO-012");
        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.NONE);
    }

    // ── markSagaProcessing: SENDING→PROCESSING ─────────────────────────────

    @Test
    void markSagaProcessing_SENDING이면_PROCESSING으로_전이() {
        PurchaseOrder order = orderWith(PurchaseOrderStatus.APPROVED);
        order.receive();

        order.markSagaProcessing();

        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.PROCESSING);
    }

    @Test
    void markSagaProcessing_SENDING_아니면_예외() {
        PurchaseOrder order = orderWith(PurchaseOrderStatus.APPROVED);

        assertThatThrownBy(order::markSagaProcessing)
                .isInstanceOf(BusinessValidationException.class)
                .hasFieldOrPropertyWithValue("code", "PO-022");
    }

    // ── markSagaDone: PROCESSING→DONE ──────────────────────────────────────

    @Test
    void markSagaDone_PROCESSING이면_DONE으로_전이() {
        PurchaseOrder order = orderWith(PurchaseOrderStatus.APPROVED);
        order.receive();
        order.markSagaProcessing();

        order.markSagaDone();

        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.DONE);
    }

    @Test
    void markSagaDone_PROCESSING_아니면_예외() {
        PurchaseOrder order = orderWith(PurchaseOrderStatus.APPROVED);
        order.receive();

        assertThatThrownBy(order::markSagaDone)
                .isInstanceOf(BusinessValidationException.class)
                .hasFieldOrPropertyWithValue("code", "PO-023");
    }

    // ── compensateInbound: RECEIVED→APPROVED, saga FAILED ──────────────────

    @Test
    void compensateInbound_RECEIVED면_APPROVED로_롤백하고_FAILED() {
        PurchaseOrder order = orderWith(PurchaseOrderStatus.APPROVED);
        order.receive();
        order.markSagaProcessing();

        order.compensateInbound();

        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.APPROVED);
        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.FAILED);
    }

    @Test
    void compensateInbound_RECEIVED_아니면_예외() {
        PurchaseOrder order = orderWith(PurchaseOrderStatus.APPROVED);

        assertThatThrownBy(order::compensateInbound)
                .isInstanceOf(BusinessValidationException.class)
                .hasFieldOrPropertyWithValue("code", "PO-024");
    }
}
