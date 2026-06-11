package org.fallguys.procurementservice.domain.exception;

public class BusinessValidationException extends BusinessException {

    public BusinessValidationException(ProcurementErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }

    public BusinessValidationException(ProcurementErrorCode errorCode, String detail) {
        super(errorCode.getCode(), detail);
    }
}
