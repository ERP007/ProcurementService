package org.fallguys.procurementservice.domain.model.purchaseorderhistory;

/**
 * CANCELED 전이의 부가 데이터. 취소 사유(cancelReason)를 보관한다.
 */
public record CancellationPayload(String cancelReason) implements StatusChangePayload {
}
