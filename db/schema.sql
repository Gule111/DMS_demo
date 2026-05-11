CREATE DATABASE IF NOT EXISTS `dms` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `dms`;

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `role` TINYINT NOT NULL COMMENT '角色(1:学员 2:教练 3:管理员)',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除(0:未删除 1:已删除)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `registration` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL COMMENT '学员ID',
    `package_type` VARCHAR(50) NOT NULL COMMENT '报考套餐类型',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1:待审核 2:已审核/待缴费 3:已缴费/报名成功 4:已驳回)',
    `fee` DECIMAL(10,2) NOT NULL COMMENT '报名费用',
    `id_card_front` VARCHAR(255) DEFAULT NULL COMMENT '身份证正面图片URL',
    `id_card_back` VARCHAR(255) DEFAULT NULL COMMENT '身份证反面图片URL',
    `health_cert` VARCHAR(255) DEFAULT NULL COMMENT '体检证明图片URL',
    `audit_remark` VARCHAR(255) DEFAULT NULL COMMENT '审核备注/驳回原因',
    `generated_reg_form` VARCHAR(255) DEFAULT NULL COMMENT '系统生成的报名表文件URL',
    `generated_health_form` VARCHAR(255) DEFAULT NULL COMMENT '系统生成的体检表文件URL',
    `coach_preference` VARCHAR(100) DEFAULT NULL COMMENT '教练偏好要求',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除(0:未删除 1:已删除)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报名申请表';

CREATE TABLE IF NOT EXISTS `coach_assignment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL COMMENT '学员ID',
    `coach_id` BIGINT NOT NULL COMMENT '教练ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态(1:生效中 2:已更换)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除(0:未删除 1:已删除)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教练分配表';

CREATE TABLE IF NOT EXISTS `exam_score` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL COMMENT '学员ID',
    `subject` TINYINT NOT NULL COMMENT '科目(1:科目一 2:科目二 3:科目三 4:科目四)',
    `score` INT DEFAULT NULL COMMENT '考试分数',
    `is_passed` TINYINT DEFAULT 0 COMMENT '是否及格(0:否 1:是)',
    `exam_date` DATE DEFAULT NULL COMMENT '考试日期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除(0:未删除 1:已删除)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试成绩记录表';

CREATE TABLE IF NOT EXISTS `learning_progress` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL COMMENT '学员ID',
    `subject` TINYINT NOT NULL COMMENT '科目(1:科目一 2:科目二 3:科目三 4:科目四)',
    `total_hours` INT NOT NULL COMMENT '要求总学时',
    `completed_hours` INT DEFAULT 0 COMMENT '已完成学时',
    `status` TINYINT DEFAULT 0 COMMENT '状态(0:未开始 1:学习中 2:已完成)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除(0:未删除 1:已删除)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学员学习进度表';

CREATE TABLE IF NOT EXISTS `exam_appointment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL COMMENT '学员ID',
    `subject` TINYINT NOT NULL COMMENT '预约科目(1:科目一 2:科目二 3:科目三 4:科目四)',
    `site_id` BIGINT NOT NULL COMMENT '考场ID',
    `exam_date` DATETIME NOT NULL COMMENT '预约考试时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态(1:待审核 2:预约成功 3:已取消 4:已驳回)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除(0:未删除 1:已删除)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试预约表';

CREATE TABLE IF NOT EXISTS `training_site` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL COMMENT '场地名称',
    `address` VARCHAR(255) NOT NULL COMMENT '场地地址',
    `capacity` INT DEFAULT 0 COMMENT '容纳人数/车辆数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除(0:未删除 1:已删除)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练/考试场地信息表';

CREATE TABLE IF NOT EXISTS `vehicle` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `plate_number` VARCHAR(20) NOT NULL COMMENT '车牌号',
    `brand` VARCHAR(50) DEFAULT NULL COMMENT '车辆品牌型号',
    `status` TINYINT DEFAULT 1 COMMENT '状态(1:可用 2:维修中 3:已报废)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除(0:未删除 1:已删除)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆信息表';
