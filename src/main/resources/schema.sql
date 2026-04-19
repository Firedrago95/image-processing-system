DROP TABLE IF EXISTS image_task;

CREATE TABLE image_tasks (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    external_job_id VARCHAR(100),
    status          VARCHAR(20) NOT NULL,
    result_data     TEXT,
    version         BIGINT          DEFAULT 0,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    CONSTRAINT uk_idempotency_key UNIQUE (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
