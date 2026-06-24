package org.fallguys.procurementservice.adapter.outbound.persistence.pendingstatuschange;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingStatusChangeJpaDao extends JpaRepository<PendingStatusChangeEntity, String> {
}
