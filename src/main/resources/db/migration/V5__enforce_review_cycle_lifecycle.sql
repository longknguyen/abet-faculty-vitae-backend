WITH ranked_published_templates AS (
    SELECT id,
           ROW_NUMBER() OVER (
               ORDER BY published_at DESC NULLS LAST, revision_number DESC, id DESC
           ) AS publication_rank
    FROM abet_template
    WHERE status = 'PUBLISHED'
)
UPDATE abet_template
SET status = 'RETIRED'
WHERE id IN (
    SELECT id
    FROM ranked_published_templates
    WHERE publication_rank > 1
);

CREATE UNIQUE INDEX uq_abet_template_single_published
    ON abet_template (status)
    WHERE status = 'PUBLISHED';

ALTER TABLE abet_template
    ADD CONSTRAINT ck_abet_template_required_sections_json
        CHECK (jsonb_typeof(required_sections) = 'array'),
    ADD CONSTRAINT ck_abet_template_formatting_rules_json
        CHECK (jsonb_typeof(formatting_rules) = 'object');
