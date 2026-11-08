-- ============================================================
-- Dữ liệu mẫu cho food_mss301_db (chạy khi profile "docker")
-- Dùng INSERT IGNORE nên chạy lại nhiều lần không bị lỗi/trùng.
-- ============================================================
INSERT IGNORE INTO categories (id, name, description) VALUES
(1, 'Món chính',   'Cơm, phở, bún và các món ăn no bụng'),
(2, 'Đồ ăn nhanh', 'Bánh mì, gà rán, hamburger, khoai tây chiên'),
(3, 'Đồ uống',     'Trà, cà phê, nước ép giải khát'),
(4, 'Tráng miệng', 'Chè, bánh ngọt sau bữa ăn');

INSERT IGNORE INTO foods (id, name, description, price, image_url, category_id) VALUES
(1,  'Cơm tấm sườn bì chả', 'Cơm tấm sườn nướng, bì, chả trứng, nước mắm chua ngọt', 45000.00, 'https://placehold.co/400x300?text=Com+Tam',      1),
(2,  'Phở bò tái',          'Phở bò nước dùng hầm xương 12 tiếng, thịt bò tái mềm',  55000.00, 'https://placehold.co/400x300?text=Pho+Bo',       1),
(3,  'Bún chả Hà Nội',      'Bún, chả nướng than hoa, nem rán, nước chấm đậm vị',    50000.00, 'https://placehold.co/400x300?text=Bun+Cha',      1),
(4,  'Bánh mì thịt nướng',  'Bánh mì giòn, thịt nướng, đồ chua, rau thơm',           25000.00, 'https://placehold.co/400x300?text=Banh+Mi',      2),
(5,  'Gà rán (2 miếng)',    'Gà rán giòn rụm kèm sốt chua ngọt',                     59000.00, 'https://placehold.co/400x300?text=Ga+Ran',       2),
(6,  'Hamburger bò phô mai','Bò xay, phô mai tan chảy, rau tươi, sốt đặc biệt',      49000.00, 'https://placehold.co/400x300?text=Hamburger',    2),
(7,  'Khoai tây chiên',     'Khoai tây chiên giòn, rắc muối',                        29000.00, 'https://placehold.co/400x300?text=Khoai+Chien',  2),
(8,  'Trà đào cam sả',      'Trà đào, cam vàng, sả thơm mát lạnh',                   35000.00, 'https://placehold.co/400x300?text=Tra+Dao',      3),
(9,  'Cà phê sữa đá',       'Cà phê phin truyền thống pha sữa đặc',                  29000.00, 'https://placehold.co/400x300?text=Ca+Phe',       3),
(10, 'Nước cam ép',         'Cam tươi vắt nguyên chất, ít đường',                    32000.00, 'https://placehold.co/400x300?text=Nuoc+Cam',     3),
(11, 'Chè khúc bạch',       'Khúc bạch mềm mịn, nhãn, hạt é, nước đường phèn',       30000.00, 'https://placehold.co/400x300?text=Che+Khuc+Bach',4),
(12, 'Bánh flan caramel',   'Bánh flan trứng sữa, caramel đắng nhẹ',                 20000.00, 'https://placehold.co/400x300?text=Banh+Flan',    4);
