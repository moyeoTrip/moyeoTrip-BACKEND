DECLARE
    applied_v8_count NUMBER;
    legacy_table_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO applied_v8_count
    FROM "flyway_schema_history"
    WHERE "version" = '8'
      AND "success" = 1;

    SELECT COUNT(*)
    INTO legacy_table_count
    FROM user_tables
    WHERE table_name = 'CHAT_ROOM_READ_STATUSES';

    IF applied_v8_count = 0 AND legacy_table_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE chat_room_read_statuses (
                chat_room_id NUMBER(19) NOT NULL,
                user_id NUMBER(19) NOT NULL,
                last_read_message_id NUMBER(19) NOT NULL
            )';
    END IF;
END;
/
