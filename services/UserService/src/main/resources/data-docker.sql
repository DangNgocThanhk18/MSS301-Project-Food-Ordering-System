-- ============================================================
-- Dữ liệu mẫu cho user_mss301_db (chạy khi profile "docker")
-- Dùng INSERT IGNORE nên chạy lại nhiều lần không bị lỗi/trùng.
-- Mật khẩu của TẤT CẢ tài khoản mẫu: password123  (đã BCrypt)
-- ============================================================
INSERT IGNORE INTO users (user_id, name, active, email, phone, address, created_at, password, role) VALUES
(1, 'Admin Hệ Thống',   b'1', 'admin@foodorder.com',    '0900000001', '12 Nguyễn Huệ, Quận 1, TP.HCM',        NOW(), '$2a$10$pv44AKPJNMNgaNLyzqD6HOOiJp51Ke0edM2zw4r16uqpCBWMsBNJa', 'ADMIN'),
(2, 'Trần Văn Shipper', b'1', 'shipper@foodorder.com',  '0900000002', '45 Lê Lợi, Quận 1, TP.HCM',            NOW(), '$2a$10$pv44AKPJNMNgaNLyzqD6HOOiJp51Ke0edM2zw4r16uqpCBWMsBNJa', 'SHIPPER'),
(3, 'Nguyễn Thị Lan',   b'1', 'lan@example.com',        '0911111111', '99 Trần Hưng Đạo, Quận 5, TP.HCM',     NOW(), '$2a$10$pv44AKPJNMNgaNLyzqD6HOOiJp51Ke0edM2zw4r16uqpCBWMsBNJa', 'CUSTOMER'),
(4, 'Lê Minh Khoa',     b'1', 'khoa@example.com',       '0922222222', '27 Võ Văn Tần, Quận 3, TP.HCM',        NOW(), '$2a$10$pv44AKPJNMNgaNLyzqD6HOOiJp51Ke0edM2zw4r16uqpCBWMsBNJa', 'CUSTOMER'),
(5, 'Phạm Thu Hà',      b'1', 'ha@example.com',         '0933333333', '8 Phan Xích Long, Phú Nhuận, TP.HCM',  NOW(), '$2a$10$pv44AKPJNMNgaNLyzqD6HOOiJp51Ke0edM2zw4r16uqpCBWMsBNJa', 'CUSTOMER'),
(6, 'Tài Khoản Bị Khóa', b'0', 'locked@example.com',    '0944444444', '1 Hai Bà Trưng, Quận 1, TP.HCM',       NOW(), '$2a$10$pv44AKPJNMNgaNLyzqD6HOOiJp51Ke0edM2zw4r16uqpCBWMsBNJa', 'CUSTOMER');
