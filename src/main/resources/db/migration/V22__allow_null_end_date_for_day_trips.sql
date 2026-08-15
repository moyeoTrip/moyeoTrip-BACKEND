ALTER TABLE chat_rooms MODIFY end_date NULL;

UPDATE chat_rooms
SET end_date = NULL
WHERE end_date = start_date;
