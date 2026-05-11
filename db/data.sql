USE `dms`;

-- 插入 user 测试数据 (role: 1学员, 2教练, 3管理员)
INSERT INTO `user` (`username`, `password`, `role`, `phone`) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', 3, '13800138000'),
('教练张', 'e10adc3949ba59abbe56e057f20f883e', 2, '13900139001'),
('教练李', 'e10adc3949ba59abbe56e057f20f883e', 2, '13900139002'),
('学员王', 'e10adc3949ba59abbe56e057f20f883e', 1, '13700137001'),
('学员赵', 'e10adc3949ba59abbe56e057f20f883e', 1, '13700137002');

-- 插入 registration 测试数据
INSERT INTO `registration` (`student_id`, `package_type`, `status`, `fee`, `id_card_front`, `id_card_back`, `health_cert`, `audit_remark`, `generated_reg_form`, `generated_health_form`, `coach_preference`) VALUES
(4, 'VIP快班', 3, 5000.00, '/files/id_front_1.jpg', '/files/id_back_1.jpg', '/files/health_1.jpg', '审核通过', '/files/forms/reg_1.pdf', '/files/forms/health_1.pdf', '希望是个性格随和的女教练'),
(5, '普通班', 2, 3500.00, '/files/id_front_2.jpg', '/files/id_back_2.jpg', '/files/health_2.jpg', '材料齐全，等待缴费', '/files/forms/reg_2.pdf', '/files/forms/health_2.pdf', '无特别要求'),
(4, '补考套餐', 1, 500.00, null, null, null, null, null, null, null),
(5, '周末班', 4, 4000.00, '/files/id_front_3.jpg', '/files/id_back_3.jpg', null, '缺少体检证明，请补充', null, null, '周末有空'),
(4, '普通班', 1, 3500.00, null, null, null, null, null, null, null);

-- 插入 coach_assignment 测试数据
INSERT INTO `coach_assignment` (`student_id`, `coach_id`, `status`) VALUES
(4, 2, 1),
(5, 3, 1),
(4, 3, 2),
(5, 2, 2),
(4, 2, 1);

-- 插入 exam_score 测试数据
INSERT INTO `exam_score` (`student_id`, `subject`, `score`, `is_passed`, `exam_date`) VALUES
(4, 1, 95, 1, '2023-10-01'),
(4, 2, 85, 1, '2023-11-01'),
(5, 1, 80, 1, '2023-10-15'),
(5, 2, 70, 0, '2023-11-15'),
(4, 3, 90, 1, '2023-12-01');

-- 插入 learning_progress 测试数据
INSERT INTO `learning_progress` (`student_id`, `subject`, `total_hours`, `completed_hours`, `status`) VALUES
(4, 1, 12, 12, 2),
(4, 2, 16, 16, 2),
(4, 3, 24, 10, 1),
(5, 1, 12, 12, 2),
(5, 2, 16, 5, 1);

-- 插入 training_site 测试数据
INSERT INTO `training_site` (`name`, `address`, `capacity`) VALUES
('驾校第一训练场', '高新区科园大道1号', 50),
('驾校第二训练场', '高新区科园大道2号', 30),
('科目二专属考场', '大学城南路88号', 100),
('科目三路考起点', '大学城北路1号', 20),
('模拟综合训练场', '中心区建设路5号', 40);

-- 插入 exam_appointment 测试数据
INSERT INTO `exam_appointment` (`student_id`, `subject`, `site_id`, `exam_date`, `status`) VALUES
(4, 1, 1, '2023-09-28 09:00:00', 2),
(4, 2, 3, '2023-10-30 14:00:00', 2),
(5, 1, 1, '2023-10-10 09:00:00', 2),
(5, 2, 3, '2023-11-20 14:00:00', 1),
(4, 3, 4, '2023-12-15 09:00:00', 1);

-- 插入 vehicle 测试数据
INSERT INTO `vehicle` (`plate_number`, `brand`, `status`) VALUES
('粤A12345学', '大众桑塔纳', 1),
('粤A12346学', '大众桑塔纳', 1),
('粤A12347学', '丰田卡罗拉', 1),
('粤A12348学', '大众桑塔纳', 2),
('粤A12349学', '大众捷达', 3);
