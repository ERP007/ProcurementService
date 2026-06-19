package org.fallguys.procurementservice.domain.model.purchaseorderhistory;

import java.time.LocalDate;

/**
 * RECEIVED 전이의 부가 데이터. 실제 입고일(receivedDate)을 보관한다.
 */
public record ReceivingPayload(LocalDate receivedDate) implements StatusChangePayload {
}
