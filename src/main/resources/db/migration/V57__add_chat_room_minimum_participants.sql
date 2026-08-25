ALTER TABLE chat_rooms ADD minimum_participants NUMBER(10) DEFAULT 3 NOT NULL;

ALTER TABLE chat_rooms ADD CONSTRAINT ck_chat_rooms_minimum_participants
    CHECK (minimum_participants BETWEEN 3 AND max_participants);
