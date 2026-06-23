package org.fallguys.procurementservice.adapter.outbound.persistence.purchaseorder;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;

public interface PurchaseOrderJpaDao extends JpaRepository<PurchaseOrderEntity, String>,
        JpaSpecificationExecutor<PurchaseOrderEntity> {
    long countByStatusNot(PurchaseOrderStatus status);
    long countByStatus(PurchaseOrderStatus status);
    long countByStatusAndDesiredArrivalDateBefore(PurchaseOrderStatus status, LocalDate date);
}
