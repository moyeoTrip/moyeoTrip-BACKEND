ALTER TABLE chat_rooms DROP CONSTRAINT ck_chat_rooms_capacity;

ALTER TABLE chat_rooms ADD CONSTRAINT ck_chat_rooms_capacity
    CHECK (max_participants BETWEEN 3 AND 20);
