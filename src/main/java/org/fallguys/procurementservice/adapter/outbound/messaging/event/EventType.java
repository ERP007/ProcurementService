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

    INBOUND_STOCK_REQUESTED("inventory.stock.inbound.requested.procurement");

    private final String wire;
}
