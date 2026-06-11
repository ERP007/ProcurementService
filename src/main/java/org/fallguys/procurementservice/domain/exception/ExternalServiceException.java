package org.fallguys.procurementservice.domain.exception;

public class ExternalServiceException extends RuntimeException {

    private final String code;

    public ExternalServiceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
