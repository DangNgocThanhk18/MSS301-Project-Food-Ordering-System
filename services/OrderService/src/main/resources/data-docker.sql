-- ============================================================
-- Dữ liệu mẫu cho order_mss301_db (chạy khi profile "docker")
-- user_id / food_id / shipper_id khớp với data-docker.sql của
-- UserService và FoodService (Order Service chỉ lưu id, không có FK).
-- Dùng INSERT IGNORE nên chạy lại nhiều lần không bị lỗi/trùng.
-- ============================================================
INSERT IGNORE INTO orders (id, user_id, total_price, status, created_at, shipping_address, phone, note, shipper_id) VALUES
-- PAID, chưa có shipper  -> hiện trong danh sách "đơn chờ shipper nhận"
(1, 3, 160000.00, 'PAID',       NOW(), '99 Trần Hưng Đạo, Quận 5, TP.HCM',    '0911111111', 'Ít cay, giao giờ trưa',   NULL),
-- DELIVERING, shipper_id = 2
(2, 4,  75000.00, 'DELIVERING', NOW(), '27 Võ Văn Tần, Quận 3, TP.HCM',       '0922222222', NULL,                      2),
-- CREATED, chưa thanh toán
(3, 5, 146000.00, 'CREATED',    NOW(), '8 Phan Xích Long, Phú Nhuận, TP.HCM', '0933333333', 'Gọi trước khi giao',      NULL),
-- CANCELLED
(4, 3,  98000.00, 'CANCELLED',  NOW(), '99 Trần Hưng Đạo, Quận 5, TP.HCM',    '0911111111', 'Khách đổi ý',             NULL);

INSERT IGNORE INTO order_items (id, order_id, food_id, food_name, quantity, unit_price) VALUES
(1, 1, 1,  'Cơm tấm sườn bì chả', 2, 45000.00),
(2, 1, 8,  'Trà đào cam sả',      2, 35000.00),
(3, 2, 2,  'Phở bò tái',          1, 55000.00),
(4, 2, 12, 'Bánh flan caramel',   1, 20000.00),
(5, 3, 5,  'Gà rán (2 miếng)',    1, 59000.00),
(6, 3, 7,  'Khoai tây chiên',     1, 29000.00),
(7, 3, 9,  'Cà phê sữa đá',       2, 29000.00),
(8, 4, 6,  'Hamburger bò phô mai',2, 49000.00);

INSERT IGNORE INTO invoices (id, order_id, invoice_number, amount, payment_method, status, issued_at) VALUES
(1, 1, 'INV-DEMO0001', 160000.00, 'CASH',  'PAID', NOW()),
(2, 2, 'INV-DEMO0002',  75000.00, 'VNPAY', 'PAID', NOW());
