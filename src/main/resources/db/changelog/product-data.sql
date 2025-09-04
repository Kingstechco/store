-- Insert sample products
INSERT INTO product (description) VALUES
    ('Laptop Computer'),
    ('Wireless Mouse'),
    ('Mechanical Keyboard'),
    ('USB-C Cable'),
    ('External Monitor'),
    ('Laptop Stand'),
    ('Webcam HD'),
    ('Bluetooth Headphones'),
    ('Portable Charger'),
    ('Desk Lamp');

-- Associate products with orders
-- Order 1 gets Laptop, Mouse, Keyboard
INSERT INTO order_product (order_id, product_id) 
SELECT 1, id FROM product WHERE description IN ('Laptop Computer', 'Wireless Mouse', 'Mechanical Keyboard');

-- Order 2 gets USB-C Cable, Monitor
INSERT INTO order_product (order_id, product_id) 
SELECT 2, id FROM product WHERE description IN ('USB-C Cable', 'External Monitor');

-- Order 3 gets Webcam, Headphones  
INSERT INTO order_product (order_id, product_id) 
SELECT 3, id FROM product WHERE description IN ('Webcam HD', 'Bluetooth Headphones');

-- Order 4 gets Laptop Stand, Charger, Desk Lamp
INSERT INTO order_product (order_id, product_id) 
SELECT 4, id FROM product WHERE description IN ('Laptop Stand', 'Portable Charger', 'Desk Lamp');