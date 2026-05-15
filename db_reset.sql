-- 开启 utf8mb4 支持
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 清理所有旧数据
TRUNCATE TABLE `biz_exams`;
TRUNCATE TABLE `biz_learning_progress`;
TRUNCATE TABLE `biz_instructor_schedule`;
TRUNCATE TABLE `biz_appointments`;
TRUNCATE TABLE `biz_training_records`;
TRUNCATE TABLE `biz_students`;
TRUNCATE TABLE `biz_instructors`;
TRUNCATE TABLE `sys_user_roles`;
TRUNCATE TABLE `sys_users`;
TRUNCATE TABLE `sys_roles`;

-- 2. 初始化核心角色 (补全 4 个字段)
INSERT INTO `sys_roles` (`id`, `role_name`, `role_code`, `description`) VALUES
(1, '管理员', 'admin', '系统全局管理员'),
(2, '教练员', 'instructor', '驾校教练'),
(3, '学员', 'student', '报名学员');

-- 3. 初始化用户账号 (MD5 加密 Aq123456)
INSERT INTO `sys_users` (`id`, `username`, `password`, `phone`, `status`, `created_at`) VALUES
(1, 'admin', MD5('Aq123456'), '13800000001', 1, NOW()),
(2, 'coach_zhang', MD5('Aq123456'), '13800000002', 1, NOW()),
(3, 'student_li', MD5('Aq123456'), '13800000003', 1, NOW()),
(4, 'student_wang', MD5('Aq123456'), '13800000004', 1, NOW());

-- 4. 绑定用户角色
INSERT INTO `sys_user_roles` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 3);

-- 5. 初始化教练档案
INSERT INTO `biz_instructors` (`id`, `user_id`, `real_name`, `phone`, `teach_type`, `experience_years`, `rating`, `current_load`) VALUES
(1, 2, '张教练', '13800000002', 'C1', 8, 4.9, 2);

-- 6. 初始化学员档案并关联教练
INSERT INTO `biz_students` (`id`, `user_id`, `real_name`, `id_card`, `phone`, `license_type`, `instructor_id`, `status`) VALUES
(1, 3, '李学员', '340123199501011234', '13800000003', 'C1', 1, 2),
(2, 4, '王学员', '340123199805055678', '13800000004', 'C1', 1, 2);

-- 7. 初始化学习进度
INSERT INTO `biz_learning_progress` (`student_id`, `subject`, `hours_done`, `status`) VALUES
(1, 1, 12, 2),
(1, 2, 8, 1),
(2, 1, 12, 2),
(2, 2, 0, 1);

-- 8. 初始化教练日程
INSERT INTO `biz_instructor_schedule` (`instructor_id`, `work_date`, `time_slot`, `is_busy`) VALUES
(1, CURDATE(), '08:00-10:00', 1),
(1, CURDATE(), '10:00-12:00', 0),
(1, CURDATE(), '14:00-16:00', 0),
(1, CURDATE(), '16:00-18:00', 0);

SET FOREIGN_KEY_CHECKS = 1;
