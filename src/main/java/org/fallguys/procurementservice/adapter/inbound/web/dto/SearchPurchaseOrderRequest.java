package org.fallguys.procurementservice.adapter.inbound.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.fallguys.procurementservice.application.port.inbound.PurchaseOrderSortField;
import org.fallguys.procurementservice.application.port.inbound.SearchPurchaseOrderQuery;
import org.fallguys.procurementservice.application.port.inbound.SortDirection;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.model.PurchaseOrderStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public record SearchPurchaseOrderRequest(
        String search,
        String status,
        String vendorCode,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,

        @Pattern(regexp = "createdAt|desiredArrivalDate|totalAmount",
                message = "sortField는 createdAt·desiredArrivalDate·totalAmount 중 하나여야 합니다.")
        String sortField,

        @Pattern(regexp = "asc|desc",
                message = "sortDirection은 asc 또는 desc여야 합니다.")
        String sortDirection,

        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 50, message = "size는 50 이하여야 합니다.")
        Integer size,

        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        Integer page
) {
    private static final Set<Integer> VALID_SIZES = Set.of(10, 20, 50);

    public SearchPurchaseOrderQuery toQuery() {
        int resolvedSize = size != null ? size : 20;
        validateSize(resolvedSize);

        return new SearchPurchaseOrderQuery(
                search,
                resolveStatuses(status),
                vendorCode,
                startDate != null ? startDate : LocalDate.now().minusDays(90),
                endDate != null ? endDate : LocalDate.now(),
                resolveSortField(sortField),
                resolveSortDirection(sortDirection),
                resolvedSize,
                page != null ? page : 1
        );
    }

    private static void validateSize(int size) {
        if (!VALID_SIZES.contains(size)) {
            throw new BusinessValidationException(
                    ProcurementErrorCode.INVALID_QUERY_PARAMETER,
                    "size는 10, 20, 50 중 하나여야 합니다. 입력값: " + size
            );
        }
    }

    private static List<PurchaseOrderStatus> resolveStatuses(String status) {
        if (status == null || status.isBlank()) {
            return Arrays.stream(PurchaseOrderStatus.values())
                    .filter(s -> s != PurchaseOrderStatus.CANCELED)
                    .toList();
        }
        return Arrays.stream(status.split(","))
                .map(String::trim)
                .map(s -> {
                    try {
                        return PurchaseOrderStatus.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        throw new BusinessValidationException(
                                ProcurementErrorCode.INVALID_QUERY_PARAMETER,
                                "'" + s + "'는 유효하지 않은 status입니다. 허용 값: "
                        );
                    }
                })
                .toList();
    }

    private static PurchaseOrderSortField resolveSortField(String sortField) {
        if (sortField == null) return PurchaseOrderSortField.CREATED_AT;
        return switch (sortField) {
            case "desiredArrivalDate" -> PurchaseOrderSortField.DESIRED_ARRIVAL_DATE;
            case "totalAmount" -> PurchaseOrderSortField.TOTAL_AMOUNT;
            default -> PurchaseOrderSortField.CREATED_AT;
        };
    }

    private static SortDirection resolveSortDirection(String sortDirection) {
        if (sortDirection == null) return SortDirection.DESC;
        return SortDirection.valueOf(sortDirection.toUpperCase());
    }
}
