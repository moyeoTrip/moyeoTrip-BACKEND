ALTER TABLE travel_courses ADD source_content_id NUMBER(19);

ALTER TABLE travel_courses ADD CONSTRAINT fk_travel_course_source_content
    FOREIGN KEY (source_content_id) REFERENCES tourism_contents(id);

ALTER TABLE travel_courses ADD CONSTRAINT uk_travel_course_source_content
    UNIQUE (source_content_id);
