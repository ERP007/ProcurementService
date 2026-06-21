package org.fallguys.procurementservice.domain.model.purchaseorder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.model.Money;
import org.fallguys.procurementservice.domain.model.purchaseorderline.PurchaseOrderLine;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class PurchaseOrder {
    private final String code;
    private String vendorCode;
    private String warehouseCode;
    private PurchaseOrderStatus status;
    private LocalDate desiredArrivalDate;
    private String memo;
    private List<PurchaseOrderLine> lines;
    private Money totalAmount;
    private ProcurementOrderCreation creation;

    // 신규 발주서를 생성한다. 총액은 라인 합계로 계산하고 생성 정보를 기록한다.
    public static PurchaseOrder create(
            String code,
            String vendorCode,
            String warehouseCode,
            PurchaseOrderStatus status,
            LocalDate desiredArrivalDate,
            String memo,
            List<PurchaseOrderLine> lines,
            String createdBy,
            Instant createdAt) {
        return new PurchaseOrder(
                code, vendorCode, warehouseCode, status, desiredArrivalDate, memo, lines,
                calculateTotalAmount(lines),
                new ProcurementOrderCreation(createdBy, createdAt));
    }

    // DRAFT 발주서의 내용을 수정한다. 총액은 라인 합계로 재계산하고 생성 정보는 유지한다.
    public void updateDraft(
            String vendorCode,
            String warehouseCode,
            LocalDate desiredArrivalDate,
            String memo,
            List<PurchaseOrderLine> lines) {
        if (this.status != PurchaseOrderStatus.DRAFT) {
            throw new BusinessValidationException(ProcurementErrorCode.PURCHASE_ORDER_NOT_DRAFT);
        }
        this.vendorCode = vendorCode;
        this.warehouseCode = warehouseCode;
        this.desiredArrivalDate = desiredArrivalDate;
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

    public void receive() {
        if (this.status != PurchaseOrderStatus.APPROVED) {
            throw new BusinessValidationException(ProcurementErrorCode.PURCHASE_ORDER_NOT_APPROVED);
        }
        this.status = PurchaseOrderStatus.RECEIVED;
    }

    public void cancel() {
        if (this.status != PurchaseOrderStatus.DRAFT && this.status != PurchaseOrderStatus.APPROVED) {
            throw new BusinessValidationException(ProcurementErrorCode.PURCHASE_ORDER_NOT_CANCELABLE);
        }
        this.status = PurchaseOrderStatus.CANCELED;
    }
}
