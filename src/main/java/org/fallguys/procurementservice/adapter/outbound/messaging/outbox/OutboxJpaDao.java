package org.fallguys.procurementservice.adapter.outbound.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxJpaDao extends JpaRepository<OutboxEntity, UUID> {

    // 폴러용: 가장 오래된 PENDING 100건을 created_at 오름차순으로 가져온다(발행 순서 보존).
    List<OutboxEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    // 정리용: 보존 기한 지난 발행 완료 행을 벌크 삭제한다. 반환값은 삭제 건수.
    @Modifying
    @Query("DELETE FROM OutboxEntity o WHERE o.status = :status AND o.publishedAt < :threshold")
    int deleteByStatusAndPublishedAtBefore(@Param("status") OutboxStatus status,
                                           @Param("threshold") Instant threshold);
}
