ALTER TABLE user_account
    ADD COLUMN external_subject VARCHAR(255) NULL,
    ALTER COLUMN password_hash DROP NOT NULL,
    ADD CONSTRAINT uq_user_account_external_subject UNIQUE (external_subject),
    ADD CONSTRAINT ck_user_account_authentication_source
        CHECK (password_hash IS NOT NULL OR external_subject IS NOT NULL);

CREATE UNIQUE INDEX uq_user_account_email_case_insensitive
    ON user_account (LOWER(email));

ALTER TABLE abet_template
    ADD COLUMN department_name VARCHAR(200) NULL;

UPDATE abet_template
SET department_name = 'Computer Science'
WHERE department_name IS NULL;

ALTER TABLE abet_template
    ALTER COLUMN department_name SET NOT NULL;

ALTER TABLE professional_development
    ALTER COLUMN activity_name TYPE VARCHAR(255);

ALTER TABLE cv_upload
    ADD CONSTRAINT ck_cv_upload_size
        CHECK (file_size_bytes > 0),
    ADD CONSTRAINT ck_cv_upload_sha256
        CHECK (file_sha256 ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT ck_cv_upload_expiry
        CHECK (expires_at IS NULL OR expires_at >= uploaded_at);
