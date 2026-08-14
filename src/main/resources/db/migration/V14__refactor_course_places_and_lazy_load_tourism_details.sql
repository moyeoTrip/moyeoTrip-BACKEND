DELETE FROM travel_course_places;

ALTER TABLE travel_course_places ADD tourism_content_id NUMBER(19);
ALTER TABLE travel_course_places MODIFY tourism_content_id NOT NULL;
ALTER TABLE travel_course_places DROP COLUMN place_name;
ALTER TABLE travel_course_places DROP COLUMN description;

ALTER TABLE travel_course_places ADD CONSTRAINT fk_travel_place_content
    FOREIGN KEY (tourism_content_id) REFERENCES tourism_contents(id);

CREATE INDEX ix_travel_place_content ON travel_course_places(tourism_content_id);
