ALTER TABLE users
    MODIFY (
        user_role VARCHAR2(255 CHAR),
        email VARCHAR2(255 CHAR),
        signup_state VARCHAR2(255 CHAR),
        fcm_token VARCHAR2(255 CHAR),
        nickname VARCHAR2(24 CHAR),
        gender VARCHAR2(255 CHAR),
        profile_file_name VARCHAR2(255 CHAR),
        nickname_color VARCHAR2(20 CHAR)
    );

ALTER TABLE user_auth_identities
    MODIFY (
        provider_type VARCHAR2(255 CHAR),
        provider_user_id VARCHAR2(255 CHAR)
    );

ALTER TABLE user_profile_images
    MODIFY (file_name VARCHAR2(255 CHAR));
