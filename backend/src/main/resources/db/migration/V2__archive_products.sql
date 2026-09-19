-- Preserve products referenced by historical stock movements.
ALTER TABLE products ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX products_active_user_idx ON products(user_id) WHERE archived = FALSE;
