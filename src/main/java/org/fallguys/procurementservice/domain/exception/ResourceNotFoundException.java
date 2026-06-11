package org.fallguys.procurementservice.domain.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }
}
