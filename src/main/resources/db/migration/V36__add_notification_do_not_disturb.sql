ALTER TABLE notification_settings ADD (
    do_not_disturb_enabled NUMBER(1) DEFAULT 0 NOT NULL,
    do_not_disturb_start_time TIMESTAMP,
    do_not_disturb_end_time TIMESTAMP
);

ALTER TABLE notification_settings ADD CONSTRAINT ck_notification_dnd_enabled
    CHECK (do_not_disturb_enabled IN (0, 1));
ALTER TABLE notification_settings ADD CONSTRAINT ck_notification_dnd_times
    CHECK (
        do_not_disturb_enabled = 0 OR
        (do_not_disturb_start_time IS NOT NULL AND
         do_not_disturb_end_time IS NOT NULL AND
         do_not_disturb_start_time <> do_not_disturb_end_time)
    );

CREATE TABLE notification_do_not_disturb_days (
    setting_id NUMBER(19) NOT NULL,
    day_of_week VARCHAR2(10 CHAR) NOT NULL,
    CONSTRAINT fk_notification_dnd_day_setting
        FOREIGN KEY (setting_id) REFERENCES notification_settings(id) ON DELETE CASCADE,
    CONSTRAINT pk_notification_dnd_days PRIMARY KEY (setting_id, day_of_week),
    CONSTRAINT ck_notification_dnd_day CHECK (
        day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')
    )
);
