package org.fallguys.procurementservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
    private ProcurementOrderCreation creation;
    private ProcurementOrderApproval approval;
    private ProcurementOrderReceiving receiving;
    private ProcurementOrderCancellation cancellation;
}
