-- The original upload endpoint created jobs before the outbox was introduced.
INSERT INTO cv_processing_outbox (id, processing_job_id, created_at)
SELECT gen_random_uuid(), job.id, job.queued_at
FROM processing_job AS job
JOIN cv_upload AS upload ON upload.id = job.cv_upload_id
WHERE job.status = 'QUEUED'
  AND upload.expires_at > CURRENT_TIMESTAMP
  AND upload.temporary_storage_key IS NOT NULL
ON CONFLICT (processing_job_id) DO NOTHING;
