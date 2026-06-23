-- 트랜잭셔널 outbox 테이블. 비즈니스 트랜잭션과 같은 커밋으로 이벤트를 적재한다.
CREATE TABLE outbox (
    event_id     UUID NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type   VARCHAR(255) NOT NULL,
    payload      TEXT NOT NULL,
    status       VARCHAR(255) NOT NULL,
    retry_count  INTEGER NOT NULL DEFAULT 0,
    created_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP(6) WITH TIME ZONE,
    PRIMARY KEY (event_id)
);

-- 폴러 조회(status='PENDING' ORDER BY created_at) 대응 인덱스.
CREATE INDEX idx_outbox_status_created_at ON outbox (status, created_at);
