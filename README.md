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
