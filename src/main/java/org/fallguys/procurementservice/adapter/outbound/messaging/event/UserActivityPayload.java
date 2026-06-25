package org.fallguys.procurementservice.adapter.outbound.messaging.event;

import java.time.Instant;

/**
 * user 서비스 활동 이력 payload. 최근 활동 목록 화면에 매핑된다.
 *
 * employeeNo : 활동 수행자 사번. 시스템 작업이면 "SYSTEM".
 * action     : 활동 종류(PROCUREMENT_ORDER_CREATED 등). producer 원본 액션.
 * occurredAt : 발생 시각. envelope.occurredAt과 같은 값.
 * title      : 목록 주요 문구(발주 code).
 * content    : 주요 문구 뒤 상세(공급사명). 없으면 null.
 * status     : 목록 배지 문구(활동 결과 발주 상태 라벨). 없으면 null.
 */
public record UserActivityPayload(
        String employeeNo,
        String action,
        Instant occurredAt,
        String title,
        String content,
        String status
) {
    public static final String SYSTEM = "SYSTEM";
}
