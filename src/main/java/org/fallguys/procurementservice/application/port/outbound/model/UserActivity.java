package org.fallguys.procurementservice.application.port.outbound.model;

import java.time.Instant;

/**
 * 발행할 사용자 활동 한 건. 활동 목록 화면에 매핑된다.
 *
 * employeeNo : 수행자 사번. null이면 어댑터가 "SYSTEM"으로 채운다.
 * type       : 활동 종류(action·배지 매핑 보유).
 * title      : 목록 주요 문구(발주 code).
 * content    : 상세 문구(공급사명 등). 없으면 null.
 * occurredAt : 발생 시각. envelope.occurredAt과 동일 값으로 실린다.
 */
public record UserActivity(
        String employeeNo,
        UserActivityType type,
        String title,
        String content,
        Instant occurredAt
) {}
