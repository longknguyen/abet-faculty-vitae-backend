CREATE TABLE cv_processing_outbox
(
    id                UUID PRIMARY KEY,
    processing_job_id UUID        NOT NULL,
    attempt_count     INTEGER     NOT NULL DEFAULT 0,
    next_attempt_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at      TIMESTAMPTZ NULL,
    last_error        VARCHAR(500) NULL,
    lock_version      BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT uq_cv_processing_outbox_job
        UNIQUE (processing_job_id),

    CONSTRAINT fk_cv_processing_outbox_job
        FOREIGN KEY (processing_job_id)
            REFERENCES processing_job (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_cv_processing_outbox_attempts
        CHECK (attempt_count >= 0)
);

CREATE INDEX ix_cv_processing_outbox_pending
    ON cv_processing_outbox (next_attempt_at, created_at)
    WHERE published_at IS NULL;
