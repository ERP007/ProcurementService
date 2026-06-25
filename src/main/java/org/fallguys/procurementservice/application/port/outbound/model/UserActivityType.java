package org.fallguys.procurementservice.application.port.outbound.model;

/**
 * 발주 사용자 활동 종류. user 서비스 활동 이력으로 발행할 때 쓰는 매핑.
 *
 * action : producer 원본 액션 문자열(user 서비스가 한글 라벨로 변환).
 * badge  : 활동 목록 배지 문구(status 필드로 그대로 전달).
 */
public enum UserActivityType {

    CREATED("PROCUREMENT_ORDER_CREATED", "발주 요청"),
    UPDATED("PROCUREMENT_ORDER_UPDATED", "발주 수정"),
    APPROVED("PROCUREMENT_ORDER_STATUS_CHANGED", "발주 승인"),
    CANCELED("PROCUREMENT_ORDER_STATUS_CHANGED", "발주 취소"),
    RECEIVED("PROCUREMENT_ORDER_STATUS_CHANGED", "입고 완료");

    private final String action;
    private final String badge;

    UserActivityType(String action, String badge) {
        this.action = action;
        this.badge = badge;
    }

    public String action() {
        return action;
    }

    public String badge() {
        return badge;
    }
}
