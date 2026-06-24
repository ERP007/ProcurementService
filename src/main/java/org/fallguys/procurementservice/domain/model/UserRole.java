package org.fallguys.procurementservice.domain.model;

public enum UserRole {
    ADMIN,
    HQ_MANAGER,
    HQ_STAFF,
    BRANCH_MANAGER,
    BRANCH_STAFF;

    public boolean isHqUser() { return this == ADMIN || this == HQ_MANAGER || this == HQ_STAFF; }
}