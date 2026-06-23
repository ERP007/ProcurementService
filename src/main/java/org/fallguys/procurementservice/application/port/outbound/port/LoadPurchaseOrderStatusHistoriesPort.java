package org.fallguys.procurementservice.application.port.outbound.port;

import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PurchaseOrderStatusHistory;

import java.util.List;

public interface LoadPurchaseOrderStatusHistoriesPort {
    /**
     * 발주서의 상태 변경 이력을 변경 시각 내림차순으로 조회한다.
     */
    List<PurchaseOrderStatusHistory> findByPoCode(String poCode);
}
