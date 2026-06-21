-- purchase_order_lines 의 snapshot 컬럼 NOT NULL 제약 제거 (ERP-267).

ALTER TABLE purchase_order_lines ALTER COLUMN item_name_snapshot DROP NOT NULL;
ALTER TABLE purchase_order_lines ALTER COLUMN unit_snapshot DROP NOT NULL;
