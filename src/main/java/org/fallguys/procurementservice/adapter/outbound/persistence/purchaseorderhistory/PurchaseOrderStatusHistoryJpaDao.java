package org.fallguys.procurementservice.adapter.outbound.persistence.purchaseorderhistory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderStatusHistoryJpaDao extends JpaRepository<PurchaseOrderStatusHistoryEntity, Long> {
    List<PurchaseOrderStatusHistoryEntity> findByPoCodeOrderByCreatedAtDesc(String poCode);
}
