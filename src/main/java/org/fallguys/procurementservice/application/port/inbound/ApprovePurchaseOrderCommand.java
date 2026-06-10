package org.fallguys.procurementservice.application.port.inbound;

public record ApprovePurchaseOrderCommand(
        String code,
        String userCode
) {}
