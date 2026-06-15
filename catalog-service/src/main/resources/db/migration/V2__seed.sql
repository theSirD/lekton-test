INSERT INTO product (id, name, description)
VALUES ('11111111-1111-1111-1111-111111111111', 'Limited Sneakers', 'Flash drop sneakers');

INSERT INTO sale (id, product_id, price, initial_stock, starts_at, ends_at)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    199.99,
    100,
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '30 days'
);
