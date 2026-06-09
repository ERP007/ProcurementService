package org.fallguys.procurementservice.domain.exception;

public class ForbiddenException extends BusinessException {

    public ForbiddenException(ProcurementErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }
}
