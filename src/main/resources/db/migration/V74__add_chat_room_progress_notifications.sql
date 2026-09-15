-- BE-32 · 대기자 승격·여행 확정·여행 불발 알림.
-- 예전에는 세 경우 모두 알림을 만들지 않아, 화면이 약속한 「동행자 모두에게 알림이 가요」가 지켜지지 않았다.
ALTER TABLE notifications DROP CONSTRAINT ck_notification_type;

ALTER TABLE notifications ADD CONSTRAINT ck_notification_type CHECK (
    notification_type IN (
        'CHAT_ROOM_CREATED',
        'CHAT_ROOM_KICKED',
        'CHAT_ROOM_WAITLIST_PROMOTED',
        'CHAT_ROOM_CONFIRMED',
        'CHAT_ROOM_CANCELLED',
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
