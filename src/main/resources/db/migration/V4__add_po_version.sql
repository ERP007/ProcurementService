-- 발주서 동시 수정 충돌 감지를 위한 낙관적 락(@Version) 컬럼 추가.
ALTER TABLE purchase_orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
