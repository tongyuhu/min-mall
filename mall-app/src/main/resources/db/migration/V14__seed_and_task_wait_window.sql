-- 人工重试拥有新的等待期限，保留原始创建时间用于审计。
ALTER TABLE integration_task ADD COLUMN wait_started_at DATETIME(6) NULL;
UPDATE integration_task SET wait_started_at=created_at;
ALTER TABLE integration_task MODIFY COLUMN wait_started_at DATETIME(6) NOT NULL;
-- 仅记录本地演示初始化的版本；业务数据仍通过业务服务写入。
CREATE TABLE local_seed_state(seed_key VARCHAR(64) PRIMARY KEY,completed BOOLEAN NOT NULL DEFAULT FALSE,completed_at DATETIME(6) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
