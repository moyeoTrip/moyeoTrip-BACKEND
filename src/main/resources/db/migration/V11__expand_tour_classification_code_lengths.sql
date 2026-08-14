ALTER TABLE tour_classification_codes
    MODIFY (
        level1_code VARCHAR2(3 CHAR),
        level2_code VARCHAR2(5 CHAR),
        level3_code VARCHAR2(9 CHAR)
    );
