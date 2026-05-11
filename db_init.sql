-- --------------------------------------------------------
-- 驾校报名与管理系统 (DMS) 数据库初始化脚本
-- 包含：表结构定义与初始样例数据
-- 注意：所有样例用户的密码均为 'Aq123456'，采用 MD5 加密
-- --------------------------------------------------------

CREATE DATABASE IF NOT EXISTS `dms_demo` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `dms_demo`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 菜单与路由权限表
-- ----------------------------
DROP TABLE IF EXISTS `sys_menus`;
CREATE TABLE `sys_menus` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` bigint(20) DEFAULT '0' COMMENT '父菜单ID',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单/路由名称',
  `path` varchar(255) DEFAULT NULL COMMENT '前端路由地址',
  `component` varchar(255) DEFAULT NULL COMMENT '前端组件路径',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识',
  `menu_type` char(1) NOT NULL COMMENT '类型: M-目录, C-菜单, F-按钮',
  `icon` varchar(100) DEFAULT NULL COMMENT '菜单图标',
  `sort_order` int(11) DEFAULT '0' COMMENT '排序号',
  `status` tinyint(4) DEFAULT '1' COMMENT '状态: 1-正常, 0-停用',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单与路由权限表';

-- 插入菜单样例数据
INSERT INTO `sys_menus` VALUES
(1, 0, '管理控制台', '/admin/dashboard', 'views/admin/Dashboard', NULL, 'C', 'dashboard', 1, 1),
(2, 0, '报名资料审核', '/admin/audit/enrollment', 'views/admin/AuditEnrollment', 'admin:audit', 'C', 'audit', 2, 1),
(3, 0, '教练分配管理', '/admin/assign/instructor', 'views/admin/AssignInstructor', 'admin:assign', 'C', 'team', 3, 1),
(4, 0, '学员工作台', '/student/dashboard', 'views/student/Dashboard', NULL, 'C', 'user-dashboard', 1, 1),
(5, 0, '在线报名', '/student/enrollment', 'views/student/Enrollment', 'student:enroll', 'C', 'form', 2, 1),
(6, 0, '教练工作台', '/instructor/dashboard', 'views/instructor/Dashboard', NULL, 'C', 'coach-dashboard', 1, 1),
(7, 0, '我的学员', '/instructor/students', 'views/instructor/Students', 'instructor:students', 'C', 'users', 2, 1),
(8, 0, '用户管理', '/admin/users', 'views/UserManage', 'admin:user:manage', 'C', 'team', 4, 1);

-- ----------------------------
-- 2. 角色表
-- ----------------------------
DROP TABLE IF EXISTS `sys_roles`;
CREATE TABLE `sys_roles` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `description` varchar(255) DEFAULT NULL COMMENT '角色描述',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 插入角色样例数据
INSERT INTO `sys_roles` VALUES
(1, '管理员', 'admin', '系统全局管理员，负责审核与统筹'),
(2, '教练员', 'instructor', '驾校教练，负责日常教学与进度录入'),
(3, '学员', 'student', '报名学员');

-- ----------------------------
-- 3. 角色-菜单关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menus`;
CREATE TABLE `sys_role_menus` (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单关联表';

-- 插入关联数据
INSERT INTO `sys_role_menus` VALUES
(1, 1), (1, 2), (1, 3), (1, 8), -- 管理员权限 (增加了8：用户管理)
(3, 4), (3, 5),         -- 学员权限
(2, 6), (2, 7);         -- 教练员权限

-- ----------------------------
-- 4. 用户基础表
-- ----------------------------
DROP TABLE IF EXISTS `sys_users`;
CREATE TABLE `sys_users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) NOT NULL COMMENT '登录名',
  `password` varchar(128) NOT NULL COMMENT '密码(MD5)',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `status` tinyint(4) DEFAULT '1' COMMENT '状态: 1-正常, 0-禁用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础表';

-- 插入用户样例数据，使用 MD5 函数直接加密 'Aq123456'
INSERT INTO `sys_users` VALUES
(1, 'admin', MD5('Aq123456'), NULL, 1, NOW()),
(2, 'coach_zhang', MD5('Aq123456'), '13800000002', 1, NOW()),
(3, 'student_li', MD5('Aq123456'), '13900000003', 1, NOW());

-- ----------------------------
-- 5. 用户-角色关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_roles`;
CREATE TABLE `sys_user_roles` (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

-- 插入关联数据
INSERT INTO `sys_user_roles` VALUES
(1, 1), -- admin账号 -> 管理员
(2, 2), -- coach_zhang -> 教练
(3, 3); -- student_li -> 学员

-- ----------------------------
-- 6. 教练员信息表
-- ----------------------------
DROP TABLE IF EXISTS `biz_instructors`;
CREATE TABLE `biz_instructors` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) DEFAULT NULL COMMENT '关联sys_users.id',
  `real_name` varchar(50) NOT NULL COMMENT '教练姓名',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `teach_type` varchar(50) DEFAULT NULL COMMENT '准教车型',
  `current_load` int(11) DEFAULT '0' COMMENT '当前带教人数',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教练员信息表';

INSERT INTO `biz_instructors` VALUES
(1, 2, '张教练', '13800000002', 'C1', 1);

-- ----------------------------
-- 7. 学员信息表
-- ----------------------------
DROP TABLE IF EXISTS `biz_students`;
CREATE TABLE `biz_students` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) DEFAULT NULL COMMENT '关联sys_users.id',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `id_card` varchar(18) NOT NULL COMMENT '身份证号',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `license_type` varchar(10) DEFAULT NULL COMMENT '报考类型',
  `instructor_id` bigint(20) DEFAULT NULL COMMENT '分配的教练ID',
  `instructor_req` varchar(255) DEFAULT NULL COMMENT '对教练的要求',
  `status` tinyint(4) DEFAULT '0' COMMENT '状态: 0-未报名, 1-审核中, 2-学习中, 3-已拿证',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_id_card` (`id_card`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学员信息表';

INSERT INTO `biz_students` VALUES
(1, 3, '李学员', '110105199001011234', '13900000003', 'C1', 1, '希望教练脾气好一点，周末练车', 2);

-- ----------------------------
-- 8. 报名材料及审核表
-- ----------------------------
DROP TABLE IF EXISTS `biz_enrollments`;
CREATE TABLE `biz_enrollments` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint(20) DEFAULT NULL COMMENT '关联学员ID',
  `id_card_front` varchar(255) NOT NULL COMMENT '身份证正面URL',
  `id_card_back` varchar(255) NOT NULL COMMENT '身份证反面URL',
  `health_cert` varchar(255) DEFAULT NULL COMMENT '体检证明URL',
  `audit_status` tinyint(4) DEFAULT '0' COMMENT '审核状态: 0-待审核, 1-通过, 2-驳回',
  `audit_remark` varchar(255) DEFAULT NULL COMMENT '审核意见',
  `auditor_id` bigint(20) DEFAULT NULL COMMENT '审核人ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报名材料及审核表';

INSERT INTO `biz_enrollments` VALUES
(1, 1, '/uploads/id_front.jpg', '/uploads/id_back.jpg', '/uploads/health.jpg', 1, '材料齐全，审核通过', 1);

-- ----------------------------
-- 9. 系统生成文档表
-- ----------------------------
DROP TABLE IF EXISTS `biz_generated_documents`;
CREATE TABLE `biz_generated_documents` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint(20) DEFAULT NULL COMMENT '关联学员ID',
  `doc_type` varchar(50) NOT NULL COMMENT '文档类型',
  `file_url` varchar(255) NOT NULL COMMENT '文件存储路径',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统生成文档表';

INSERT INTO `biz_generated_documents` VALUES
(1, 1, 'EnrollmentForm', '/docs/enrollment_form_stu1.pdf');

-- ----------------------------
-- 10. 学员学习进度表
-- ----------------------------
DROP TABLE IF EXISTS `biz_learning_progress`;
CREATE TABLE `biz_learning_progress` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint(20) DEFAULT NULL COMMENT '关联学员ID',
  `subject` tinyint(4) NOT NULL COMMENT '科目: 1, 2, 3, 4',
  `hours_done` int(11) DEFAULT '0' COMMENT '已完成学时',
  `status` tinyint(4) DEFAULT '0' COMMENT '状态: 0-未开始, 1-进行中, 2-已完成',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学员学习进度表';

INSERT INTO `biz_learning_progress` VALUES
(1, 1, 1, 12, 2), -- 李学员科一已完成
(2, 1, 2, 8, 1);  -- 李学员科二进行中 (8学时)

-- ----------------------------
-- 11. 考试报名及成绩表
-- ----------------------------
DROP TABLE IF EXISTS `biz_exams`;
CREATE TABLE `biz_exams` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint(20) DEFAULT NULL COMMENT '关联学员ID',
  `subject` tinyint(4) NOT NULL COMMENT '考试科目',
  `exam_date` date DEFAULT NULL COMMENT '预约考试日期',
  `exam_site` varchar(100) DEFAULT NULL COMMENT '考试地点',
  `status` tinyint(4) DEFAULT '0' COMMENT '状态: 0-待审核, 1-预约成功, 2-考试完成',
  `score` int(11) DEFAULT NULL COMMENT '考试成绩',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试报名及成绩表';

INSERT INTO `biz_exams` VALUES
(1, 1, 1, '2026-05-10', '市第一车辆管理所考场', 2, 98); -- 科目一考了98分

-- ----------------------------
-- 12. 基础信息字典表
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dict_type` varchar(50) NOT NULL COMMENT '字典类型',
  `dict_code` varchar(50) NOT NULL COMMENT '字典编码',
  `dict_value` varchar(100) NOT NULL COMMENT '字典展示值',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基础信息字典表';

INSERT INTO `sys_dict` VALUES
(1, 'LICENSE_TYPE', 'C1', '小型汽车 C1'),
(2, 'LICENSE_TYPE', 'C2', '小型自动挡汽车 C2'),
(3, 'EXAM_SITE', 'SITE1', '市第一车辆管理所考场'),
(4, 'EXAM_SITE', 'SITE2', '城南驾考中心');

SET FOREIGN_KEY_CHECKS = 1;
