ALTER TABLE chat_messages DROP CONSTRAINT uk_chat_message_system_event;

CREATE UNIQUE INDEX uk_chat_message_system_event
    ON chat_messages (
        CASE WHEN system_event_key IS NOT NULL THEN chat_room_id END,
        CASE WHEN system_event_key IS NOT NULL THEN system_event_key END
    );
