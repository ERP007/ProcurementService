package org.fallguys.procurementservice.domain.model.purchaseorder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.model.Money;
import org.fallguys.procurementservice.domain.model.purchaseorderline.PurchaseOrderLine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
public class PurchaseOrder {
    private final String code;
    private String vendorCode;
    private String warehouseCode;
    private PurchaseOrderStatus status;
    private String memo;
    private List<PurchaseOrderLine> lines;
    private Money totalAmount;
    private ProcurementOrderCreation creation;
    private SagaStatus sagaStatus;

    // 기존 시그니처 호환 생성자. saga 상태는 NONE으로 시작한다(영속 복원 시 전체 생성자 사용).
    public PurchaseOrder(
            String code,
            String vendorCode,
            String warehouseCode,
            PurchaseOrderStatus status,
            String memo,
            List<PurchaseOrderLine> lines,
            Money totalAmount,
            ProcurementOrderCreation creation) {
        this(code, vendorCode, warehouseCode, status, memo, lines, totalAmount, creation,
                SagaStatus.NONE);
    }

    // 신규 발주서를 생성한다. 총액은 라인 합계로 계산하고 생성 정보를 기록한다.
    public static PurchaseOrder create(
            String code,
            String vendorCode,
            String warehouseCode,
            PurchaseOrderStatus status,
            String memo,
            List<PurchaseOrderLine> lines,
            String createdBy,
            Instant createdAt) {
        return new PurchaseOrder(
                code, vendorCode, warehouseCode, status, memo, lines,
                calculateTotalAmount(lines),
                new ProcurementOrderCreation(createdBy, createdAt));
    }

    // DRAFT 발주서의 내용을 수정한다. 총액은 라인 합계로 재계산하고 생성 정보는 유지한다.
    public void updateDraft(
            String vendorCode,
            String warehouseCode,
            String memo,
            List<PurchaseOrderLine> lines) {
        if (this.status != PurchaseOrderStatus.DRAFT) {
            throw new BusinessValidationException(ProcurementErrorCode.PURCHASE_ORDER_NOT_DRAFT);
        }
        this.vendorCode = vendorCode;
        this.warehouseCode = warehouseCode;
        this.memo = memo;
        this.lines = lines;
        this.totalAmount = calculateTotalAmount(lines);
    }

    private static Money calculateTotalAmount(List<PurchaseOrderLine> lines) {
        return new Money(lines.stream()
                .map(line -> line.lineAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    // 상태 전환은 상태값만 바꾼다. 전환 부가 정보(담당자·시각·입고일·취소사유)는 이력(history) 테이블에 적재한다.
    public void approve(List<PurchaseOrderLine> lines) {
        if (this.status != PurchaseOrderStatus.DRAFT) {
            throw new BusinessValidationException(ProcurementErrorCode.PURCHASE_ORDER_NOT_DRAFT);
        }
        this.status = PurchaseOrderStatus.APPROVED;
        this.lines = lines;
    }

    // 입고 트리거. APPROVED→RECEIVED 전이와 함께 saga를 SENDING으로 연다(재고 입고 이벤트 발행 대기).
    public void receive() {
        if (this.status != PurchaseOrderStatus.APPROVED) {
            throw new BusinessValidationException(ProcurementErrorCode.PURCHASE_ORDER_NOT_APPROVED);
        }
        this.status = PurchaseOrderStatus.RECEIVED;
        this.sagaStatus = SagaStatus.SENDING;
    }

    // saga 진행: 이벤트 발행 완료(reply 대기). SENDING→PROCESSING.
    public void markSagaProcessing() {
        if (this.sagaStatus != SagaStatus.SENDING) {
            throw new BusinessValidationException(ProcurementErrorCode.SAGA_NOT_SENDING);
        }
        this.sagaStatus = SagaStatus.PROCESSING;
    }

    // saga 완료: 재고 입고 성공 reply 수신. PROCESSING→DONE.
    public void markSagaDone() {
        if (this.sagaStatus != SagaStatus.PROCESSING) {
            throw new BusinessValidationException(ProcurementErrorCode.SAGA_NOT_PROCESSING);
        }
        this.sagaStatus = SagaStatus.DONE;
    }

    // 보상: 재고 입고 실패 reply 수신. RECEIVED→직전 상태(APPROVED) 롤백, saga=FAILED.
    public void compensateInbound() {
        if (this.status != PurchaseOrderStatus.RECEIVED) {
            throw new BusinessValidationException(ProcurementErrorCode.INBOUND_COMPENSATION_NOT_ALLOWED);
        }
        this.status = PurchaseOrderStatus.APPROVED;
        this.sagaStatus = SagaStatus.FAILED;
    }

    public void cancel() {
        if (this.status != PurchaseOrderStatus.DRAFT && this.status != PurchaseOrderStatus.APPROVED) {
            throw new BusinessValidationException(ProcurementErrorCode.PURCHASE_ORDER_NOT_CANCELABLE);
        }
        this.status = PurchaseOrderStatus.CANCELED;
    }
}
