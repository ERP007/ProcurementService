-- saga in-flight 상태 전환 staging 테이블. po_code가 PK(애그리거트당 동시 진행 saga 1건).
-- 확정(DONE) 시 이력으로 승격 후 삭제, 실패(FAILED) 시 이력 없이 삭제된다.
CREATE TABLE pending_status_change (
    po_code                  VARCHAR(255) NOT NULL,
    status                   VARCHAR(255) NOT NULL,
    actor_code               VARCHAR(255) NOT NULL,
    actor_name_snapshot      VARCHAR(255),
    actor_position_snapshot  VARCHAR(255),
    payload                  JSONB,
    occurred_at              TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    PRIMARY KEY (po_code)
);

-- 입고 saga 보상 시 박제하는 직전 실패 사유 컬럼. 진행 페이지 폴링 응답 노출용(nullable).
ALTER TABLE purchase_orders ADD COLUMN last_failure_reason TEXT;
