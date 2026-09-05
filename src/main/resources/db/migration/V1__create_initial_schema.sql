CREATE TABLE user_account
(
    id            UUID PRIMARY KEY,
    email         VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE professor
(
    id                    UUID PRIMARY KEY,
    user_account_id       UUID,
    given_name            VARCHAR(100) NOT NULL,
    middle_name           VARCHAR(100),
    family_name           VARCHAR(100) NOT NULL,
    suffix                VARCHAR(50) NULL,
    display_name_override VARCHAR(255) NULL,
    email                 VARCHAR(320) NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_professor_user_account
        UNIQUE (user_account_id)
            CONSTRAINT fk_professor_user_account
            FOREIGN KEY (user_account_id)
            REFERENCES user_account (id)
            ON DELETE SET NULL
);

CREATE TABLE education
(
    id              UUID PRIMARY KEY,
    professor_id    UUID         NOT NULL,
    degree          VARCHAR(150) NOT NULL,
    discipline      VARCHAR(200) NOT NULL,
    institution     VARCHAR(250) NOT NULL,
    graduation_year INTEGER NULL,
    display_order   INTEGER      NOT NULL DEFAULT 0,

    CONSTRAINT fk_education_professor
        FOREIGN KEY (professor_id)
            REFERENCES professor (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_education_graduation_year
        CHECK (
            graduation_year IS NULL
                OR graduation_year BETWEEN 1900 AND 2100
            ),

    CONSTRAINT ck_education_display_order
        CHECK ( display_order >= 0 )
);