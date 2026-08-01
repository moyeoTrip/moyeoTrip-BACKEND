ALTER TABLE users
    ADD (nickname_color VARCHAR2(20));

UPDATE users
SET nickname_color =
        CASE MOD(id, 10)
            WHEN 0 THEN 'RED'
            WHEN 1 THEN 'ORANGE'
            WHEN 2 THEN 'YELLOW'
            WHEN 3 THEN 'GREEN'
            WHEN 4 THEN 'BLUE'
            WHEN 5 THEN 'NAVY'
            WHEN 6 THEN 'PURPLE'
            WHEN 7 THEN 'PINK'
            WHEN 8 THEN 'SKY_BLUE'
            ELSE 'MINT'
        END
WHERE nickname IS NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT ck_users_nickname_color CHECK (
        (nickname IS NULL AND nickname_color IS NULL)
        OR
        (
            nickname IS NOT NULL
            AND nickname_color IN (
                'RED',
                'ORANGE',
                'YELLOW',
                'GREEN',
                'BLUE',
                'NAVY',
                'PURPLE',
                'PINK',
                'SKY_BLUE',
                'MINT'
            )
        )
    );
