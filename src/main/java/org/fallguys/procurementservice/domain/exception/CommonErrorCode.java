package org.fallguys.procurementservice.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    FORBIDDEN("ER-403", "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR("ER-500", "서버 내부 오류가 발생했습니다."),
    EXTERNAL_SERVICE_ERROR("ER-502", "일시적으로 서비스를 이용할 수 없습니다.");

    private final String code;
    private final String message;
}
