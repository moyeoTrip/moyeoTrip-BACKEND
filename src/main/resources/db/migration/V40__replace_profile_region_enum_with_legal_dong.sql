CREATE TABLE user_interested_legal_dongs (
    user_id NUMBER(19) NOT NULL,
    legal_dong_code_id NUMBER(19) NOT NULL,
    CONSTRAINT fk_user_interested_legal_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_interested_legal_code
        FOREIGN KEY (legal_dong_code_id) REFERENCES legal_dong_codes(id),
    CONSTRAINT pk_user_interested_legal_dongs PRIMARY KEY (user_id, legal_dong_code_id)
);

INSERT INTO user_interested_legal_dongs (user_id, legal_dong_code_id)
SELECT selected_region.user_id, legal_code.id
FROM user_interested_regions selected_region
JOIN legal_dong_codes legal_code
  ON legal_code.region_code = '47'
 AND legal_code.signgu_name = CASE selected_region.region
    WHEN 'POHANG' THEN '포항시'
    WHEN 'GYEONGJU' THEN '경주시'
    WHEN 'GIMCHEON' THEN '김천시'
    WHEN 'ANDONG' THEN '안동시'
    WHEN 'GUMI' THEN '구미시'
    WHEN 'YEONGJU' THEN '영주시'
    WHEN 'YEONGCHEON' THEN '영천시'
    WHEN 'SANGJU' THEN '상주시'
    WHEN 'MUNGYEONG' THEN '문경시'
    WHEN 'GYEONGSAN' THEN '경산시'
    WHEN 'UISEONG' THEN '의성군'
    WHEN 'CHEONGSONG' THEN '청송군'
    WHEN 'YEONGYANG' THEN '영양군'
    WHEN 'YEONGDEOK' THEN '영덕군'
    WHEN 'CHEONGDO' THEN '청도군'
    WHEN 'GORYEONG' THEN '고령군'
    WHEN 'SEONGJU' THEN '성주군'
    WHEN 'CHILGOK' THEN '칠곡군'
    WHEN 'YECHEON' THEN '예천군'
    WHEN 'BONGHWA' THEN '봉화군'
    WHEN 'ULJIN' THEN '울진군'
    WHEN 'ULLEUNG' THEN '울릉군'
 END;

DROP TABLE user_interested_regions CASCADE CONSTRAINTS;
