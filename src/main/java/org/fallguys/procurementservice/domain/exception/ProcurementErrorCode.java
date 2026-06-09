package org.fallguys.procurementservice.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProcurementErrorCode {

    UNAUTHORIZED("PO-01-01", "인증 정보가 유효하지 않습니다."),
    FORBIDDEN("PO-01-02", "접근 권한이 없습니다.");

    private final String code;
    private final String message;
}
