CREATE TABLE IF NOT EXISTS login_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(255) NOT NULL,
    ip VARCHAR(50),
    user_agent VARCHAR(500),
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(255),
    login_time DATETIME NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_login_time (login_time)
); 