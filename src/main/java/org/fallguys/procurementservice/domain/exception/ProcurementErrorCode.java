package org.fallguys.procurementservice.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProcurementErrorCode {

    UNAUTHORIZED("PO-01-01", "인증 정보가 유효하지 않습니다."),
    FORBIDDEN("PO-01-02", "접근 권한이 없습니다."),

    VENDOR_NOT_FOUND("PO-02-01", "공급사를 찾을 수 없습니다."),
    WAREHOUSE_NOT_FOUND("PO-02-02", "창고를 찾을 수 없습니다."),
    ITEM_NOT_FOUND("PO-02-03", "존재하지 않는 품목이 포함되어 있습니다."),
    PURCHASE_ORDER_NOT_FOUND("PO-02-04", "발주서를 찾을 수 없습니다."),

    DUPLICATE_ITEM_CODE("PO-03-01", "동일한 품목 코드가 중복되어 있습니다."),
    DESIRED_ARRIVAL_DATE_TOO_FAR("PO-03-02", "도착 희망일은 오늘로부터 1년 이내여야 합니다."),
    ITEM_INACTIVE("PO-03-03", "비활성 품목이 포함되어 있습니다."),
    WAREHOUSE_INACTIVE("PO-03-04", "비활성 창고입니다."),
    PURCHASE_ORDER_NOT_DRAFT("PO-03-05", "DRAFT 상태인 발주서만 수정할 수 있습니다."),
    INVALID_QUERY_PARAMETER("PO-03-06", "유효하지 않은 조회 파라미터입니다."),

    ITEM_SERVICE_ERROR("PO-07-01", "아이템 서비스 호출에 실패했습니다."),
    INVENTORY_SERVICE_ERROR("PO-07-02", "재고 서비스 호출에 실패했습니다.");

    private final String code;
    private final String message;
}
