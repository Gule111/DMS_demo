create table dms_demo.biz_enrollments
(
    id            bigint auto_increment comment '主键'
        primary key,
    student_id    bigint            null comment '关联学员ID',
    id_card_front varchar(255)      not null comment '身份证正面URL',
    id_card_back  varchar(255)      not null comment '身份证反面URL',
    health_cert   varchar(255)      null comment '体检证明URL',
    audit_status  tinyint default 0 null comment '审核状态: 0-待审核, 1-通过, 2-驳回',
    audit_remark  text              null comment '审核意见',
    auditor_id    bigint            null comment '审核人ID'
)
    comment '报名材料及审核表' charset = utf8mb4;

create table dms_demo.biz_exams
(
    id         bigint auto_increment comment '主键'
        primary key,
    student_id bigint            null comment '关联学员ID',
    subject    tinyint           not null comment '考试科目',
    exam_type  tinyint default 1 null comment '考试类型: 1-正式考试, 2-模拟考试',
    exam_date  date              null comment '预约考试日期',
    exam_site  varchar(100)      null comment '考试地点',
    status     tinyint default 0 null comment '状态: 0-待审核, 1-预约成功, 2-考试完成',
    score      int               null comment '考试成绩'
)
    comment '考试报名及成绩表' charset = utf8mb4;

create table dms_demo.biz_generated_documents
(
    id         bigint auto_increment comment '主键'
        primary key,
    student_id bigint       null comment '关联学员ID',
    doc_type   varchar(50)  not null comment '文档类型',
    file_url   varchar(255) not null comment '文件存储路径'
)
    comment '系统生成文档表' charset = utf8mb4;

create table dms_demo.biz_instructors
(
    id           bigint auto_increment comment '主键'
        primary key,
    user_id      bigint        null comment '关联sys_users.id',
    real_name    varchar(50)   not null comment '教练姓名',
    phone        varchar(20)   not null comment '联系电话',
    teach_type   varchar(50)   null comment '准教车型',
    current_load int default 0 null comment '当前带教人数'
)
    comment '教练员信息表' charset = utf8mb4;

create table dms_demo.biz_learning_progress
(
    id         bigint auto_increment comment '主键'
        primary key,
    student_id bigint            null comment '关联学员ID',
    subject    tinyint           not null comment '科目: 1, 2, 3, 4',
    hours_done int     default 0 null comment '已完成学时',
    status     tinyint default 0 null comment '状态: 0-未开始, 1-进行中, 2-已完成'
)
    comment '学员学习进度表' charset = utf8mb4;

create table dms_demo.biz_students
(
    id             bigint auto_increment comment '主键'
        primary key,
    user_id        bigint            null comment '关联sys_users.id',
    real_name      varchar(50)       not null comment '真实姓名',
    id_card        varchar(18)       not null comment '身份证号',
    phone          varchar(20)       not null comment '联系电话',
    license_type   varchar(10)       null comment '报考类型',
    instructor_id  bigint            null comment '分配的教练ID',
    instructor_req varchar(255)      null comment '对教练的要求',
    status         tinyint default 0 null comment '状态: 0-未报名, 1-审核中, 2-学习中, 3-已拿证',
    constraint uk_id_card
        unique (id_card)
)
    comment '学员信息表' charset = utf8mb4;

create table dms_demo.sys_dict
(
    id         bigint auto_increment comment '主键'
        primary key,
    dict_type  varchar(50)  not null comment '字典类型',
    dict_code  varchar(50)  not null comment '字典编码',
    dict_value varchar(100) not null comment '字典展示值'
)
    comment '基础信息字典表' charset = utf8mb4;

create table dms_demo.sys_menus
(
    id         bigint auto_increment comment '主键'
        primary key,
    parent_id  bigint  default 0 null comment '父菜单ID',
    menu_name  varchar(50)       not null comment '菜单/路由名称',
    path       varchar(255)      null comment '前端路由地址',
    component  varchar(255)      null comment '前端组件路径',
    perms      varchar(100)      null comment '权限标识',
    menu_type  char              not null comment '类型: M-目录, C-菜单, F-按钮',
    icon       varchar(100)      null comment '菜单图标',
    sort_order int     default 0 null comment '排序号',
    status     tinyint default 1 null comment '状态: 1-正常, 0-停用'
)
    comment '菜单与路由权限表' charset = utf8mb4;

create table dms_demo.sys_role_menus
(
    role_id bigint not null comment '角色ID',
    menu_id bigint not null comment '菜单ID',
    primary key (role_id, menu_id)
)
    comment '角色-菜单关联表' charset = utf8mb4;

create table dms_demo.sys_roles
(
    id          bigint auto_increment comment '主键'
        primary key,
    role_name   varchar(50)  not null comment '角色名称',
    role_code   varchar(50)  not null comment '角色编码',
    description varchar(255) null comment '角色描述'
)
    comment '角色表' charset = utf8mb4;

create table dms_demo.sys_user_roles
(
    user_id bigint not null comment '用户ID',
    role_id bigint not null comment '角色ID',
    primary key (user_id, role_id)
)
    comment '用户-角色关联表' charset = utf8mb4;

create table dms_demo.sys_users
(
    id         bigint auto_increment comment '主键'
        primary key,
    username   varchar(50)                        not null comment '登录名',
    password   varchar(128)                       not null comment '密码(MD5)',
    phone      varchar(20)                        null comment '手机号',
    status     tinyint  default 1                 null comment '状态: 1-正常, 0-禁用',
    created_at datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_phone
        unique (phone),
    constraint uk_username
        unique (username)
)
    comment '用户基础表' charset = utf8mb4;

