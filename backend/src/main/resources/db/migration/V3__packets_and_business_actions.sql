ALTER TABLE products DROP CONSTRAINT products_unit_check;
ALTER TABLE products ADD CONSTRAINT products_unit_check CHECK (unit IN ('pieces','kg','grams','bags','cartons','boxes','dozens','litres','millilitres','quintals','packets'));
ALTER TABLE inventory_transactions DROP CONSTRAINT inventory_transactions_unit_check;
ALTER TABLE inventory_transactions ADD CONSTRAINT inventory_transactions_unit_check CHECK (unit IN ('pieces','kg','grams','bags','cartons','boxes','dozens','litres','millilitres','quintals','packets'));

CREATE TABLE business_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    product_id UUID NOT NULL,
    action_type VARCHAR(20) NOT NULL CHECK (action_type = 'REORDER'),
    recommended_quantity NUMERIC(19,3) NOT NULL CHECK (recommended_quantity > 0),
    unit VARCHAR(20) NOT NULL CHECK (unit IN ('pieces','kg','grams','bags','cartons','boxes','dozens','litres','millilitres','quintals','packets')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('RECOMMENDED','APPROVED','COMPLETED','CANCELLED')),
    reason VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    CONSTRAINT actions_product_owner_fk FOREIGN KEY (product_id,user_id) REFERENCES products(id,user_id),
    CONSTRAINT actions_approval_time CHECK (status NOT IN ('APPROVED','COMPLETED') OR approved_at IS NOT NULL),
    CONSTRAINT actions_completion_time CHECK ((status = 'COMPLETED') = (completed_at IS NOT NULL))
);
CREATE UNIQUE INDEX actions_one_approved_per_product ON business_actions(user_id,product_id) WHERE status='APPROVED';
CREATE INDEX actions_owner_created ON business_actions(user_id,created_at DESC);
ALTER TABLE business_actions ENABLE ROW LEVEL SECURITY;
