-- Approved Milestone 1 schema. PostgreSQL owns structure; Hibernate validates it.
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL CHECK (btrim(name) <> ''),
    email VARCHAR(254) NOT NULL CHECK (btrim(email) <> ''),
    password VARCHAR(255) NOT NULL CHECK (btrim(password) <> ''),
    business_name VARCHAR(160) NOT NULL CHECK (btrim(business_name) <> ''),
    preferred_language VARCHAR(20) NOT NULL DEFAULT 'en' CHECK (btrim(preferred_language) <> ''),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX users_email_unique ON users (lower(email));

CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name VARCHAR(160) NOT NULL CHECK (btrim(name) <> ''),
    category VARCHAR(100) NOT NULL CHECK (btrim(category) <> ''),
    unit VARCHAR(20) NOT NULL CHECK (unit IN ('pieces','kg','grams','bags','cartons','boxes','dozens','litres','millilitres','quintals')),
    current_stock NUMERIC(19,3) NOT NULL DEFAULT 0 CHECK (current_stock >= 0),
    minimum_stock NUMERIC(19,3) NOT NULL DEFAULT 0 CHECK (minimum_stock >= 0),
    price NUMERIC(19,2) CHECK (price >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT products_id_user_unique UNIQUE (id, user_id)
);
CREATE INDEX products_user_name_idx ON products(user_id, name);
CREATE INDEX products_user_category_idx ON products(user_id, category);

CREATE TABLE inventory_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    product_id UUID NOT NULL,
    transaction_type VARCHAR(10) NOT NULL CHECK (transaction_type IN ('ADD','REMOVE')),
    quantity NUMERIC(19,3) NOT NULL CHECK (quantity > 0),
    unit VARCHAR(20) NOT NULL CHECK (unit IN ('pieces','kg','grams','bags','cartons','boxes','dozens','litres','millilitres','quintals')),
    source VARCHAR(10) NOT NULL CHECK (source IN ('VOICE','MANUAL')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT transactions_product_owner_fk FOREIGN KEY (product_id, user_id)
        REFERENCES products(id, user_id) ON DELETE RESTRICT
);
CREATE INDEX transactions_user_created_idx ON inventory_transactions(user_id, created_at DESC);
CREATE INDEX transactions_product_created_idx ON inventory_transactions(product_id, created_at DESC);

-- Supabase's browser Data API must not expose these tables (especially passwords).
-- No client policies: all application access goes through the Spring backend's
-- direct database connection as the table owner. JWT authorization comes later.
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_transactions ENABLE ROW LEVEL SECURITY;
