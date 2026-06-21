package org.fallguys.procurementservice.domain.model.purchaseorderhistory;

/**
 * 상태 변경에 따라오는 상태별 부가 데이터.
 *
 * DRAFT·APPROVED 처럼 부가 데이터가 없는 전이는 payload가 null이며,
 * RECEIVED·CANCELED 처럼 추가 정보가 필요한 전이만 구현체를 갖는다.
 * 직렬화(JSON) 변환은 어댑터가 담당하고 도메인은 값만 다룬다.
 */
public sealed interface StatusChangePayload
        permits ReceivingPayload, CancellationPayload {
}
