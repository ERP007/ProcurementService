package org.fallguys.procurementservice.adapter.outbound.persistence;

import org.fallguys.procurementservice.domain.model.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface PurchaseOrderJpaDao extends JpaRepository<PurchaseOrderEntity, String> {
    long countByStatusNot(PurchaseOrderStatus status);
    long countByStatus(PurchaseOrderStatus status);
    long countByStatusAndDesiredArrivalDateBefore(PurchaseOrderStatus status, LocalDate date);
}
