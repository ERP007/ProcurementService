package org.fallguys.procurementservice.adapter.inbound.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.fallguys.procurementservice.application.port.inbound.command.CreatePurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.inbound.command.PurchaseOrderLineCommand;
import org.fallguys.procurementservice.application.port.inbound.command.UpdatePurchaseOrderDraftCommand;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;

import java.time.LocalDate;
import java.util.List;

public record DraftPurchaseOrderRequest(
        @NotBlank(message = "공급사 코드는 필수입니다.")
        String vendorCode,

        @NotBlank(message = "창고 코드는 필수입니다.")
        String warehouseCode,

        @NotNull(message = "도착 희망일은 필수입니다.")
        @FutureOrPresent(message = "도착 희망일은 오늘 이후여야 합니다.")
        LocalDate desiredArrivalDate,

        @Size(max = 500, message = "메모는 최대 500자까지 입력할 수 있습니다.")
        String memo,

        @Size(max = 100, message = "발주 라인은 최대 100개까지 입력할 수 있습니다.")
        @Valid
        List<PurchaseOrderLineRequest> lines
) {
    public CreatePurchaseOrderCommand toCommand(String userCode) {
        List<PurchaseOrderLineCommand> lineCommands = lines == null
                ? List.of()
                : lines.stream().map(PurchaseOrderLineRequest::toCommand).toList();

        return new CreatePurchaseOrderCommand(
                userCode,
                vendorCode,
                warehouseCode,
                desiredArrivalDate,
                memo,
                lineCommands,
                PurchaseOrderStatus.DRAFT
        );
    }

    public UpdatePurchaseOrderDraftCommand toUpdateCommand(String code) {
        List<PurchaseOrderLineCommand> lineCommands = lines == null
                ? List.of()
                : lines.stream().map(PurchaseOrderLineRequest::toCommand).toList();

        return new UpdatePurchaseOrderDraftCommand(
                code,
                vendorCode,
                warehouseCode,
                desiredArrivalDate,
                memo,
                lineCommands
        );
    }
}
