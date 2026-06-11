package org.fallguys.procurementservice.domain.exception;

public class BusinessValidationException extends BusinessException {

    public BusinessValidationException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }

    public BusinessValidationException(ErrorCode errorCode, String detail) {
        super(errorCode.getCode(), detail);
    }
}
