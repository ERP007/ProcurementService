package org.fallguys.procurementservice.domain.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(ProcurementErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }

    public ResourceNotFoundException(ProcurementErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }
}
