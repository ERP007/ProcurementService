package org.fallguys.procurementservice.adapter.outbound.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxJpaDao extends JpaRepository<OutboxEntity, UUID> {

    // 폴러용: 가장 오래된 PENDING 100건을 created_at 오름차순으로 가져온다(발행 순서 보존).
    List<OutboxEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
