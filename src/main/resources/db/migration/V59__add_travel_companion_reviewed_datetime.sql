ALTER TABLE travel_companions ADD reviewed_datetime TIMESTAMP;

UPDATE travel_companions
SET reviewed_datetime = created_datetime
WHERE manner_score IS NOT NULL OR one_line_review IS NOT NULL;

CREATE INDEX ix_travel_companions_target_reviewed
    ON travel_companions(companion_id, reviewed_datetime DESC);
