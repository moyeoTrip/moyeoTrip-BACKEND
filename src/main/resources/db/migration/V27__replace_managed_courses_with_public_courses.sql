ALTER TABLE travel_courses DROP CONSTRAINT ck_travel_course_owner;
ALTER TABLE travel_courses DROP CONSTRAINT ck_travel_course_type;

UPDATE travel_courses SET type = 'PUBLIC' WHERE type = 'MANAGED';

ALTER TABLE travel_courses ADD CONSTRAINT ck_travel_course_type
    CHECK (type IN ('CUSTOM', 'PUBLIC'));

ALTER TABLE travel_courses ADD CONSTRAINT ck_travel_course_owner
    CHECK ((type = 'CUSTOM' AND owner_id IS NOT NULL) OR type = 'PUBLIC');
