package org.fallguys.procurementservice.adapter.outbound.messaging.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 발행 이벤트 종류. wire(메시지 본문/라우팅에 실리는 문자열)를 보유한다.
 * wire 문자열은 inventory 서비스의 소비 명세에 맞춘다.
 */
@Getter
@RequiredArgsConstructor
public enum EventType {

    INBOUND_STOCK_REQUESTED("inventory.stock.inbound.requested.procurement"),

    // user 서비스 활동 이력 수신용. erp.events로 비동기 발행(유실 가능).
    USER_ACTIVITY_OCCURRED("user.activity.occurred");

    private final String wire;
}
