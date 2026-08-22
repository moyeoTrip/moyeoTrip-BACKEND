ALTER TABLE notifications DROP CONSTRAINT ck_notification_type;

ALTER TABLE notifications ADD CONSTRAINT ck_notification_type CHECK (
    notification_type IN (
        'CHAT_ROOM_CREATED',
        'CHAT_ROOM_KICKED',
        'CHAT_MESSAGE_RECEIVED',
        'TRAVEL_COURSE_UPDATED',
        'MEETING_INFO_UPDATED',
        'RECRUITMENT_DEADLINE',
        'FRIEND_REQUEST',
        'FRIEND_ACCEPTED',
        'FEED_LIKE',
        'MARKETING'
    )
);
