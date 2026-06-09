package org.fallguys.procurementservice.adapter.inbound.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.fallguys.procurementservice.application.port.inbound.CreatePurchaseOrderLineCommand;

import java.math.BigDecimal;

public record CreatePurchaseOrderLineRequest(
        @NotBlank(message = "품목 코드는 필수입니다.")
        String itemCode,

        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        int quantity,

        @NotNull(message = "단가는 필수입니다.")
        @DecimalMin(value = "0", message = "단가는 0 이상이어야 합니다.")
        BigDecimal unitPrice
) {
    public CreatePurchaseOrderLineCommand toCommand() {
        return new CreatePurchaseOrderLineCommand(itemCode, quantity, unitPrice);
    }
}
