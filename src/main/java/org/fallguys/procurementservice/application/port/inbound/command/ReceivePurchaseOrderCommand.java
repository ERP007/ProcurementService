package org.fallguys.procurementservice.application.port.inbound.command;

import java.time.LocalDate;

// userName: 입고 실행자 이름(JWT name claim). 없을 수 있어 nullable.
public record ReceivePurchaseOrderCommand(String code, String userCode, String userName, LocalDate receivedDate) {}
