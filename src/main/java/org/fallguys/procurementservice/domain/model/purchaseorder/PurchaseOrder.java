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
    private VendorRef vendor;
    private WarehouseRef warehouse;
    private PurchaseOrderStatus status;
    private String memo;
    private List<PurchaseOrderLine> lines;
    private Money totalAmount;
    private ProcurementOrderCreation creation;
    private SagaStatus sagaStatus;

    // 직전 입고 saga 실패 사유. 보상 시 기록, 재시도(receive) 시 초기화. 진행 페이지 노출용.
    private String lastFailureReason;

    // 기존 시그니처 호환 생성자. saga 상태는 NONE으로 시작한다(영속 복원 시 전체 생성자 사용).
    public PurchaseOrder(
            String code,
            VendorRef vendor,
            WarehouseRef warehouse,
            PurchaseOrderStatus status,
            String memo,
            List<PurchaseOrderLine> lines,
            Money totalAmount,
            ProcurementOrderCreation creation) {
        this(code, vendor, warehouse, status, memo, lines, totalAmount, creation,
                SagaStatus.NONE, null);
    }

    /** 화면 표시용 진행 상태(status·sagaStatus 조합 파생). 조합 규칙은 PurchaseOrderProgress가 소유. */
    public PurchaseOrderProgress progress() {
        return PurchaseOrderProgress.from(status, sagaStatus);
    }

    /** 진행 페이지 폴링용 결과 구분. 폴링 지속 여부(pending)·성패를 백엔드가 saga 상태로 판단한다. */
    public ProgressOutcome sagaOutcome() {
        if (sagaStatus.inProgress()) {
            return ProgressOutcome.PENDING;
        }
        return sagaStatus == SagaStatus.FAILED ? ProgressOutcome.FAILED : ProgressOutcome.SUCCESS;
    }

    // 신규 발주서를 생성한다. 총액은 라인 합계로 계산하고 생성 정보를 기록한다.
    // vendor·warehouse는 확정(APPROVED) 생성이면 박제(snapshot), DRAFT면 codeOnly로 서비스가 구성해 넘긴다.
    public static PurchaseOrder create(
            String code,
            VendorRef vendor,
            WarehouseRef warehouse,
            PurchaseOrderStatus status,
            String memo,
            List<PurchaseOrderLine> lines,
            String createdBy,
            Instant createdAt) {
        return new PurchaseOrder(
                code, vendor, warehouse, status, memo, lines,
                calculateTotalAmount(lines),
                new ProcurementOrderCreation(createdBy, createdAt));
    }

    // DRAFT 발주서의 내용을 수정한다. 총액은 라인 합계로 재계산하고 생성 정보는 유지한다.
    // DRAFT는 미확정이므로 vendor·warehouse는 codeOnly(표시명 박제 X)로 받는다.
    public void updateDraft(
            VendorRef vendor,
            WarehouseRef warehouse,
            String memo,
            List<PurchaseOrderLine> lines) {
        if (this.status != PurchaseOrderStatus.DRAFT) {
            throw new BusinessValidationException(ProcurementErrorCode.PURCHASE_ORDER_NOT_DRAFT);
        }
        this.vendor = vendor;
        this.warehouse = warehouse;
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
    // 확정 시점이므로 vendor·warehouse 표시명을 박제(snapshot)한다. 이후 master가 바뀌어도 발주 당시 값이 보존된다.
    public void approve(VendorRef vendor, WarehouseRef warehouse, List<PurchaseOrderLine> lines) {
        if (this.status != PurchaseOrderStatus.DRAFT) {
            throw new BusinessValidationException(ProcurementErrorCode.PURCHASE_ORDER_NOT_DRAFT);
        }
        this.status = PurchaseOrderStatus.APPROVED;
        this.vendor = vendor;
        this.warehouse = warehouse;
        this.lines = lines;
        // totalAmount는 lines 파생값이므로 lines 교체 시 항상 재계산한다(updateDraft와 동일 불변식).
        this.totalAmount = calculateTotalAmount(lines);
    }

    // 입고 트리거. APPROVED→RECEIVED 전이와 함께 saga를 SENDING으로 연다(재고 입고 이벤트 발행 대기).
    public void receive() {
        if (this.status != PurchaseOrderStatus.APPROVED) {
            throw new BusinessValidationException(ProcurementErrorCode.PURCHASE_ORDER_NOT_APPROVED);
        }
        // 직전 saga가 아직 확정되지 않았으면 새 saga 전환을 막는다(staging 1건 불변식 보호).
        if (this.sagaStatus.inProgress()) {
            throw new BusinessValidationException(ProcurementErrorCode.SAGA_IN_PROGRESS);
        }
        this.status = PurchaseOrderStatus.RECEIVED;
        this.sagaStatus = SagaStatus.SENDING;
        // 재시도이므로 직전 실패 사유를 초기화한다(이전 보상 흔적이 진행 페이지에 남지 않도록).
        this.lastFailureReason = null;
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
    // 실패 사유를 박제해 진행 페이지 폴링 응답에 노출한다(재시도 시 receive에서 초기화).
    public void compensateInbound(String failureReason) {
        if (this.status != PurchaseOrderStatus.RECEIVED) {
            throw new BusinessValidationException(ProcurementErrorCode.INBOUND_COMPENSATION_NOT_ALLOWED);
        }
        this.status = PurchaseOrderStatus.APPROVED;
        this.sagaStatus = SagaStatus.FAILED;
        this.lastFailureReason = failureReason;
    }

    public void cancel() {
        if (this.status != PurchaseOrderStatus.DRAFT && this.status != PurchaseOrderStatus.APPROVED) {
            throw new BusinessValidationException(ProcurementErrorCode.PURCHASE_ORDER_NOT_CANCELABLE);
        }
        this.status = PurchaseOrderStatus.CANCELED;
    }
}
