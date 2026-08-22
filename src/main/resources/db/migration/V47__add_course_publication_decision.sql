ALTER TABLE travel_courses ADD (
    publication_status VARCHAR2(20 CHAR) DEFAULT 'NOT_REQUESTED' NOT NULL,
    show_creator_nickname NUMBER(1) DEFAULT 1 NOT NULL
);

UPDATE travel_courses
SET publication_status = 'PUBLISHED'
WHERE type = 'PUBLIC';

ALTER TABLE travel_courses ADD CONSTRAINT ck_travel_course_publication_status CHECK (
    publication_status IN ('NOT_REQUESTED', 'PENDING', 'DECLINED', 'PUBLISHED')
);

ALTER TABLE travel_courses ADD CONSTRAINT ck_travel_course_publication_type CHECK (
    (type = 'PUBLIC' AND publication_status = 'PUBLISHED')
    OR (type = 'CUSTOM' AND publication_status <> 'PUBLISHED')
);

ALTER TABLE travel_courses ADD CONSTRAINT ck_travel_course_show_creator CHECK (
    show_creator_nickname IN (0, 1)
);

CREATE INDEX ix_travel_course_publication_owner
    ON travel_courses(owner_id, publication_status, updated_datetime DESC);

ALTER TABLE notifications DROP CONSTRAINT ck_notification_type;
ALTER TABLE notifications ADD CONSTRAINT ck_notification_type CHECK (
    notification_type IN (
        'CHAT_ROOM_CREATED',
        'CHAT_MESSAGE_RECEIVED',
        'TRAVEL_COURSE_UPDATED',
        'MEETING_INFO_UPDATED',
        'RECRUITMENT_DEADLINE',
        'FRIEND_REQUEST',
        'FRIEND_ACCEPTED',
        'FEED_LIKE',
        'COURSE_PUBLICATION_REQUESTED',
        'MARKETING'
    )
);
