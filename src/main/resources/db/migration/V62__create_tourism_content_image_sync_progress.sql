CREATE TABLE tourism_content_image_sync_progress (
    sync_name VARCHAR2(50 CHAR) PRIMARY KEY,
    last_content_id NUMBER(19),
    completed NUMBER(1) DEFAULT 0 NOT NULL,
    created_datetime TIMESTAMP NOT NULL,
    updated_datetime TIMESTAMP NOT NULL,
    CONSTRAINT ck_tourism_image_sync_progress_completed CHECK (completed IN (0, 1))
);
