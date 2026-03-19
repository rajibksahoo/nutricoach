# NutriCoach — CLAUDE.md

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
| Frontend | Next.js 14 + TypeScript |
| API contract | springdoc-openapi → openapi-typescript |
| Payments | Razorpay (UPI, INR, GST invoicing) |
| Messaging | MSG91 (OTP) + WATI (WhatsApp Business API) |
| AI | Spring AI + OpenAI GPT-4o (async job pattern) |
| Storage | AWS S3 ap-south-1 (Mumbai) |
| Queue | BullMQ + Redis via Upstash |
| Deploy | Railway (backend) + Vercel (frontend) + GitHub Actions |
| Rate limiting | bucket4j |
| Mobile (later) | React Native + Expo, Android-first |

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

No microservices until a proven bottleneck.

## Database Migrations — Liquibase XML ONLY
- **Never use Flyway. Never use SQL migration files.**
- Master file: `src/main/resources/db/changelog/db.changelog-master.xml`
- Individual changesets: `src/main/resources/db/changelog/changes/NNN-description.xml`
- Always include `<rollback>` blocks in every changeset
- Naming: `001-create-coaches.xml`, `002-create-clients.xml`, etc.

## Multi-Tenancy
- Every table has `coach_id UUID NOT NULL` as tenant discriminator
- Spring Security enforces tenant isolation at the service layer
- Clients belong to exactly one coach
- Never return cross-tenant data — enforce at repository or service level

## API Conventions
- Base path: `/api/v1/`
- All responses wrapped in `ApiResponse<T>` (`success`, `message`, `data`, `errorCode`)
- Validation errors: 400 with field messages joined by ", "
- Auth errors: 401 `UNAUTHORIZED`, tenant violations: 403 `ACCESS_DENIED`
- Business logic errors: use `NutriCoachException` with appropriate HTTP status

## Security
- Phone-first auth (Indian market — no email required)
- OTP via MSG91 → JWT on verify
- JWT contains: `sub` (phone), `coachId` (UUID), `role`
- JWT secret in `app.jwt.secret` (Base64-encoded), expiry in `app.jwt.expiry-hours`
- Public endpoints: `/api/v1/auth/**`, `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`
- `@EnableMethodSecurity` active — use `@PreAuthorize` for fine-grained control

## India-Specific Decisions
- Phone-first (OTP), not email
- Razorpay not Stripe (UPI support, INR settlement)
- AWS Mumbai (ap-south-1) for DPDP Act 2023 data residency
- Indian food database using IFCT data from NIN
- Regional cuisines: South Indian, Gujarati, Punjabi, Jain, Bengali, Rajasthani, Maharashtrian, North-East
- WhatsApp (WATI) for meal plan sharing and client reminders

## UI/UX Direction (Next.js frontend)
- Everfit-inspired 3-column layout
- Left icon nav → sidebar status filter → main content → right context panel
- Client status filters: "needs meal plan / low adherence / check-in due"
- WhatsApp share button prominent in client context panel

## 30-Day Plan
| Week | Focus |
|---|---|
| 1 | Spring Boot scaffold, Liquibase schema, JWT+OTP auth, Next.js scaffold, Railway+Vercel deploy |
| 2 | Client management, Indian food DB, meal plan builder, client portal, coach dashboard |
| 3 | Progress logging, Razorpay subscriptions, WhatsApp reminders, feature gating |
| 4 | Spring AI meal plan generation, branding, landing page, security hardening |

## Developer Notes
- Solo founder, ~2 hours/day
- Java/Spring Boot is the strongest skill — lean into it
- Ask before making irreversible decisions (DB schema changes, API contract breaks, external service choices)
- Prefer detailed, executable steps with specific commands
- Keep it simple — no over-engineering, no speculative abstractions

## Key Files
| File | Purpose |
|---|---|
| `pom.xml` | Maven dependencies (Spring Boot 3.3.6, Java 21) |
| `src/main/resources/application.yml` | App config (DB, JWT, external services) |
| `src/main/resources/db/changelog/db.changelog-master.xml` | Liquibase master |
| `src/main/java/com/nutricoach/NutriCoachApplication.java` | Entry point |
| `src/main/java/com/nutricoach/common/` | Shared: security, exceptions, responses, base entity |
