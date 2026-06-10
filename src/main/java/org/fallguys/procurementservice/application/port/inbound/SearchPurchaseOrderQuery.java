package org.fallguys.procurementservice.application.port.inbound;

import org.fallguys.procurementservice.domain.model.PurchaseOrderStatus;

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
