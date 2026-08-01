ALTER TABLE user_auth_identities
    DROP CONSTRAINT ck_auth_provider;

ALTER TABLE user_auth_identities
    ADD CONSTRAINT ck_auth_provider
        CHECK (provider_type IN ('EMAIL', 'KAKAO', 'APPLE', 'GOOGLE'));
