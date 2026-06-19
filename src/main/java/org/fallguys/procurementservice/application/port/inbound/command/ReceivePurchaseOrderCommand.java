package org.fallguys.procurementservice.application.port.inbound.command;

import java.time.LocalDate;

public record ReceivePurchaseOrderCommand(String code, String userCode, LocalDate receivedDate) {}
