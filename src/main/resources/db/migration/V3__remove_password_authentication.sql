ALTER TABLE user_account
    DROP CONSTRAINT ck_user_account_authentication_source,
    DROP COLUMN password_hash;
