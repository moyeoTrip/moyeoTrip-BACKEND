ALTER TABLE feeds DROP COLUMN title;

ALTER TABLE feeds ADD visibility VARCHAR2(20 CHAR) DEFAULT 'PUBLIC' NOT NULL;

ALTER TABLE feeds ADD CONSTRAINT ck_feeds_visibility
    CHECK (visibility IN ('PUBLIC', 'FRIENDS', 'PRIVATE'));

CREATE INDEX ix_feeds_visibility_created ON feeds(visibility, created_datetime DESC);
