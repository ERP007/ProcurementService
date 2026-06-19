package org.fallguys.procurementservice.application.port.inbound.query;

import org.fallguys.procurementservice.application.port.inbound.model.PurchaseOrderSortField;
import org.fallguys.procurementservice.application.port.inbound.model.SortDirection;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;

import java.time.LocalDate;
import java.util.List;

public record SearchPurchaseOrderQuery(
        String search,
        List<PurchaseOrderStatus> statuses,
        String vendorCode,
        LocalDate startDate,
        LocalDate endDate,
        PurchaseOrderSortField sortField,
        SortDirection sortDirection,
        int size,
        int page
) {}
