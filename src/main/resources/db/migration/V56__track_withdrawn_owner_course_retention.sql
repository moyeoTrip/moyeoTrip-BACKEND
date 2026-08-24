ALTER TABLE travel_courses ADD retained_after_owner_withdrawal NUMBER(1) DEFAULT 0 NOT NULL;

ALTER TABLE travel_courses ADD CONSTRAINT ck_course_withdrawal_retention
    CHECK (retained_after_owner_withdrawal IN (0, 1));
