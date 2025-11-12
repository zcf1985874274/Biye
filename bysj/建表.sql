
-- 棋牌房间信息表 (rooms)
CREATE TABLE rooms (
    room_id INT AUTO_INCREMENT PRIMARY KEY,           -- 房间ID
    room_name VARCHAR(100) NOT NULL,                   -- 房间名称
    room_type VARCHAR(50),                             -- 房间类型（如麻将、扑克等）
    max_players INT,                                   -- 房间最大玩家数
    min_players INT,                                   -- 房间最小玩家数
    price_per_hour DECIMAL(10, 2) NOT NULL,            -- 每小时单价
    description TEXT,                                  -- 房间描述
    image_path VARCHAR(255),                           -- 图片路径或连接
    status ENUM('空闲', '使用中') DEFAULT '空闲'       -- 房间状态
);

-- 记录表 (usage_records)
CREATE TABLE usage_records (
    record_id INT AUTO_INCREMENT PRIMARY KEY,    -- 记录ID
    room_id INT,                                 -- 房间ID，关联到rooms表
    user_id INT,                                 -- 用户ID，关联到用户表
    start_time DATETIME NOT NULL,                -- 开始时间
    end_time DATETIME NOT NULL,                  -- 结束时间
    total_price DECIMAL(10, 2) NOT NULL,         -- 总费用
    FOREIGN KEY (room_id) REFERENCES rooms(room_id), -- 外键关联房间ID
    FOREIGN KEY (user_id) REFERENCES users(user_id)  -- 外键关联用户ID
);

-- 用户信息表
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,         -- 用户ID
    username VARCHAR(50) NOT NULL UNIQUE,           -- 用户名
    password VARCHAR(255) NOT NULL,                 -- 密码
    email VARCHAR(100),                             -- 邮箱
    phone VARCHAR(20),                              -- 电话
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 注册时间
    last_login TIMESTAMP,                           -- 上次登录时间
    status ENUM('登录', '未登录') DEFAULT '未登录'  -- 账户状态
);

-- 管理员信息表
CREATE TABLE admins (
    admin_id INT AUTO_INCREMENT PRIMARY KEY,   -- 管理员ID
    username VARCHAR(50) NOT NULL UNIQUE,       -- 管理员用户名
    password VARCHAR(255) NOT NULL,             -- 密码
    role ENUM('super_admin', 'admin') DEFAULT 'admin',  -- 角色类型
    last_login TIMESTAMP,                       -- 上次登录时间
    status ENUM('登录', '未登录') DEFAULT '未登录'  -- 状态
);