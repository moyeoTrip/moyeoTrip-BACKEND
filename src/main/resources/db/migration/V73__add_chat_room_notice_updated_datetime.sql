-- BE-30 · 공지 동시 수정 충돌을 서버가 막을 수 있게 수정시각을 남긴다.
--
-- 예전에는 공지 응답이 `noticeId · content · pinned · authorNickname · created_datetime` 뿐이라
-- **충돌을 감지할 값이 없었다.** 두 사람이 같은 공지를 고치면 나중에 저장한 쪽이 앞사람 수정을
-- 말없이 덮어썼고, 앞사람에게는 아무 알림도 없었다 (QA NOTICE-021).
--
-- 기존 행은 수정된 적이 없으므로 생성시각으로 채운다 — 그래야 NOT NULL 로 올릴 수 있고,
-- 화면이 받는 첫 값도 「아직 한 번도 안 고쳤다」는 사실과 맞는다.
ALTER TABLE chat_room_notices ADD updated_datetime TIMESTAMP;

UPDATE chat_room_notices SET updated_datetime = created_datetime WHERE updated_datetime IS NULL;

ALTER TABLE chat_room_notices MODIFY updated_datetime TIMESTAMP NOT NULL;
