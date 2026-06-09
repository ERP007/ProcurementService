package org.fallguys.procurementservice.domain.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(ProcurementErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }
}
