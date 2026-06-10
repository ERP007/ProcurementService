package org.fallguys.procurementservice.adapter.inbound.web.dto;

import java.time.Instant;

public record PurchaseOrderHistoryResponse(
        String status,
        PersonInfo changedBy,
        Instant changedAt
) {}
