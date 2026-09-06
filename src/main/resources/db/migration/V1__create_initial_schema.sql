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

    CONSTRAINT uq_professor_user_account
        UNIQUE (user_account_id),
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

CREATE TABLE experience
(
    id              UUID PRIMARY KEY,
    professor_id    UUID         NOT NULL,
    experience_type VARCHAR(20)  NOT NULL,
    organisation    VARCHAR(255) NOT NULL,
    academic_rank   VARCHAR(100) NULL,
    position_title  VARCHAR(200) NOT NULL,
    start_year      INTEGER      NOT NULL,
    end_year        INTEGER NULL,
    is_current      BOOLEAN      NOT NULL DEFAULT FALSE,
    employment_type VARCHAR(20)  NOT NULL,
    display_order   INTEGER      NOT NULL DEFAULT 0,

    CONSTRAINT fk_experience_professor
        FOREIGN KEY (professor_id)
            REFERENCES professor (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_experience_type
        CHECK (
            experience_type IN ('ACADEMIC', 'INDUSTRY', 'OTHER')
            ),
    CONSTRAINT ck_experience_years
        CHECK (
            (start_year BETWEEN 1900 AND 2100) AND (end_year BETWEEN start_year AND 2100)
            ),
    CONSTRAINT ck_display_order
        CHECK (
            display_order >= 0
            )
);


CREATE TABLE credential
(
    id                   UUID PRIMARY KEY,
    professor_id         UUID         NOT NULL,
    credential_type      VARCHAR(20)  NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    issuing_organisation VARCHAR(100) NULL,
    credential_number    VARCHAR(200) NULL,
    issue_date           DATE NULL,
    expiration_date      DATE NULL,
    display_order        INTEGER      NOT NULL DEFAULT 0,

    CONSTRAINT fk_credential_professor
        FOREIGN KEY (professor_id) REFERENCES professor (id) ON DELETE RESTRICT,

    CONSTRAINT ck_issue_date_expiration_date
        CHECK (
            (issue_date IS NULL)
                OR
            (expiration_date IS NULL)
                OR
            (expiration_date >= issue_date)
            ),
    CONSTRAINT ck_display_order
        CHECK (
            display_order >= 0
            )
);

CREATE TABLE professional_development
(
    id            UUID PRIMARY KEY,
    professor_id  UUID        NOT NULL,
    activity_name VARCHAR(20) NOT NULL,
    provider      VARCHAR(255) NULL,
    start_date    DATE NULL,
    end_date      DATE NULL,
    description   TEXT NULL,
    display_order INTEGER     NOT NULL DEFAULT 0,

    CONSTRAINT fk_professional_development_professor
        FOREIGN KEY (professor_id) REFERENCES professor (id) ON DELETE RESTRICT,

    CONSTRAINT ck_start_date_end_date
        CHECK (
            (start_date IS NULL)
                OR
            (end_date IS NULL)
                OR
            (end_date >= start_date)
            ),
    CONSTRAINT ck_display_order
        CHECK (
            display_order >= 0
            )
);


CREATE TABLE contribution
(
    id                    UUID PRIMARY KEY,
    professor_id          UUID         NOT NULL,
    contribution_type     VARCHAR(20)  NOT NULL,
    title                 VARCHAR(255) NOT NULL,
    citation              TEXT NULL,
    venue_or_organisation VARCHAR(100) NULL,
    contribution_date     DATE NULL,
    description           TEXT NULL,
    display_order         INTEGER      NOT NULL DEFAULT 0,

    CONSTRAINT fk_contribution_professor
        FOREIGN KEY (professor_id) REFERENCES professor (id) ON DELETE RESTRICT,

    CONSTRAINT ck_start_date_end_date
        CHECK (
            (start_date IS NULL)
                OR
            (end_date IS NULL)
                OR
            (end_date >= start_date)
            ),
    CONSTRAINT ck_display_order
        CHECK (
            display_order >= 0
            ),
    CONSTRAINT ck_contribution_type
        CHECK (
            contribution_type IN ('SERVICE', 'PUBLICATION_PRESENTATION')
            )
);

CREATE INDEX ix_contribution_professor ON contribution (professor_id);

CREATE TABLE cv_upload
(
    id                    UUID PRIMARY KEY,
    professor_id          UUID         NOT NULL,
    uploaded_by_user_id   UUID         NOT NULL,
    original_filename     VARCHAR(20)  NOT NULL,
    temporary_storage_key VARCHAR(500) NOT NULL UNIQUE,
    detected_mime_type    VARCHAR(100) NOT NULL,
    file_size_bytes       BIGINT       NOT NULL,
    file_sha256           VARCHAR(64) NOT NULL,
    uploaded_at           TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_cv_upload_professor
        FOREIGN KEY (professor_id) REFERENCES professor (id) ON DELETE RESTRICT,

    CONSTRAINT fk_cv_upload_user_account
        FOREIGN KEY (uploaded_by_user_id) REFERENCES user_account (id) ON DELETE RESTRICT,

);





