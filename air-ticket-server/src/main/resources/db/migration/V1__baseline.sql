-- Canonical V1. Flyway runs this only in an empty database.
-- No CREATE DATABASE / USE or data reset; no historical passenger records.
-- Default teaching administrator: admin / admin123; change the password after deployment.
CREATE TABLE IF NOT EXISTS `admin` (
  `id`       INT           NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username` VARCHAR(50)   NOT NULL COMMENT '管理员用户名',
  `password` VARCHAR(255)  NOT NULL COMMENT '密码（BCrypt 加密）',
  `role`     TINYINT       DEFAULT 1 COMMENT '角色：0-超级管理员，1-普通管理员',
  `menu_id`  VARCHAR(500)  DEFAULT NULL COMMENT '分配的菜单ID列表，逗号分隔',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- ----------------------------
-- 2. 菜单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `menu` (
  `id`    INT          NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `path`  VARCHAR(100) NOT NULL COMMENT '前端路由路径',
  `title` VARCHAR(50)  NOT NULL COMMENT '菜单显示名称',
  `icon`  VARCHAR(50)  DEFAULT NULL COMMENT '图标',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- ----------------------------
-- 3. 注册用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id`       INT          NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50)  NOT NULL COMMENT '用户名（登录用）',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt 加密）',
  `name`     VARCHAR(50)  NOT NULL COMMENT '真实姓名',
  `gender`   TINYINT      DEFAULT 0 COMMENT '性别：0-男，1-女',
  `phone`    VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `email`    VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `identity` VARCHAR(18)  DEFAULT NULL COMMENT '身份证号码',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='注册用户表';

-- ----------------------------
-- 4. 航空公司表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `company` (
  `company_id`      INT          NOT NULL AUTO_INCREMENT COMMENT '公司ID',
  `company_name`    VARCHAR(100) NOT NULL COMMENT '公司名称',
  `company_profile` TEXT         DEFAULT NULL COMMENT '公司简介',
  `company_picture` VARCHAR(255) DEFAULT NULL COMMENT '公司图片路径',
  `company_culture` TEXT         DEFAULT NULL COMMENT '公司文化',
  `company_phone`   VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  `company_purport` TEXT         DEFAULT NULL COMMENT '公司宗旨',
  PRIMARY KEY (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='航空公司表';

-- ----------------------------
-- 5. 航班信息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hbxx` (
  `hbid`       INT            NOT NULL AUTO_INCREMENT COMMENT '航班ID',
  `company_id` INT            DEFAULT NULL COMMENT '所属航空公司ID',
  `xclb`       VARCHAR(50)    DEFAULT NULL COMMENT '行程类别（国内/国际）',
  `start`      VARCHAR(100)   NOT NULL COMMENT '出发地',
  `end`        VARCHAR(100)   NOT NULL COMMENT '目的地',
  `starttime`  DATETIME       NOT NULL COMMENT '出发时间',
  `number`     INT            DEFAULT 0 COMMENT '余票数量',
  `price`      DECIMAL(10,2)  NOT NULL COMMENT '票价（元）',
  `duration`   VARCHAR(50)    DEFAULT NULL COMMENT '飞行时长',
  `place`      VARCHAR(200)   DEFAULT NULL COMMENT '乘机地点',
  PRIMARY KEY (`hbid`),
  KEY `idx_hbxx_start_end_time` (`start`, `end`, `starttime`),
  KEY `idx_hbxx_company_id` (`company_id`),
  CONSTRAINT `fk_hbxx_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='航班信息表';

-- ----------------------------
-- 6. 机票订单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `jpdd` (
  `ddid`       INT            NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `hbid`       INT            NOT NULL COMMENT '航班ID',
  `userid`     INT            NOT NULL COMMENT '用户ID',
  `cabin_id`   INT            DEFAULT NULL COMMENT '舱位ID',
  `quantity`   INT            NOT NULL DEFAULT 1 COMMENT '购买数量',
  `cost`       DECIMAL(10,2)  NOT NULL COMMENT '总费用',
  `unit_price` DECIMAL(10,2)  DEFAULT NULL COMMENT '下单时单价快照',
  `ordertime`  DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `status`     TINYINT        DEFAULT 1 COMMENT '订单状态：1-已购票，2-退订中，3-已退订',
  PRIMARY KEY (`ddid`),
  KEY `idx_jpdd_userid` (`userid`),
  KEY `idx_jpdd_hbid` (`hbid`),
  KEY `idx_jpdd_status` (`status`),
  KEY `idx_jpdd_cabin_id` (`cabin_id`),
  CONSTRAINT `fk_jpdd_hbxx` FOREIGN KEY (`hbid`) REFERENCES `hbxx` (`hbid`),
  CONSTRAINT `fk_jpdd_user` FOREIGN KEY (`userid`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机票订单表';

-- ----------------------------
-- 7. 航班舱位表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hbxx_cabin` (
  `cabin_id`          INT            NOT NULL AUTO_INCREMENT COMMENT '舱位ID',
  `hbid`              INT            NOT NULL COMMENT '航班ID',
  `cabin_class`       TINYINT        NOT NULL COMMENT '舱位类型：1-经济舱 2-商务舱 3-头等舱',
  `cabin_name`        VARCHAR(20)    NOT NULL COMMENT '舱位显示名',
  `price`             DECIMAL(10,2)  NOT NULL COMMENT '该舱位票价',
  `total_number`      INT            NOT NULL DEFAULT 0 COMMENT '该舱位总座位数',
  `available_number`  INT            NOT NULL DEFAULT 0 COMMENT '该舱位可售余票',
  PRIMARY KEY (`cabin_id`),
  UNIQUE KEY `uk_hbxx_cabin` (`hbid`, `cabin_class`),
  CONSTRAINT `fk_cabin_hbxx` FOREIGN KEY (`hbid`) REFERENCES `hbxx` (`hbid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='航班舱位表';

-- ----------------------------
-- 8. 座位占位表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `seat_hold` (
  `hold_id`     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '占位ID',
  `hbid`        INT           NOT NULL COMMENT '航班ID',
  `cabin_id`    INT           NOT NULL COMMENT '舱位ID',
  `userid`      INT           NOT NULL COMMENT '占位用户',
  `quantity`    INT           NOT NULL COMMENT '占位数量',
  `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '占位时间',
  `expire_time` DATETIME      NOT NULL COMMENT '过期时间',
  `status`      TINYINT       DEFAULT 0 COMMENT '0-占位中 1-已转订单 2-已过期释放 3-用户取消',
  PRIMARY KEY (`hold_id`),
  KEY `idx_seat_hold_cabin_status` (`cabin_id`, `status`),
  KEY `idx_seat_hold_expire` (`expire_time`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位占位表';

-- ----------------------------
-- 9. 退订机票表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tdjp` (
  `id`         INT      NOT NULL AUTO_INCREMENT COMMENT '退订记录ID',
  `ddid`       INT      DEFAULT NULL COMMENT '关联的订单ID',
  `canceltime` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '退订申请时间',
  `process`    TINYINT  DEFAULT 0 COMMENT '审核状态：0-待审核，1-审核通过',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tdjp_ddid` (`ddid`),
  KEY `idx_tdjp_process` (`process`),
  CONSTRAINT `fk_tdjp_jpdd` FOREIGN KEY (`ddid`) REFERENCES `jpdd` (`ddid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退订机票表';

-- ----------------------------
-- 10. 航空资讯表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `system` (
  `id`      INT          NOT NULL AUTO_INCREMENT COMMENT '资讯ID',
  `title`   VARCHAR(200) NOT NULL COMMENT '资讯标题',
  `profile` TEXT         DEFAULT NULL COMMENT '资讯内容',
  `picture` VARCHAR(255) DEFAULT NULL COMMENT '配图路径',
  `time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='航空资讯表';

-- ----------------------------
-- 11. 订单操作日志表（审计表）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `order_log` (
    `log_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID（自增主键）',
    `ddid`       INT          DEFAULT NULL COMMENT '关联的订单ID（关联 jpdd.ddid）',
    `action`     VARCHAR(20)  NOT NULL COMMENT '操作类型：CREATE-创建订单，UPDATE-更新订单状态，DELETE-删除订单',
    `old_status` TINYINT      DEFAULT NULL COMMENT '变更前订单状态：1-已购票，2-退订中，3-已退订（CREATE 时为 NULL）',
    `new_status` TINYINT      DEFAULT NULL COMMENT '变更后订单状态：1-已购票，2-退订中，3-已退订（DELETE 时为 NULL）',
    `log_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '日志记录时间（默认当前时间）',
    `operator`   VARCHAR(50)  DEFAULT NULL COMMENT '操作人（系统触发时为 NULL，管理员操作时记录管理员用户名）',
    `remark`     VARCHAR(255) DEFAULT NULL COMMENT '备注信息（记录操作详情或异常说明）',
    PRIMARY KEY (`log_id`),
    KEY `idx_order_log_ddid` (`ddid`),
    KEY `idx_order_log_action` (`action`),
    KEY `idx_order_log_time` (`log_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单操作日志表（审计表，由触发器自动写入，记录订单生命周期内所有关键操作）';


-- ============================

INSERT INTO `admin` (`username`, `password`, `role`, `menu_id`) VALUES
('admin', '$2a$10$C3R9jauegHQZlgiNrw75KeFxehXBxUPcFG4zOf93GgmTxd6uP77j6', 0, NULL);
INSERT INTO `menu` (`id`, `path`, `title`, `icon`) VALUES
(1, '/admin/dashboard', '首页', 'el-icon-s-home'),
(2, '/admin/user', '用户管理', 'el-icon-user'),
(3, '/admin/company', '航空公司管理', 'el-icon-office-building'),
(4, '/admin/flight', '航班管理', 'el-icon-s-promotion'),
(5, '/admin/order', '订单管理', 'el-icon-s-order'),
(6, '/admin/refund', '退订管理', 'el-icon-s-claim'),
(7, '/admin/news', '资讯管理', 'el-icon-document'),
(8, '/admin/menu', '菜单管理', 'el-icon-menu'),
(9, '/admin/role', '角色管理', 'el-icon-s-custom');
DROP PROCEDURE IF EXISTS `sp_add_index_if_not_exists`;
DELIMITER $$
CREATE PROCEDURE `sp_add_index_if_not_exists`(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_cols VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND INDEX_NAME = p_index
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_cols, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 订单按下单时间范围查询（用户/管理端按时间筛选订单）
CALL sp_add_index_if_not_exists('jpdd', 'idx_jpdd_ordertime', '`ordertime`');

-- 占位记录按用户查询（用户查看自己的抢票占位）
CALL sp_add_index_if_not_exists('seat_hold', 'idx_seat_hold_userid', '`userid`, `status`');

-- 舱位按航班查询（航班详情展示舱位列表）
CALL sp_add_index_if_not_exists('hbxx_cabin', 'idx_hbxx_cabin_hbid', '`hbid`');

DROP PROCEDURE IF EXISTS `sp_add_index_if_not_exists`;


-- ============================
-- 四、检查约束（数据完整性，MySQL 8.0.16+ 支持）
-- 使用存储过程实现幂等添加，可重复执行
-- ============================

DROP PROCEDURE IF EXISTS `sp_add_check_if_not_exists`;
DELIMITER $$
CREATE PROCEDURE `sp_add_check_if_not_exists`(
    IN p_table VARCHAR(64),
    IN p_constraint VARCHAR(64),
    IN p_check_expr VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND CONSTRAINT_NAME = p_constraint
          AND CONSTRAINT_TYPE = 'CHECK'
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD CONSTRAINT `', p_constraint, '` CHECK (', p_check_expr, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 订单数量必须大于 0
CALL sp_add_check_if_not_exists('jpdd', 'chk_jpdd_quantity', '`quantity` > 0');

-- 订单总费用必须大于等于 0
CALL sp_add_check_if_not_exists('jpdd', 'chk_jpdd_cost', '`cost` >= 0');

-- 舱位类型只能为 1/2/3
CALL sp_add_check_if_not_exists('hbxx_cabin', 'chk_hbxx_cabin_class', '`cabin_class` IN (1, 2, 3)');

-- 舱位可售数不能超过总数
CALL sp_add_check_if_not_exists('hbxx_cabin', 'chk_hbxx_cabin_available', '`available_number` <= `total_number` AND `available_number` >= 0');

-- 占位状态只能为 0/1/2/3
CALL sp_add_check_if_not_exists('seat_hold', 'chk_seat_hold_status', '`status` IN (0, 1, 2, 3)');

-- 订单状态只能为 1/2/3
CALL sp_add_check_if_not_exists('jpdd', 'chk_jpdd_status', '`status` IN (1, 2, 3)');

-- 航班票价必须大于 0
CALL sp_add_check_if_not_exists('hbxx', 'chk_hbxx_price', '`price` > 0');

-- 航班余票不能为负
CALL sp_add_check_if_not_exists('hbxx', 'chk_hbxx_number', '`number` >= 0');

DROP PROCEDURE IF EXISTS `sp_add_check_if_not_exists`;


-- ============================
-- 五、视图（简化多表关联查询）
-- ============================

-- 1. 订单详情视图：订单 + 航班 + 航空公司 + 用户 + 舱位
DROP VIEW IF EXISTS `v_jpdd_detail`;
CREATE VIEW `v_jpdd_detail` AS
SELECT
    j.ddid,
    j.hbid,
    j.userid,
    j.cabin_id,
    j.quantity,
    j.cost,
    j.unit_price,
    j.ordertime,
    j.status,
    h.company_id,
    c.company_name,
    h.xclb,
    h.start,
    h.`end`,
    h.starttime,
    h.duration,
    h.place,
    h.price AS flight_price,
    u.username,
    u.name AS user_name,
    u.phone AS user_phone,
    cab.cabin_class,
    cab.cabin_name,
    cab.price AS cabin_price
FROM `jpdd` j
LEFT JOIN `hbxx` h ON j.hbid = h.hbid
LEFT JOIN `company` c ON h.company_id = c.company_id
LEFT JOIN `user` u ON j.userid = u.id
LEFT JOIN `hbxx_cabin` cab ON j.cabin_id = cab.cabin_id;


-- 2. 退订详情视图：退订 + 订单 + 航班 + 用户
DROP VIEW IF EXISTS `v_tdjp_detail`;
CREATE VIEW `v_tdjp_detail` AS
SELECT
    t.id,
    t.ddid,
    t.canceltime,
    t.process,
    j.hbid,
    j.userid,
    j.quantity,
    j.cost,
    j.unit_price,
    j.ordertime,
    h.start,
    h.`end`,
    h.starttime,
    h.company_id,
    c.company_name,
    u.username,
    u.name AS user_name,
    u.phone AS user_phone,
    cab.cabin_name
FROM `tdjp` t
LEFT JOIN `jpdd` j ON t.ddid = j.ddid
LEFT JOIN `hbxx` h ON j.hbid = h.hbid
LEFT JOIN `company` c ON h.company_id = c.company_id
LEFT JOIN `user` u ON j.userid = u.id
LEFT JOIN `hbxx_cabin` cab ON j.cabin_id = cab.cabin_id;


-- 3. 航班余票汇总视图：航班 + 航空公司 + 各舱位余票汇总
DROP VIEW IF EXISTS `v_hbxx_cabin_summary`;
CREATE VIEW `v_hbxx_cabin_summary` AS
SELECT
    h.hbid,
    h.company_id,
    c.company_name,
    h.xclb,
    h.start,
    h.`end`,
    h.starttime,
    h.duration,
    h.place,
    h.price,
    h.number AS total_remaining,
    SUM(CASE WHEN cab.cabin_class = 1 THEN cab.available_number ELSE 0 END) AS economy_remaining,
    SUM(CASE WHEN cab.cabin_class = 2 THEN cab.available_number ELSE 0 END) AS business_remaining,
    SUM(CASE WHEN cab.cabin_class = 3 THEN cab.available_number ELSE 0 END) AS first_remaining,
    SUM(cab.available_number) AS cabin_total_remaining
FROM `hbxx` h
LEFT JOIN `company` c ON h.company_id = c.company_id
LEFT JOIN `hbxx_cabin` cab ON h.hbid = cab.hbid
GROUP BY h.hbid, h.company_id, c.company_name, h.xclb, h.start, h.`end`, h.starttime, h.duration, h.place, h.price, h.number;


-- 4. 用户订单统计视图：用户 + 订单数量 + 总消费
DROP VIEW IF EXISTS `v_user_order_stats`;
CREATE VIEW `v_user_order_stats` AS
SELECT
    u.id AS userid,
    u.username,
    u.name,
    u.phone,
    COUNT(j.ddid) AS total_orders,
    SUM(CASE WHEN j.status = 1 THEN 1 ELSE 0 END) AS active_orders,
    SUM(CASE WHEN j.status = 2 THEN 1 ELSE 0 END) AS refunding_orders,
    SUM(CASE WHEN j.status = 3 THEN 1 ELSE 0 END) AS refunded_orders,
    SUM(CASE WHEN j.status = 1 THEN j.cost ELSE 0 END) AS total_spent
FROM `user` u
LEFT JOIN `jpdd` j ON u.id = j.userid
GROUP BY u.id, u.username, u.name, u.phone;


-- ============================
-- 六、存储过程
-- ============================

-- 1. 航班销售统计存储过程：按日期范围统计各航班销售情况
DROP PROCEDURE IF EXISTS `sp_flight_sales_stats`;
DELIMITER $$
CREATE PROCEDURE `sp_flight_sales_stats`(
    IN p_start_date DATETIME,
    IN p_end_date DATETIME
)
BEGIN
    SELECT
        h.hbid,
        c.company_name,
        h.start,
        h.`end`,
        h.starttime,
        COUNT(j.ddid) AS order_count,
        COALESCE(SUM(j.quantity), 0) AS total_tickets_sold,
        COALESCE(SUM(j.cost), 0) AS total_revenue,
        h.number AS current_remaining
    FROM `hbxx` h
    LEFT JOIN `company` c ON h.company_id = c.company_id
    LEFT JOIN `jpdd` j ON h.hbid = j.hbid
        AND j.ordertime BETWEEN p_start_date AND p_end_date
        AND j.status IN (1, 2)
    GROUP BY h.hbid, c.company_name, h.start, h.`end`, h.starttime, h.number
    ORDER BY total_revenue DESC;
END$$
DELIMITER ;


-- 2. 航空公司业绩排名存储过程
DROP PROCEDURE IF EXISTS `sp_company_ranking`;
DELIMITER $$
CREATE PROCEDURE `sp_company_ranking`()
BEGIN
    SELECT
        c.company_id,
        c.company_name,
        COUNT(DISTINCT h.hbid) AS flight_count,
        COUNT(j.ddid) AS total_orders,
        COALESCE(SUM(j.quantity), 0) AS total_tickets_sold,
        COALESCE(SUM(j.cost), 0) AS total_revenue
    FROM `company` c
    LEFT JOIN `hbxx` h ON c.company_id = h.company_id
    LEFT JOIN `jpdd` j ON h.hbid = j.hbid AND j.status IN (1, 2)
    GROUP BY c.company_id, c.company_name
    ORDER BY total_revenue DESC;
END$$
DELIMITER ;


-- 3. 退订审核处理存储过程：审核通过时自动恢复余票（事务安全）
DROP PROCEDURE IF EXISTS `sp_approve_refund`;
DELIMITER $$
CREATE PROCEDURE `sp_approve_refund`(
    IN p_tdjp_id INT,
    OUT p_result INT
)
BEGIN
    DECLARE v_ddid INT;
    DECLARE v_hbid INT;
    DECLARE v_cabin_id INT;
    DECLARE v_quantity INT;
    DECLARE v_process TINYINT;

    -- 初始化输出参数：0-失败 1-成功
    SET p_result = 0;

    -- 查询退订记录
    SELECT t.ddid, t.process, j.hbid, j.cabin_id, j.quantity
    INTO v_ddid, v_process, v_hbid, v_cabin_id, v_quantity
    FROM `tdjp` t
    LEFT JOIN `jpdd` j ON t.ddid = j.ddid
    WHERE t.id = p_tdjp_id;

    -- 校验：记录存在且未审核
    IF v_ddid IS NULL THEN
        SET p_result = -1;  -- 退订记录不存在
    ELSEIF v_process = 1 THEN
        SET p_result = -2;  -- 已审核，不能重复操作
    ELSE
        -- 开启事务
        START TRANSACTION;

        -- 更新退订审核状态
        UPDATE `tdjp` SET process = 1 WHERE id = p_tdjp_id;

        -- 更新订单状态为已退订
        UPDATE `jpdd` SET status = 3 WHERE ddid = v_ddid;

        -- 恢复舱位余票
        UPDATE `hbxx_cabin`
        SET available_number = available_number + v_quantity
        WHERE cabin_id = v_cabin_id;

        -- 恢复航班总余票
        UPDATE `hbxx`
        SET number = number + v_quantity
        WHERE hbid = v_hbid;

        COMMIT;
        SET p_result = 1;  -- 成功
    END IF;
END$$
DELIMITER ;


-- 4. 用户消费统计存储过程
DROP PROCEDURE IF EXISTS `sp_user_consumption_stats`;
DELIMITER $$
CREATE PROCEDURE `sp_user_consumption_stats`(
    IN p_userid INT
)
BEGIN
    SELECT
        u.username,
        u.name,
        COUNT(j.ddid) AS total_orders,
        SUM(CASE WHEN j.status = 1 THEN 1 ELSE 0 END) AS active_orders,
        SUM(CASE WHEN j.status = 2 THEN 1 ELSE 0 END) AS refunding_orders,
        SUM(CASE WHEN j.status = 3 THEN 1 ELSE 0 END) AS refunded_orders,
        COALESCE(SUM(CASE WHEN j.status = 1 THEN j.cost ELSE 0 END), 0) AS total_spent,
        COALESCE(SUM(CASE WHEN j.status = 1 THEN j.quantity ELSE 0 END), 0) AS total_tickets
    FROM `user` u
    LEFT JOIN `jpdd` j ON u.id = j.userid
    WHERE u.id = p_userid
    GROUP BY u.id, u.username, u.name;
END$$
DELIMITER ;


-- ============================
-- 七、触发器（自动维护数据一致性与审计日志）
-- ============================

-- 注意：由于本项目使用 Spring Boot + MyBatis 在 Java 应用层管理余票扣减/恢复逻辑
-- （见 JpddServiceImpl.holdSeat / TdjpServiceImpl.approve），
-- 因此不创建订单插入/更新的余票触发器，避免与 Java 代码重复执行导致余票计算错误。
-- 以下触发器仅处理应用层未覆盖的数据维护场景和审计日志。

-- 1. 航班删除前触发器：级联删除相关舱位和占位记录（seat_hold 表无外键约束，需手动清理）
DROP TRIGGER IF EXISTS `tr_hbxx_before_delete`;
DELIMITER $$
CREATE TRIGGER `tr_hbxx_before_delete`
BEFORE DELETE ON `hbxx`
FOR EACH ROW
BEGIN
    -- 删除关联的舱位记录
    DELETE FROM `hbxx_cabin` WHERE hbid = OLD.hbid;
    -- 删除关联的占位记录（seat_hold 表未建外键，需手动清理）
    DELETE FROM `seat_hold` WHERE hbid = OLD.hbid;
END$$
DELIMITER ;


-- 2. 舱位删除前触发器：清理关联的占位记录
DROP TRIGGER IF EXISTS `tr_hbxx_cabin_before_delete`;
DELIMITER $$
CREATE TRIGGER `tr_hbxx_cabin_before_delete`
BEFORE DELETE ON `hbxx_cabin`
FOR EACH ROW
BEGIN
    -- 清理该舱位的占位记录
    DELETE FROM `seat_hold` WHERE cabin_id = OLD.cabin_id;
END$$
DELIMITER ;


-- 3. 订单插入后触发器：记录订单创建日志（仅审计，不影响业务数据）
DROP TRIGGER IF EXISTS `tr_jpdd_after_insert`;
DELIMITER $$
CREATE TRIGGER `tr_jpdd_after_insert`
AFTER INSERT ON `jpdd`
FOR EACH ROW
BEGIN
    INSERT INTO `order_log` (ddid, action, new_status, remark)
    VALUES (NEW.ddid, 'CREATE', NEW.status, CONCAT('订单创建，购买数量：', NEW.quantity, '，总费用：', NEW.cost));
END$$
DELIMITER ;


-- 4. 订单状态变更后触发器：记录状态变更日志（仅审计，不影响业务数据）
DROP TRIGGER IF EXISTS `tr_jpdd_after_update`;
DELIMITER $$
CREATE TRIGGER `tr_jpdd_after_update`
AFTER UPDATE ON `jpdd`
FOR EACH ROW
BEGIN
    -- 仅在状态变更时记录日志
    IF OLD.status != NEW.status THEN
        INSERT INTO `order_log` (ddid, action, old_status, new_status, remark)
        VALUES (
            NEW.ddid,
            'UPDATE',
            OLD.status,
            NEW.status,
            CASE
                WHEN NEW.status = 1 THEN '订单状态变更为已购票'
                WHEN NEW.status = 2 THEN '订单状态变更为退订中'
                WHEN NEW.status = 3 THEN '订单状态变更为已退订'
                ELSE CONCAT('订单状态从 ', OLD.status, ' 变更为 ', NEW.status)
            END
        );
    END IF;
END$$
DELIMITER ;


-- 5. 订单删除后触发器：记录订单删除日志（仅审计，不影响业务数据）
DROP TRIGGER IF EXISTS `tr_jpdd_after_delete`;
DELIMITER $$
CREATE TRIGGER `tr_jpdd_after_delete`
AFTER DELETE ON `jpdd`
FOR EACH ROW
BEGIN
    INSERT INTO `order_log` (ddid, action, old_status, remark)
    VALUES (OLD.ddid, 'DELETE', OLD.status, CONCAT('订单删除，原订单数量：', OLD.quantity, '，原总费用：', OLD.cost));
END$$
DELIMITER ;


-- ============================

CREATE TABLE IF NOT EXISTS schema_version(version INT PRIMARY KEY, applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
INSERT IGNORE INTO schema_version(version)VALUES(1);
