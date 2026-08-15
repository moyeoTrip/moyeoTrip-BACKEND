ALTER TABLE chat_rooms ADD end_date DATE;

UPDATE chat_rooms
SET end_date = start_date + trip_days - 1;

ALTER TABLE chat_rooms MODIFY end_date NOT NULL;
ALTER TABLE chat_rooms DROP CONSTRAINT ck_chat_rooms_days;
ALTER TABLE chat_rooms DROP COLUMN trip_days;

UPDATE chat_rooms
SET max_participants = 12
WHERE max_participants > 12;

ALTER TABLE chat_rooms DROP CONSTRAINT ck_chat_rooms_capacity;
ALTER TABLE chat_rooms ADD CONSTRAINT ck_chat_rooms_capacity
    CHECK (max_participants BETWEEN 3 AND 12);

ALTER TABLE chat_rooms MODIFY (
    meeting_latitude NULL,
    meeting_longitude NULL
);

ALTER TABLE chat_rooms ADD CONSTRAINT ck_chat_rooms_meeting_coordinates CHECK (
    (meeting_latitude IS NULL AND meeting_longitude IS NULL) OR
    (meeting_latitude IS NOT NULL AND meeting_longitude IS NOT NULL)
);

ALTER TABLE travel_courses DROP CONSTRAINT uk_travel_course_source_content;
ALTER TABLE travel_courses DROP CONSTRAINT fk_travel_course_source_content;
ALTER TABLE travel_courses DROP COLUMN source_content_id;
