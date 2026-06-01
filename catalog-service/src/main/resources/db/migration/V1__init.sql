CREATE TABLE product (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT
);

CREATE TABLE sale (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES product(id),
    price NUMERIC(12, 2) NOT NULL,
    initial_stock INT NOT NULL CHECK (initial_stock > 0),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT sale_time_range CHECK (ends_at > starts_at)
);

CREATE INDEX idx_sale_product ON sale(product_id);
