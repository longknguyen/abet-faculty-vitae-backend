ALTER TABLE abet_template
    ADD COLUMN review_start_date DATE NULL,
    ADD COLUMN review_end_date DATE NULL,
    ADD COLUMN submission_deadline DATE NULL;

ALTER TABLE abet_template
    ADD CONSTRAINT ck_abet_template_review_dates
        CHECK (
            (review_start_date IS NULL AND review_end_date IS NULL)
            OR
            (review_start_date IS NOT NULL
                AND review_end_date IS NOT NULL
                AND review_end_date >= review_start_date)
        ),
    ADD CONSTRAINT ck_abet_template_submission_deadline
        CHECK (
            submission_deadline IS NULL
            OR
            (review_start_date IS NOT NULL
                AND review_end_date IS NOT NULL
                AND submission_deadline BETWEEN review_start_date AND review_end_date)
        );
