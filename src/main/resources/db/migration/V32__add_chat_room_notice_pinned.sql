ALTER TABLE chat_room_notices ADD pinned NUMBER(1) DEFAULT 0 NOT NULL;

ALTER TABLE chat_room_notices ADD CONSTRAINT ck_chat_room_notice_pinned
    CHECK (pinned IN (0, 1));

CREATE INDEX ix_chat_notices_pinned
    ON chat_room_notices(chat_room_id, pinned, created_datetime DESC, id DESC);
