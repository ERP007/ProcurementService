package org.fallguys.procurementservice.application.port.inbound.command;

public record ApprovePurchaseOrderCommand(
        String code,
        String userCode
) {}
