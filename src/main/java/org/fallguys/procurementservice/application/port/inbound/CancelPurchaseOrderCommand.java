package org.fallguys.procurementservice.application.port.inbound;

public record CancelPurchaseOrderCommand(String code, String userCode, String reason) {
}
