ALTER TABLE users ADD introduction VARCHAR2(300 CHAR);

CREATE TABLE user_travel_styles (
    user_id NUMBER(19) NOT NULL,
    travel_style VARCHAR2(30 CHAR) NOT NULL,
    CONSTRAINT fk_user_travel_style_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT pk_user_travel_styles PRIMARY KEY (user_id, travel_style),
    CONSTRAINT ck_user_travel_style CHECK (travel_style IN (
        'NATURE', 'PHOTOGRAPHY', 'SEA', 'MOUNTAIN', 'TREKKING', 'CAMPING',
        'FOOD', 'CAFE', 'HISTORY_CULTURE', 'HEALING', 'ACTIVITY', 'FESTIVAL'
    ))
);

CREATE TABLE user_interested_regions (
    user_id NUMBER(19) NOT NULL,
    region VARCHAR2(30 CHAR) NOT NULL,
    CONSTRAINT fk_user_interested_region_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT pk_user_interested_regions PRIMARY KEY (user_id, region),
    CONSTRAINT ck_user_interested_region CHECK (region IN (
        'POHANG', 'GYEONGJU', 'GIMCHEON', 'ANDONG', 'GUMI', 'YEONGJU', 'YEONGCHEON',
        'SANGJU', 'MUNGYEONG', 'GYEONGSAN', 'UISEONG', 'CHEONGSONG', 'YEONGYANG',
        'YEONGDEOK', 'CHEONGDO', 'GORYEONG', 'SEONGJU', 'CHILGOK', 'YECHEON',
        'BONGHWA', 'ULJIN', 'ULLEUNG'
    ))
);

ALTER TABLE notification_settings ADD chat_notification_mode VARCHAR2(30 CHAR) DEFAULT 'ALL' NOT NULL;
UPDATE notification_settings
SET chat_notification_mode = CASE WHEN chat_message_enabled = 1 THEN 'ALL' ELSE 'NONE' END;
ALTER TABLE notification_settings ADD CONSTRAINT ck_notification_chat_mode
    CHECK (chat_notification_mode IN ('ALL', 'MENTIONS_AND_REPLIES', 'NONE'));
ALTER TABLE notification_settings DROP COLUMN chat_message_enabled;

ALTER TABLE chat_messages ADD reply_to_message_id NUMBER(19);
ALTER TABLE chat_messages ADD CONSTRAINT fk_chat_message_reply
    FOREIGN KEY (reply_to_message_id) REFERENCES chat_messages(id) ON DELETE SET NULL;
CREATE INDEX ix_chat_messages_reply ON chat_messages(reply_to_message_id);
