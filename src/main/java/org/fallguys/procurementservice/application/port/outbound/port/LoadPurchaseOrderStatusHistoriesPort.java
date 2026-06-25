package org.fallguys.procurementservice.application.port.outbound.port;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PurchaseOrderStatusHistory;

import java.util.List;
import java.util.Optional;

public interface LoadPurchaseOrderStatusHistoriesPort {
    /**
     * 발주서의 상태 변경 이력을 변경 시각 내림차순으로 조회한다.
     */
    List<PurchaseOrderStatusHistory> findByPoCode(String poCode);

    /**
     * 발주서의 특정 상태 이력 중 가장 최근 1건을 조회한다(없으면 empty).
     */
    Optional<PurchaseOrderStatusHistory> findLatestByPoCodeAndStatus(String poCode, PurchaseOrderStatus status);
}
