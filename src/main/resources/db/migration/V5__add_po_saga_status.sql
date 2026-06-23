-- 입고 saga(트랜잭셔널 outbox) 진행 상태 컬럼 추가. 비즈니스 status와 분리.
ALTER TABLE purchase_orders ADD COLUMN saga_status VARCHAR(255) NOT NULL DEFAULT 'NONE';
