package org.fallguys.procurementservice.adapter.outbound.persistence.purchaseorderhistory;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderStatusHistoryJpaDao extends JpaRepository<PurchaseOrderStatusHistoryEntity, Long> {
    List<PurchaseOrderStatusHistoryEntity> findByPoCodeOrderByCreatedAtDesc(String poCode);

    Optional<PurchaseOrderStatusHistoryEntity> findFirstByPoCodeAndStatusOrderByCreatedAtDesc(
            String poCode, PurchaseOrderStatus status);
}
