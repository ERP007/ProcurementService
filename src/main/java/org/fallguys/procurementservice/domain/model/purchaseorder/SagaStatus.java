package org.fallguys.procurementservice.domain.model.purchaseorder;

public enum SagaStatus {
    NONE,
    SENDING,
    PROCESSING,
    DONE,
    FAILED;

    public boolean inProgress() { return this == SENDING || this == PROCESSING; }

    public boolean applied() { return this == DONE || this == FAILED; }
}
