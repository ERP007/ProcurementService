package org.fallguys.procurementservice.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderJpaDao extends JpaRepository<PurchaseOrderEntity, String> {
}
