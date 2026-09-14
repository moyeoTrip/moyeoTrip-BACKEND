-- BE-17: 거절 이력 조회 API 를 열면서 「언제 거절했는지」를 남긴다.
-- chat_room_join_applications 는 BaseTimeEntity(생성 시각만) 라 거절 시각을 가진 컬럼이 없었다.
-- 이미 REJECTED 로 남아 있는 기존 행은 시각을 알 수 없어 NULL 로 둔다(응답에서 신청 시각으로 대체).
ALTER TABLE chat_room_join_applications ADD rejected_datetime TIMESTAMP;

CREATE INDEX ix_chat_applications_rejected ON chat_room_join_applications(chat_room_id, status, rejected_datetime);
