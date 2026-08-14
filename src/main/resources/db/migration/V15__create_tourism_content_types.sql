CREATE TABLE tourism_content_types (
    code NUMBER(3) PRIMARY KEY,
    name VARCHAR2(30 CHAR) NOT NULL,
    CONSTRAINT uk_tourism_content_type_name UNIQUE (name)
);

INSERT INTO tourism_content_types (code, name) VALUES (12, '관광지');
INSERT INTO tourism_content_types (code, name) VALUES (14, '문화시설');
INSERT INTO tourism_content_types (code, name) VALUES (15, '축제공연행사');
INSERT INTO tourism_content_types (code, name) VALUES (25, '여행코스');
INSERT INTO tourism_content_types (code, name) VALUES (28, '레포츠');
INSERT INTO tourism_content_types (code, name) VALUES (32, '숙박');
INSERT INTO tourism_content_types (code, name) VALUES (38, '쇼핑');
INSERT INTO tourism_content_types (code, name) VALUES (39, '음식점');

ALTER TABLE tourism_contents ADD CONSTRAINT fk_tourism_content_type
    FOREIGN KEY (content_type_id) REFERENCES tourism_content_types(code);
