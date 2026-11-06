ALTER TABLE extraction_run
    ADD COLUMN parser_name VARCHAR(100) NULL,
    ADD COLUMN parser_media_type VARCHAR(100) NULL,
    ADD COLUMN mapper_name VARCHAR(100) NULL,
    ADD COLUMN mapper_version VARCHAR(50) NULL,
    ADD COLUMN model_provider VARCHAR(100) NULL,
    ADD COLUMN model_name VARCHAR(200) NULL,
    ADD COLUMN mapping_completed_at TIMESTAMPTZ NULL;

ALTER TABLE extraction_review
    ADD COLUMN faculty_cycle_record_id UUID NULL;

UPDATE extraction_review review
SET faculty_cycle_record_id = upload.faculty_cycle_record_id
FROM extraction_run run
JOIN processing_job job ON job.id = run.processing_job_id
JOIN cv_upload upload ON upload.id = job.cv_upload_id
WHERE review.extraction_run_id = run.id;

ALTER TABLE extraction_review
    ALTER COLUMN faculty_cycle_record_id SET NOT NULL,
    ADD CONSTRAINT fk_extraction_review_faculty_cycle_record
        FOREIGN KEY (faculty_cycle_record_id)
            REFERENCES faculty_cycle_record (id)
            ON DELETE RESTRICT;

CREATE UNIQUE INDEX uq_extraction_review_active_faculty_cycle
    ON extraction_review (faculty_cycle_record_id)
    WHERE status = 'DRAFT';
