package org.fallguys.procurementservice.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProcurementErrorCode implements ErrorCode {

    PURCHASE_ORDER_NOT_FOUND("PO-001", "발주서를 찾을 수 없습니다."),
    VENDOR_NOT_FOUND("PO-002", "활성 공급사를 찾을 수 없습니다."),
    VENDOR_NOT_FOUND_ON_DETAIL("PO-003", "공급사를 찾을 수 없습니다."),
    WAREHOUSE_NOT_FOUND("PO-004", "창고를 찾을 수 없습니다."),
    ITEM_NOT_FOUND("PO-005", "존재하지 않는 품목이 포함되어 있습니다."),

    VALIDATION_FAILED("PO-006", "요청 형식이 올바르지 않습니다."),
    DESIRED_ARRIVAL_DATE_TOO_FAR("PO-007", "도착 희망일은 오늘로부터 1년 이내여야 합니다."),
    DUPLICATE_ITEM_CODE("PO-008", "동일한 품목 코드가 중복되어 있습니다."),
    ITEM_INACTIVE("PO-009", "비활성 품목이 포함되어 있습니다."),
    WAREHOUSE_INACTIVE("PO-010", "비활성 창고입니다."),
    PURCHASE_ORDER_NOT_DRAFT("PO-011", "DRAFT 상태인 발주서만 수정할 수 있습니다."),
    PURCHASE_ORDER_NOT_APPROVED("PO-012", "APPROVED 상태인 발주서만 입고 처리할 수 있습니다."),
    PURCHASE_ORDER_NOT_CANCELABLE("PO-013", "DRAFT 또는 APPROVED 상태인 발주서만 취소할 수 있습니다."),

    INVALID_QUERY_SIZE("PO-014", "size는 10, 20, 50 중 하나여야 합니다."),
    INVALID_QUERY_STATUS("PO-015", "유효하지 않은 status입니다."),
    INVALID_QUERY_DATE_RANGE("PO-016", "시작일은 종료일보다 이전이어야 합니다."),
    INVALID_QUERY_DATE_PERIOD("PO-017", "조회 기간은 최대 365일입니다."),

    INVENTORY_INBOUND_FAILED("PO-018", "재고 입고 처리에 실패했습니다."),

    EMPTY_PURCHASE_ORDER_LINE("PO-019", "발주 라인은 1개 이상이어야 합니다."),
    DESIRED_ARRIVAL_DATE_IN_PAST("PO-020", "도착 희망일은 오늘 이후여야 합니다."),
    PURCHASE_ORDER_CONFLICT("PO-021", "다른 사용자가 먼저 발주서를 변경했습니다. 다시 시도해 주세요.");

    private final String code;
    private final String message;
}
