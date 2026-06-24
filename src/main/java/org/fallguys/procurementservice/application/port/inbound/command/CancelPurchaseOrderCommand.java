package org.fallguys.procurementservice.application.port.inbound.command;

public record CancelPurchaseOrderCommand(
        String code, String userCode, String userName, String userPosition, String reason) {
}
