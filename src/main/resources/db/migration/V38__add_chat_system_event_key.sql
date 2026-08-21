ALTER TABLE chat_messages ADD system_event_key VARCHAR2(30 CHAR);

ALTER TABLE chat_messages ADD CONSTRAINT uk_chat_message_system_event
    UNIQUE (chat_room_id, system_event_key);
