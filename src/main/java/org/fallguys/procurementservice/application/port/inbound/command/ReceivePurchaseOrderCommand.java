package org.fallguys.procurementservice.application.port.inbound.command;

import java.time.LocalDate;

// userName·userPosition: 입고 실행자 표시 정보(JWT claim). 없을 수 있어 nullable.
public record ReceivePurchaseOrderCommand(
        String code, String userCode, String userName, String userPosition, LocalDate receivedDate) {}
