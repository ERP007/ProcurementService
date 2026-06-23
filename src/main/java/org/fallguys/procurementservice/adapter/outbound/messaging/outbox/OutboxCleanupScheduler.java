package org.fallguys.procurementservice.adapter.outbound.messaging.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * outbox 정리 스케줄러. 발행 완료(PUBLISHED)되고 보존 기한이 지난 행을 새벽에 벌크 삭제한다.
 * PENDING(미발행)·FAILED(보상/조사 대상)는 보존한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxCleanupScheduler {

    private static final int RETENTION_DAYS = 3;

    private final OutboxJpaDao outboxJpaDao;

    @Scheduled(cron = "${outbox.cleanup.cron:0 0 4 * * *}", zone = "Asia/Seoul")
    @Transactional
    public void cleanup() {
        Instant threshold = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
        int deleted = outboxJpaDao.deleteByStatusAndPublishedAtBefore(OutboxStatus.PUBLISHED, threshold);
        if (deleted > 0) {
            log.info("outbox 정리 완료 deleted={} threshold={}", deleted, threshold);
        }
    }
}
