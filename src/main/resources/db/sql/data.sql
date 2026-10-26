INSERT INTO category (name, description)
SELECT 'Electronics', 'Gadgets and devices'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = 'Electronics');

INSERT INTO category (name, description)
SELECT 'Books', 'Fiction and non-fiction'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = 'Books');

INSERT INTO product (name, price, description, category_id)
SELECT 'Laptop Pro', 1299.99, 'High-performance laptop', c.id
FROM category c
WHERE c.name = 'Electronics'
  AND NOT EXISTS (SELECT 1 FROM product WHERE name = 'Laptop Pro');

INSERT INTO product (name, price, description, category_id)
SELECT 'Clean Code', 34.99, 'A handbook of agile software craftsmanship', c.id
FROM category c
WHERE c.name = 'Books'
  AND NOT EXISTS (SELECT 1 FROM product WHERE name = 'Clean Code');

INSERT INTO orders (reference, total_amount, status, product_id)
SELECT 'ORD-0001', 1299.99, 'PENDING', p.id
FROM product p
WHERE p.name = 'Laptop Pro'
  AND NOT EXISTS (SELECT 1 FROM orders WHERE reference = 'ORD-0001');

INSERT INTO orders (reference, total_amount, status, product_id)
SELECT 'ORD-0002', 34.99, 'SHIPPED', p.id
FROM product p
WHERE p.name = 'Clean Code'
  AND NOT EXISTS (SELECT 1 FROM orders WHERE reference = 'ORD-0002');

INSERT INTO report (title, content, score)
SELECT 'Spring Xpose Launch Report', 'Initial demo report seeded at startup', 4.8
WHERE NOT EXISTS (SELECT 1 FROM report WHERE title = 'Spring Xpose Launch Report');


-- ── Filter demo data (@ExposeField) ───────────────────────────────────────────
-- A product without category or tags, so ?categoryId_is_null=true and ?tags_is_null=true match something
INSERT INTO product (name, price, description, category_id)
SELECT 'Mystery Box', 9.99, 'Contents unknown', NULL
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = 'Mystery Box');

-- Product tags (@ElementCollection) for ?tags_in= / ?tags_not_in=
INSERT INTO product_tags (product_id, tag)
SELECT p.id, 'sale' FROM product p
WHERE p.name = 'Laptop Pro'
  AND NOT EXISTS (SELECT 1 FROM product_tags t WHERE t.product_id = p.id AND t.tag = 'sale');

INSERT INTO product_tags (product_id, tag)
SELECT p.id, 'new' FROM product p
WHERE p.name = 'Laptop Pro'
  AND NOT EXISTS (SELECT 1 FROM product_tags t WHERE t.product_id = p.id AND t.tag = 'new');

INSERT INTO product_tags (product_id, tag)
SELECT p.id, 'bestseller' FROM product p
WHERE p.name = 'Clean Code'
  AND NOT EXISTS (SELECT 1 FROM product_tags t WHERE t.product_id = p.id AND t.tag = 'bestseller');

-- A cancelled order for ?status_not_in=CANCELLED
INSERT INTO orders (reference, total_amount, status, product_id)
SELECT 'ORD-0003', 1299.99, 'CANCELLED', p.id
FROM product p
WHERE p.name = 'Laptop Pro'
  AND NOT EXISTS (SELECT 1 FROM orders WHERE reference = 'ORD-0003');
