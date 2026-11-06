# abet-faculty-vitae-backend
Spring Boot backend for parsing faculty CVs, managing structured records and generating versioned ABET-compliant faculty vitae.

## Local frontend development without NetBadge

Use the opt-in `dev` profile to run the real backend with a fixed local identity. The profile binds
the server to `127.0.0.1`, does not contact NetBadge, and still enforces the same CSRF cookie/header
flow used by the deployed frontend.

```shell
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The default identity is a new faculty user, so the UI starts with the faculty onboarding flow. The
account is provisioned in the local database on its first request. To exercise another identity or
the administrator UI, set any of these before starting the backend:

```shell
export DEV_USER_EMAIL=admin.dev@virginia.edu
export DEV_USER_DISPLAY_NAME='Admin Developer'
export DEV_USER_SURNAME=Developer
export DEV_USER_ROLES=ADMIN,FACULTY
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Then run `pnpm dev` in the frontend repository. Vite proxies `/api` to
`http://localhost:8080`, so no frontend authentication mock or CORS override is needed. Restart the
backend after changing the development identity. Do not enable the `dev` profile in a deployment;
deployed environments must use the `netbadge` profile.

## Local malware scanner

Start the optional ClamAV service before exercising the CV processing pipeline:

```shell
docker compose --profile malware up -d clamav
```

The first start downloads the signature database into the `clamav-signatures` Docker volume and may take several minutes. Check readiness with `docker compose ps clamav`. Stop the container without deleting its signatures with:

```shell
docker compose --profile malware down
```

Delete the signature volume only when a completely fresh scanner database is required:

```shell
docker compose --profile malware down --volumes
```

The local port is bound to `127.0.0.1` and is not exposed on other network interfaces. In a container deployment, set `CLAMAV_HOST` to the private ClamAV service name and do not publish port 3310 publicly.

Production CV processing runs in the `parser-worker` artefact, separate from the web application. It
remains in this repository so the parsing contract and dependency versions can be tested together;
the worker interface must never be exposed as a public API.

## Isolated CV parser

Apache Tika is deliberately absent from the backend application's runtime classpath. It runs in a
separate, disposable parser-worker container with no outbound network, a read-only filesystem, a
bounded tmpfs, a non-root user, dropped Linux capabilities, and CPU, memory, PID and per-document
execution limits.

Generate a fresh local bearer token and start the worker:

```shell
export PARSER_WORKER_TOKEN="$(openssl rand -hex 32)"
docker compose --profile parser up -d --build parser-worker
```

Give the same token to the backend and enable remote extraction:

```shell
export CV_PARSER_WORKER_ENABLED=true
export PARSER_WORKER_TOKEN
./gradlew bootRun
```

The worker is reachable only through `127.0.0.1:8081` during local development. In deployment,
place it on a private internal network, set `CV_PARSER_WORKER_URL` to that private address, and do
not publish the worker port publicly. The worker kills and relies on the container restart policy
to recreate its process if Tika exceeds the configured 30-second parsing deadline.

## End-to-end CV processing

Generate a local token, then start RabbitMQ, ClamAV and the isolated parser:

```shell
export PARSER_WORKER_TOKEN="$(openssl rand -hex 32)"
docker compose --profile processing --profile malware --profile parser up -d --build
```

Start the backend with queue processing enabled. The development profile supplies the local
NetBadge identity while preserving CSRF and role checks:

```shell
export CV_PROCESSING_ENABLED=true
export CV_PARSER_WORKER_ENABLED=true
export PARSER_WORKER_TOKEN
export DEV_USER_EMAIL=faculty.dev@virginia.edu
export DEV_USER_DISPLAY_NAME='Faculty Developer'
export DEV_USER_SURNAME=Developer
export DEV_USER_ROLES=FACULTY,ADMIN
./gradlew bootRun --args='--spring.profiles.active=dev'
```

In the frontend repository:

```shell
unset VITE_API_BASE_URL
corepack pnpm install --frozen-lockfile
corepack pnpm dev
```

The upload transaction writes an outbox row before commit. A confirmed RabbitMQ publisher sends
only the processing-job UUID. The consumer retrieves the private object into an owner-only random
temporary file, verifies its stored size and SHA-256 digest, scans it with ClamAV, and calls the
parser only after a clean result. Retries repeat the scan and exhausted operational failures are
dead-lettered. Document text, filenames, storage credentials and parser credentials are never
placed on the queue.

For production, RabbitMQ, ClamAV and the parser must be reachable only on private service networks.
Do not publish their management, AMQP, scanner or parser ports publicly. Give the web/consumer
process access only to the private CV bucket and these internal endpoints; the parser container
must retain its no-outbound-network, read-only filesystem and resource limits.

## Structured ABET mapping boundary

Sanitised parser text is mapped behind `CvStructuredMapper`. The default implementation is a
deterministic heading mapper with no network, tools, environment access or credentials. It creates
unconfirmed review suggestions for all supported ABET content areas, and strict validation runs
before any review or review item is stored. Mapper/parser provenance records implementation names
and versions only, never secrets.

An AI implementation can be added behind the same interface, but it must use a private model
endpoint with no tools, retrieval or outbound network access. Document text and model output must
remain untrusted, model output must be translated into `StructuredCvDraft`, and it must pass the
existing validator before persistence. No model output can directly confirm or update faculty
records; faculty confirmation remains a separate authenticated action.
