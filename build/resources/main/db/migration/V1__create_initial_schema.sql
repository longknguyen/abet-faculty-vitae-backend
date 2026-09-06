CREATE TABLE user_account
(
    id            UUID PRIMARY KEY,
    email         VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_user_account_email
        UNIQUE (email)
);

CREATE TABLE user_account_role
(
    user_account_id UUID        NOT NULL,
    role_name       VARCHAR(20) NOT NULL,

    CONSTRAINT pk_user_account_role
        PRIMARY KEY (user_account_id, role_name),

    CONSTRAINT fk_user_account_role_user
        FOREIGN KEY (user_account_id)
            REFERENCES user_account (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_user_account_role_name
        CHECK (role_name IN ('ADMIN', 'FACULTY'))
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

    CONSTRAINT ck_experience_employment_type
        CHECK (
            employment_type IN ('FULL_TIME', 'PART_TIME')
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

    CONSTRAINT ck_credential_type
        CHECK (
            credential_type IN ('LICENCE', 'AWARD', 'CERTIFICATION', 'OTHER')
            ),

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
        FOREIGN KEY (professor_id)
            REFERENCES professor (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_contribution_type
        CHECK (
            contribution_type IN (
                                  'SERVICE',
                                  'PUBLICATION_PRESENTATION'
                )
            ),

    CONSTRAINT ck_contribution_display_order
        CHECK (display_order >= 0)
);

CREATE INDEX ix_contribution_professor ON contribution (professor_id);

CREATE TABLE abet_template
(
    id                      UUID PRIMARY KEY,
    review_cycle            VARCHAR(50)  NOT NULL,
    revision_number         INTEGER      NOT NULL,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    required_sections       JSONB        NOT NULL DEFAULT '[]'::JSONB,
    formatting_rules        JSONB        NOT NULL DEFAULT '{}'::JSONB,
    rendering_template_name VARCHAR(200) NOT NULL,
    published_at            TIMESTAMPTZ NULL,

    CONSTRAINT uq_abet_template_cycle_revision
        UNIQUE (review_cycle, revision_number),

    CONSTRAINT ck_abet_template_revision
        CHECK (revision_number > 0),

    CONSTRAINT ck_abet_template_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'RETIRED')),

    CONSTRAINT ck_abet_template_published
        CHECK (
            (status = 'DRAFT' AND published_at IS NULL)
                OR
            (status IN ('PUBLISHED', 'RETIRED') AND published_at IS NOT NULL)
            )
);

CREATE TABLE faculty_cycle_record
(
    id                  UUID PRIMARY KEY,
    professor_id        UUID        NOT NULL,
    abet_template_id    UUID        NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    content_revision    INTEGER     NOT NULL DEFAULT 1,
    profile_snapshot    JSONB NULL,
    submitted_at        TIMESTAMPTZ NULL,
    verified_by_user_id UUID NULL,
    verified_at         TIMESTAMPTZ NULL,
    lock_version        BIGINT      NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_faculty_cycle_professor_template
        UNIQUE (professor_id, abet_template_id),

    CONSTRAINT fk_faculty_cycle_professor
        FOREIGN KEY (professor_id)
            REFERENCES professor (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_faculty_cycle_template
        FOREIGN KEY (abet_template_id)
            REFERENCES abet_template (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_faculty_cycle_verifier
        FOREIGN KEY (verified_by_user_id)
            REFERENCES user_account (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_faculty_cycle_status
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'VERIFIED')),

    CONSTRAINT ck_faculty_cycle_content_revision
        CHECK (content_revision > 0),

    CONSTRAINT ck_faculty_cycle_submission
        CHECK (
            status = 'DRAFT'
                OR submitted_at IS NOT NULL
            ),

    CONSTRAINT ck_faculty_cycle_verification
        CHECK (
            (
                status = 'VERIFIED'
                    AND verified_by_user_id IS NOT NULL
                    AND verified_at IS NOT NULL
                )
                OR
            (
                status <> 'VERIFIED'
                    AND verified_by_user_id IS NULL
                    AND verified_at IS NULL
                )
            )
);

CREATE TABLE cv_upload
(
    id                      UUID PRIMARY KEY,
    faculty_cycle_record_id UUID         NOT NULL,
    uploaded_by_user_id     UUID         NOT NULL,
    original_filename       VARCHAR(255) NOT NULL,
    temporary_storage_key   VARCHAR(500) NULL,
    detected_mime_type      VARCHAR(100) NOT NULL,
    file_size_bytes         BIGINT       NOT NULL,
    file_sha256             VARCHAR(64)  NOT NULL,
    uploaded_at             TIMESTAMPTZ  NOT NULL,
    expires_at              TIMESTAMPTZ NULL,
    purged_at               TIMESTAMPTZ NULL,

    CONSTRAINT uq_cv_upload_temporary_storage_key
        UNIQUE (temporary_storage_key),

    CONSTRAINT fk_cv_upload_faculty_cycle_record
        FOREIGN KEY (faculty_cycle_record_id)
            REFERENCES faculty_cycle_record (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_cv_upload_user_account
        FOREIGN KEY (uploaded_by_user_id)
            REFERENCES user_account (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_cv_upload_purge
        CHECK (
            purged_at IS NULL
            OR temporary_storage_key IS NULL
        )
);

CREATE TABLE processing_job
(
    id            UUID PRIMARY KEY,
    cv_upload_id  UUID        NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    current_stage VARCHAR(40) NOT NULL DEFAULT 'QUEUED',
    queued_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at    TIMESTAMPTZ NULL,
    completed_at  TIMESTAMPTZ NULL,
    error_message TEXT NULL,
    lock_version  BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT uq_processing_job_upload
        UNIQUE (cv_upload_id),

    CONSTRAINT fk_processing_job_upload
        FOREIGN KEY (cv_upload_id)
            REFERENCES cv_upload (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_processing_job_status
        CHECK (
            status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED')
            ),

    CONSTRAINT ck_processing_job_stage
        CHECK (
            current_stage IN (
                              'QUEUED',
                              'VALIDATING',
                              'EXTRACTING_TEXT',
                              'STRUCTURING_WITH_AI',
                              'CREATING_REVIEW',
                              'COMPLETE'
                )
            )
);

CREATE TABLE extraction_run
(
    id                 UUID PRIMARY KEY,
    processing_job_id  UUID        NOT NULL,
    run_number         INTEGER     NOT NULL,
    status             VARCHAR(20) NOT NULL,
    extracted_text     TEXT NULL,
    structured_output  JSONB NULL,
    confidence_score   NUMERIC(5, 4) NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at       TIMESTAMPTZ NULL,
    error_message      TEXT NULL,
    payload_expires_at TIMESTAMPTZ NULL,
    payload_purged_at  TIMESTAMPTZ NULL,

    CONSTRAINT uq_extraction_run_number
        UNIQUE (processing_job_id, run_number),

    CONSTRAINT fk_extraction_run_job
        FOREIGN KEY (processing_job_id)
            REFERENCES processing_job (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_extraction_run_number
        CHECK (run_number > 0),

    CONSTRAINT ck_extraction_run_status
        CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED')),

    CONSTRAINT ck_extraction_run_confidence
        CHECK (
            confidence_score IS NULL
                OR confidence_score BETWEEN 0 AND 1
            ),

    CONSTRAINT ck_extraction_run_purge
        CHECK (
            payload_purged_at IS NULL
                OR (
                extracted_text IS NULL
                    AND structured_output IS NULL
                )
            )
);

CREATE TABLE extraction_review
(
    id                  UUID PRIMARY KEY,
    extraction_run_id   UUID        NOT NULL,
    reviewed_by_user_id UUID        NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at        TIMESTAMPTZ NULL,
    lock_version        BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT uq_extraction_review_run
        UNIQUE (extraction_run_id),

    CONSTRAINT fk_extraction_review_run
        FOREIGN KEY (extraction_run_id)
            REFERENCES extraction_run (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_extraction_review_user
        FOREIGN KEY (reviewed_by_user_id)
            REFERENCES user_account (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_extraction_review_status
        CHECK (status IN ('DRAFT', 'CONFIRMED')),

    CONSTRAINT ck_extraction_review_confirmation
        CHECK (
            (status = 'DRAFT' AND confirmed_at IS NULL)
                OR
            (status = 'CONFIRMED' AND confirmed_at IS NOT NULL)
            )
);

CREATE TABLE extraction_review_item
(
    id                   UUID PRIMARY KEY,
    extraction_review_id UUID        NOT NULL,
    section_type         VARCHAR(40) NOT NULL,
    source_text          TEXT NULL,
    ai_suggested_value   JSONB       NOT NULL,
    edited_value         JSONB NULL,
    selected             BOOLEAN     NOT NULL DEFAULT FALSE,
    display_order        INTEGER     NOT NULL DEFAULT 0,
    lock_version         BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT fk_extraction_review_item_review
        FOREIGN KEY (extraction_review_id)
            REFERENCES extraction_review (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_extraction_review_item_section
        CHECK (
            section_type IN (
                             'PROFILE',
                             'EDUCATION',
                             'EXPERIENCE',
                             'CREDENTIAL',
                             'PROFESSIONAL_DEVELOPMENT',
                             'SERVICE',
                             'PUBLICATION_PRESENTATION'
                )
            ),

    CONSTRAINT ck_extraction_review_item_display_order
        CHECK (display_order >= 0)
);

CREATE TABLE generated_document
(
    id                      UUID PRIMARY KEY,
    faculty_cycle_record_id UUID         NOT NULL,
    generated_by_user_id    UUID         NOT NULL,
    document_format         VARCHAR(10)  NOT NULL,
    mime_type               VARCHAR(100) NOT NULL,
    storage_key             VARCHAR(500) NULL,
    file_sha256             VARCHAR(64)     NOT NULL,
    file_size_bytes         BIGINT       NOT NULL,
    source_content_revision INTEGER      NOT NULL,
    page_count              INTEGER NULL,
    validation_status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    validation_details      JSONB NULL,
    generated_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at              TIMESTAMPTZ NULL,
    purged_at               TIMESTAMPTZ NULL,

    CONSTRAINT uq_generated_document_cycle_format
        UNIQUE (faculty_cycle_record_id, document_format),

    CONSTRAINT fk_generated_document_cycle
        FOREIGN KEY (faculty_cycle_record_id)
            REFERENCES faculty_cycle_record (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_generated_document_generator
        FOREIGN KEY (generated_by_user_id)
            REFERENCES user_account (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_generated_document_format
        CHECK (document_format IN ('PDF', 'DOCX')),

    CONSTRAINT ck_generated_document_validation
        CHECK (validation_status IN ('PENDING', 'VALID', 'INVALID')),

    CONSTRAINT ck_generated_document_size
        CHECK (file_size_bytes > 0),

    CONSTRAINT ck_generated_document_revision
        CHECK (source_content_revision > 0),

    CONSTRAINT ck_generated_document_page_count
        CHECK (page_count IS NULL OR page_count > 0),

    CONSTRAINT ck_generated_document_sha256
        CHECK (file_sha256 ~ '^[0-9a-f]{64}$'
) ,

    CONSTRAINT ck_generated_document_purge
        CHECK (
            purged_at IS NULL
            OR storage_key IS NULL
        )
);

CREATE TABLE SPRING_SESSION
(
    PRIMARY_ID               CHAR(36)     NOT NULL,
    SESSION_ID               CHAR(36)     NOT NULL,
    CREATION_TIME            BIGINT       NOT NULL,
    LAST_ACCESS_TIME         BIGINT       NOT NULL,
    MAX_INACTIVE_INTERVAL    INTEGER      NOT NULL,
    EXPIRY_TIME              BIGINT       NOT NULL,
    PRINCIPAL_NAME           VARCHAR(100),

    CONSTRAINT SPRING_SESSION_PK
        PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1
    ON SPRING_SESSION (SESSION_ID);

CREATE INDEX SPRING_SESSION_IX2
    ON SPRING_SESSION (EXPIRY_TIME);

CREATE INDEX SPRING_SESSION_IX3
    ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES
(
    SESSION_PRIMARY_ID    CHAR(36)     NOT NULL,
    ATTRIBUTE_NAME        VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES       BYTEA        NOT NULL,

    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK
        PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),

    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK
        FOREIGN KEY (SESSION_PRIMARY_ID)
            REFERENCES SPRING_SESSION (PRIMARY_ID)
            ON DELETE CASCADE
);

