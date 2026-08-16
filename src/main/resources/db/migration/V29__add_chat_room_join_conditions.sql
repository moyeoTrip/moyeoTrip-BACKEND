ALTER TABLE chat_rooms ADD (
    gender_restriction VARCHAR2(20 CHAR) DEFAULT 'NONE' NOT NULL,
    minimum_age NUMBER(3),
    maximum_age NUMBER(3),
    join_approval_mode VARCHAR2(20 CHAR) DEFAULT 'MANUAL' NOT NULL
);

ALTER TABLE chat_rooms ADD CONSTRAINT ck_chat_room_gender_restriction
    CHECK (gender_restriction IN ('NONE', 'FEMALE_ONLY', 'MALE_ONLY'));

ALTER TABLE chat_rooms ADD CONSTRAINT ck_chat_room_join_approval_mode
    CHECK (join_approval_mode IN ('AUTO', 'MANUAL'));

ALTER TABLE chat_rooms ADD CONSTRAINT ck_chat_room_age_restriction
    CHECK (
        (minimum_age IS NULL OR minimum_age BETWEEN 1 AND 120)
        AND (maximum_age IS NULL OR maximum_age BETWEEN 1 AND 120)
        AND (minimum_age IS NULL OR maximum_age IS NULL OR minimum_age <= maximum_age)
    );
