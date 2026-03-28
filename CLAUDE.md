# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project
B2B SaaS for nutritionists and fitness coaches in India.
Coaches pay monthly to manage clients, create meal plans, track progress, and handle billing.

## Business Model
- Coach subscriptions via Razorpay: ₹999 / ₹2,499 / ₹4,999 per month
- Coaches serve their own clients through the platform
- Target: 3 paying coaches by Day 30

## Tech Stack
| Layer | Choice |
|---|---|
| Backend | Java 21 + Spring Boot 3.3 (modular monolith) |
| Database | PostgreSQL via Supabase + Liquibase XML migrations |
| Frontend | Next.js 16 + TypeScript |
| API contract | springdoc-openapi → openapi-typescript |
| Payments | Razorpay (UPI, INR, GST invoicing) |
| Messaging | MSG91 (OTP) + WATI (WhatsApp Business API) |
| AI | Spring AI + OpenAI GPT-4o (async job pattern) |
| Storage | AWS S3 ap-south-1 (Mumbai) |
| Deploy | Railway (backend) + Vercel (frontend) + GitHub Actions |
| Rate limiting | bucket4j |
| Mobile (later) | React Native + Expo, Android-first |

## Commands

### Build
```bash
mvn clean package -DskipTests
```

### Run locally
```bash
# 1. Start the Docker test container (same one used for tests)
docker start pg-test
# or first time: docker run -d --name pg-test -p 5433:5432 -e POSTGRES_DB=nutricoach_test -e POSTGRES_USER=nutricoach -e POSTGRES_PASSWORD=nutricoach postgres:16-alpine

# 2. Run backend — no env vars needed
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Dev mode features when running with `local` profile:
- OTP is printed to console — use `111111` as universal bypass OTP
- Razorpay API calls are skipped — dummy checkout URL returned
- JWT expiry is 30 days — no re-login during development
- SQL queries logged to console

### Run all tests
```bash
# One-time Docker setup — creates container with TWO databases:
#   nutricoach_test    → local dev app (application-local.yml)
#   nutricoach_test_it → integration tests (application-test.yml)
docker run -d --name pg-test -p 5433:5432 \
  -e POSTGRES_DB=nutricoach_test \
  -e POSTGRES_USER=nutricoach \
  -e POSTGRES_PASSWORD=nutricoach \
  postgres:16-alpine
docker exec pg-test psql -U nutricoach -d nutricoach_test -c "CREATE DATABASE nutricoach_test_it;"

# Run all tests — no env vars needed, datasource is in application-test.yml
docker start pg-test
mvn test

# Run a single test class
mvn test -Dtest=AuthIntegrationTest
```

### IntelliJ test setup
Just start `pg-test` and click Run — no env var configuration needed.
The datasource URL (`localhost:5433`) lives in `src/test/resources/application-test.yml`.

CI override: set `TEST_DB_URL` env var to point at a CI-managed PostgreSQL instance.

## Architecture — Modular Monolith
Single deployable JAR. 8 Spring modules:

```
com.nutricoach
├── auth          — OTP login, JWT issuance
├── coach         — Coach profile, onboarding
├── client        — Client management (multi-tenant)
├── plans         — Meal plan builder
├── ai            — GPT-4o meal plan generation (async)
├── progress      — Progress logging, photos (S3)
├── billing       — Razorpay subscriptions, feature gating
└── notifications — MSG91 OTP, WATI WhatsApp
```

Each module has: `controller/`, `service/`, `repository/`, `entity/`, `dto/`

Shared code lives in `com.nutricoach.common` (exception, response, security, config, entity).

### Module implementation status
| Module       | Entity | Repo | Service | Controller | Tests |
|--------------|--------|------|---------|------------|-------|
| auth         | ✓      | ✓    | ✓       | ✓          | ✓     |
| coach        | ✓      | ✓    | ✓       | ✓          | ✓     |
| client       | ✓      | ✓    | ✓       | ✓          | ✓     |
| plans        | ✓      | ✓    | ✓       | ✓          | ✓     |
| billing      | ✓      | ✓    | ✓       | ✓          | ✓     |
| progress     | ✓      | ✓    | ✓       | ✓          | ✓     |
| ai           | ✓      | —    | —       | —          | —     |
| notifications| ✓      | —    | —       | —          | —     |

**coach** has `CoachService` (profile, GSTIN validation) + `DashboardService` (analytics).
**billing** has `BillingService` + `RazorpayService` (Razorpay SDK) + `SubscriptionGate` (feature gating per tier) + `WebhookController` (public endpoint, HMAC-verified). Tier limits: TRIAL=5 clients, STARTER=25, PROFESSIONAL=100, ENTERPRISE=∞.
**progress** has `ProgressService` (measurements, S3 photos) + `CheckInService` (date-based adherence).

## Code Patterns

### Entities
- All extend `BaseEntity` (UUID PK, `createdAt`/`updatedAt`, JPA auditing) — except `OtpRequest` (custom ID)
- Soft deletes via `deletedAt Instant` — never hard delete
- Enums as nested static inner classes: `Client.Status`, `MealPlan.Status`
- Use **Lombok** (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`) — JPA requires mutable classes, records don't work with Hibernate
- Package name is `entity`, never `domain` (e.g. `com.nutricoach.client.entity`, not `com.nutricoach.client.domain`)

### DTOs
- Always **Java records** (immutable) — no Lombok needed, records provide canonical constructor, `equals`, `hashCode`, `toString`
- Input: `CreateXxxRequest` / `UpdateXxxRequest` with `@Valid`
- Output: `XxxResponse` — mapped via **MapStruct** only (`@Mapper(componentModel = "spring")`)
- Mappers live in `module/mapper/` package (e.g., `coach/mapper/CoachMapper.java`)
- **Never use manual `from()` factory methods** — always MapStruct
- Lombok annotation processor **must** be listed before MapStruct in `pom.xml` `annotationProcessorPaths`

### Services
- `@Transactional` for writes, `@Transactional(readOnly = true)` for reads
- `@Transactional(noRollbackFor = NutriCoachException.class)` when state must persist on business error (e.g. OTP attempt counter)
- Throw `NutriCoachException.notFound()` / `.badRequest()` / `.conflict()` — never return null or raw exceptions

### Controllers
- `@PreAuthorize("hasRole('COACH')")` at class level
- Always return `ResponseEntity<ApiResponse<T>>`
- HTTP status: `201 CREATED` for POST, `200 OK` for everything else
- Get tenant identity via `SecurityUtils.getCurrentCoachId()` — never from request params

## Database Migrations — Liquibase XML ONLY
- **Never use Flyway. Never use SQL migration files.**
- Master file: `src/main/resources/db/changelog/db.changelog-master.xml`
- Individual changesets: `src/main/resources/db/changelog/changes/NNN-description.xml`
- Always include `<rollback>` blocks in every changeset
- Escape `&` as `&amp;` in XML comments

## Multi-Tenancy
- Every table has `coach_id UUID NOT NULL` as tenant discriminator
- Every `@Repository` query must filter by `coachId`
- Every `@Service` method must validate that the requesting coach owns the resource
- Never return cross-tenant data

## Security
- Phone-first auth (Indian market — no email required)
- OTP via MSG91 → JWT on verify (`dev-mode: true` in test profile prints OTP to logs)
- JWT contains: `sub` (phone), `coachId` (UUID), `role`
- Public endpoints: `/api/v1/auth/**`, `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`
- `@EnableMethodSecurity` active — use `@PreAuthorize` for fine-grained control
- Unauthenticated requests return `401` (configured via `AuthenticationEntryPoint`)

## Testing
- All integration tests extend `AbstractIntegrationTest`
- Tests use a real PostgreSQL container (no H2, no mocks for DB)
- `TEST_DB_URL` env var switches between manual container and Testcontainers auto-mode
- `@BeforeEach` must delete child entities before parent entities (FK constraints)
- Cover: 200/201 happy path, 400 validation, 401 no token, 403 wrong tenant, 404 not found, 409 conflict
- After modifying any constructor, service dependency, or Spring Bean, check and update all test files that mock or instantiate that class before committing

## India-Specific Decisions
- Razorpay not Stripe (UPI support, INR settlement)
- AWS Mumbai (ap-south-1) for DPDP Act 2023 data residency
- Indian food database using IFCT data from NIN
- WhatsApp (WATI) for meal plan sharing and client reminders

## Developer Notes
- Solo founder, ~2 hours/day
- Java/Spring Boot is the strongest skill — lean into it
- Ask before making irreversible decisions (DB schema changes, API contract breaks, external service choices)
- Keep it simple — no over-engineering, no speculative abstractions
- Run `/spring-boot-patterns` before implementing any new module for a full checklist
