package org.fallguys.procurementservice.adapter.inbound.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelPurchaseOrderRequest(
        @NotBlank
        @Size(max = 200)
        String reason
) {
    public CancelPurchaseOrderRequest {
        if (reason != null) reason = reason.trim();
    }
}
