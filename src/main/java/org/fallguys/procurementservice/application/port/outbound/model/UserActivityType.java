package org.fallguys.procurementservice.application.port.outbound.model;

/**
 * 발주 사용자 활동 종류. user 서비스 활동 이력으로 발행할 때 쓰는 매핑.
 *
 * action : producer 원본 액션 문자열(user 서비스가 한글 라벨로 변환).
 */
public enum UserActivityType {

    CREATED("PROCUREMENT_ORDER_CREATED"),
    UPDATED("PROCUREMENT_ORDER_UPDATED"),
    APPROVED("PROCUREMENT_ORDER_STATUS_CHANGED"),
    CANCELED("PROCUREMENT_ORDER_STATUS_CHANGED"),
    RECEIVED("PROCUREMENT_ORDER_STATUS_CHANGED");

    private final String action;

    UserActivityType(String action) {
        this.action = action;
    }

    public String action() {
        return action;
    }
}
