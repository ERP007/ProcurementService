package org.fallguys.procurementservice.domain.model.purchaseorderhistory;

/**
 * 상태를 변경한 행위자. code + 행위 시점 박제된 표시 정보(name·position).
 *
 * 행위자는 "누가 했나"라는 불변 사실이므로 DRAFT를 포함한 모든 행위 시점에 박제한다.
 * (변동 master인 벤더명·창고명이 확정 시점에만 박제되는 것과 다르다.)
 * 신뢰 토큰(JWT)에서 추출한 값을 고정하며, 조회 시 외부 호출 없이 그대로 사용한다.
 *
 * code     : 담당자 식별자(사번 등).
 * name     : 행위 시점 이름(JWT name claim). 없을 수 있어 nullable.
 * position : 행위 시점 직급(JWT position claim). 없을 수 있어 nullable.
 */
public record ActorRef(String code, String name, String position) {

    // 시스템 자동 행위(보상 등)용. 표시 정보 없음.
    public static ActorRef system(String code) {
        return new ActorRef(code, null, null);
    }
}
