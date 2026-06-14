CREATE TABLE sale_stock (
    sale_id UUID PRIMARY KEY,
    initial_stock INT NOT NULL CHECK (initial_stock > 0),
    sold INT NOT NULL DEFAULT 0 CHECK (sold >= 0),
    CONSTRAINT sale_stock_invariant CHECK (sold <= initial_stock)
);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    status VARCHAR(32) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_sale ON orders(sale_id);
CREATE INDEX idx_orders_status ON orders(status);

CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox(published_at) WHERE published_at IS NULL;
