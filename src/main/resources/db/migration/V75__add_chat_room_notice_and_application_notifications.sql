-- BE-33 · 공지 등록·참가 신청 결과 알림.
-- 화면은 「공지를 올리면 방 사람들에게 알림이 가요」와 「결과는 알림으로 알려드려요」라고 약속하는데
-- 서버는 셋 다 만들지 않았다. 신청자는 승인도 거절도 알 길이 없어 직접 화면을 다시 여는 수밖에 없었다.
ALTER TABLE notifications DROP CONSTRAINT ck_notification_type;

ALTER TABLE notifications ADD CONSTRAINT ck_notification_type CHECK (
    notification_type IN (
        'CHAT_ROOM_CREATED',
        'CHAT_ROOM_KICKED',
        'CHAT_ROOM_WAITLIST_PROMOTED',
        'CHAT_ROOM_CONFIRMED',
        'CHAT_ROOM_CANCELLED',
        'CHAT_ROOM_NOTICE_POSTED',
        'CHAT_ROOM_APPLICATION_APPROVED',
        'CHAT_ROOM_APPLICATION_REJECTED',
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
