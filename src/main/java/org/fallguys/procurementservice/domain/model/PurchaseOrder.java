package org.fallguys.procurementservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;

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
    private ProcurementOrderApproval approval;
    private ProcurementOrderReceiving receiving;
    private ProcurementOrderCancellation cancellation;

    public void approve(ProcurementOrderApproval approval, List<PurchaseOrderLine> validatedLines) {
        if (this.status != PurchaseOrderStatus.DRAFT) {
            throw new BusinessValidationException(ProcurementErrorCode.PURCHASE_ORDER_NOT_DRAFT);
        }
        this.status = PurchaseOrderStatus.APPROVED;
        this.approval = approval;
        this.lines = validatedLines;
    }
}
