# NutriCoach — 30-Day MVP Progress Tracker

> **Instructions for Claude**: At the start of each session, read this file first.
> Update task statuses as work is completed. Add notes under each day.
> Target: 3 paying coaches by Day 30.

---

## Quick Status

| Week | Theme | Status |
|------|-------|--------|
| Week 1 (Days 1–7) | Scaffold + Auth + Schema + Deploy | ✅ **Complete** (frontend scaffold deferred, now done on web repo) |
| Week 2 (Days 8–14) | Client Mgmt + Meal Plans + Dashboard | ✅ **Backend complete** (frontend on `nutricoach-web`) |
| Week 3 (Days 15–21) | Progress + Billing + WhatsApp | ✅ **Backend complete** |
| Week 4 (Days 22–30) | AI + Branding + Launch | 🟡 **In Progress** (Days 22, 26 done; 23–30 mostly pending) |

**Current Day**: ~27 (as of 2026-04-09)
**Next task**: Wire frontend AI generate button + WhatsApp share → then Day 23 polish, Day 25 branding, Day 27 monitoring

**Bonus delivered (not in original 30-day plan)**: Client Portal — separate client-role JWT auth (Phase 1) + read-only portal endpoints under `/api/v1/portal/*` (Phase 2). On branch `feat/client-portal-auth`, awaiting PR.

---

## Week 1 — Foundation (Days 1–7)

### Day 1 ✅ Spring Boot Scaffold + Auth
- [x] Spring Boot 3.3 project with 8-module structure
- [x] `pom.xml` with all dependencies (JWT, Liquibase, OpenAPI, bucket4j)
- [x] `application.yml` with full config (DB, JWT, MSG91, WATI, Razorpay, S3, OpenAI)
- [x] `.env.example` with all environment variables
- [x] `BaseEntity` (UUID, createdAt, updatedAt with JPA auditing)
- [x] `ApiResponse<T>` wrapper
- [x] `NutriCoachException` + `GlobalExceptionHandler`
- [x] `SecurityConfig` (stateless, JWT, public paths)
- [x] `JwtService` (generate + validate, coachId + role claims)
- [x] `JwtAuthenticationFilter`
- [x] `AuthController` — POST `/api/v1/auth/otp/send` + `/api/v1/auth/otp/verify`
- [x] `AuthService` — OTP flow (6-digit, 10-min TTL, 3 attempts, 60-sec cooldown)
- [x] `Msg91Service` — SMS with dev-mode logging
- [x] `CoachUserDetailsService`
- [x] `OtpRequest` entity + repository
- [x] `OpenApiConfig` (Swagger with JWT bearer)
- [x] `JpaConfig` (@EnableJpaAuditing)
- [x] `NutriCoachApplication` (@EnableAsync)

### Day 2 ✅ Liquibase Schema (11 changesets)
- [x] `001-create-coaches.xml` — subscription tiers, trial, Razorpay
- [x] `002-create-clients.xml` — multi-tenant, JSONB health data
- [x] `003-create-meal-plans.xml` — status, ai_generated flag
- [x] `004-create-meal-plan-days-meals-items.xml` — 3-level plan structure
- [x] `005-create-food-items.xml` — Indian food DB, IFCT, cuisines
- [x] `006-create-progress.xml` — measurements, adherence, S3 photos
- [x] `007-create-check-ins.xml` — date-based adherence tracking
- [x] `008-create-billing.xml` — subscriptions + invoices with GST
- [x] `009-create-otp-requests.xml` — BCrypt hash, attempts, expiry
- [x] `010-create-ai-jobs.xml` — async job tracking with JSONB
- [x] `011-create-notification-logs.xml` — SMS + WhatsApp logs
- [x] All entities: Coach, Client, MealPlan, MealPlanDay, Meal, MealItem, FoodItem, ProgressLog, ProgressPhoto, CheckIn, Subscription, Invoice, AiJob, NotificationLog

### Days 3–7 🔲 Frontend Scaffold + CI/CD + Deploy (skipping — backend first)
- [ ] **Day 3**: Next.js 14 + TypeScript scaffold
  - `npx create-next-app@latest frontend --typescript --tailwind --app`
  - Everfit-inspired 3-column layout shell
  - Auth pages (phone input + OTP input)
  - Axios client with JWT interceptor
  - openapi-typescript type generation from `/v3/api-docs`
- [ ] **Day 4**: Railway + Vercel deployment
  - `railway.json` config
  - `vercel.json` config
  - GitHub Actions CI: test → build → deploy-backend → deploy-frontend
  - Supabase DB connection verified in prod
- [ ] **Day 5**: Coach onboarding flow
  - `CoachController` — GET/PUT `/api/v1/coach/me` (profile)
  - `CoachService` — get profile, update name/business/email/gstin
  - `CoachDto` (request + response)
  - Frontend: onboarding wizard (name → business → done)
- [ ] **Day 6**: Buffer / polish Week 1
  - Fix any deploy issues
  - Verify OTP auth works end-to-end on Railway
  - Swagger UI accessible on prod
- [ ] **Day 7**: Review & plan Week 2

---

## Week 2 — Core Product (Days 8–14)

### Day 8 ✅ Client Management — CRUD
- [x] `SecurityUtils` — resolves coachId from SecurityContext via CoachRepository
- [x] `ClientRepository` — `findByCoachId`, `findByCoachIdAndStatus`, `findByIdAndCoachId`, duplicate phone check
- [x] `CreateClientRequest` — phone (Indian regex), name, email, health fields, JSONB lists
- [x] `UpdateClientRequest` — all optional patch fields, status transition
- [x] `ClientResponse` — full client record mapped from entity
- [x] `ClientService` — tenant-isolated CRUD, soft-delete, conflict check on phone
- [x] `ClientController` — GET/POST `/api/v1/clients`, GET/PUT/DELETE `/api/v1/clients/{id}`, `?status=` filter
- [ ] Frontend: client list page with status filter sidebar (Next.js not yet scaffolded)

### Day 9 ✅ Indian Food Database Seed
- [ ] Liquibase changeset `012-seed-food-items.xml` — ~100 common Indian foods
  - Grains: rice (basmati, brown), roti/chapati, poha, idli, dosa batter
  - Proteins: dal (toor, moong, chana), paneer, chicken breast, eggs, tofu
  - Vegetables: spinach, tomato, onion, cauliflower, okra, bitter gourd
  - Fruits: banana, mango, apple, papaya, pomegranate
  - Dairy: milk, curd/yogurt, ghee, cheese
  - Snacks: roasted chana, makhana, sprouts
- [x] `012-seed-food-items.xml` — 100 Indian foods (IFCT data), with rollback
- [x] `FoodItemRepository` — JPQL search by name/nameHindi, filter by cuisine + category
- [x] `FoodItemMapper` (MapStruct) — FoodItem → FoodItemResponse
- [x] `FoodItemService` — search + findById
- [x] `FoodItemController` — GET `/api/v1/food-items?q=&cuisine=&category=`, GET `/api/v1/food-items/{id}`
- [x] MapStruct added to pom.xml (v1.5.5.Final, Lombok before MapStruct in compiler plugin)

### Day 10 ✅ Meal Plan Builder — API
- [x] `MealPlanRepository`, `MealPlanDayRepository`, `MealRepository`, `MealItemRepository`
- [x] DTOs: `CreateMealPlanRequest`, `UpdateMealPlanRequest`, `MealPlanSummaryResponse`, `MealPlanResponse`, `MealPlanDayResponse`, `MealResponse`, `MealItemResponse`, `AddMealRequest`, `UpdateMealRequest`, `AddMealItemRequest`
- [x] `MealPlanMapper` (MapStruct) — `MealPlan → MealPlanSummaryResponse`
- [x] `MealPlanService` — full CRUD + day/meal/item management, auto nutrition recalc, batch food name lookup (no N+1)
- [x] `MealPlanController` — all endpoints in one controller (15 endpoints across 4 resource levels)
- [x] Tenant isolation: clientId verified to belong to coachId before every plan operation
- [x] Nutrition auto-calc: per-item (quantity/100 × macro per 100g) + day totals recalculated on every item add/remove

### Day 11 🔲 Meal Plan Builder — Frontend
- [ ] Meal plan list page (per client)
- [ ] Drag-and-drop day editor (7-day grid)
- [ ] Food item search modal with nutritional preview
- [ ] Macro totals display per day + per meal

### Day 12 🔲 Client Portal (read-only)
- [ ] Public-facing client view (no login required, magic link or PIN)
- [ ] Client can see their active meal plan
- [ ] WhatsApp share button (deep link to client portal)
- [ ] PDF export of meal plan

### Day 13 ✅ Coach Profile + Dashboard API
- [x] `UpdateCoachRequest` — name, email, businessName, gstin (with Indian GSTIN regex validation)
- [x] `CoachResponse` — profile + subscription tier/status + trialEndsAt
- [x] `CoachMapper` (MapStruct) — Coach → CoachResponse
- [x] `CoachService` — getProfile, updateProfile (patch semantics)
- [x] `CoachController` — GET/PUT `/api/v1/coach/me`, GET `/api/v1/coach/dashboard`
- [x] `DashboardResponse` — totalClients, by-status counts, totalMealPlans, clientsNeedingPlan, recentClients (top 5)
- [x] `DashboardService` — aggregates from ClientRepository + MealPlanRepository
- [x] `CoachProfileIntegrationTest` — 6 tests (get, update, GSTIN validation, empty body)
- [x] `DashboardIntegrationTest` — 5 tests (zeros, counts, needs-plan logic, recent 5, 401)
- [ ] Frontend dashboard with 3-column Everfit layout

### Day 14 🔲 Buffer / Polish Week 2
- [ ] Fix any bugs from Days 8–13
- [ ] API documentation review
- [ ] Week 2 review & plan Week 3

---

## Week 3 — Monetization + Engagement (Days 15–21)

### Day 15 ✅ Progress Logging
- [x] `ProgressController` — POST/GET `/api/v1/clients/{id}/progress`, GET `/chart?days=`
- [x] `CheckInController` — POST/GET `/api/v1/clients/{id}/check-ins`
- [x] `ProgressService` — upsert measurements (one per client per day), chart query
- [x] `CheckInService` — create (409 on duplicate date), history descending
- [x] `ProgressLogRepository`, `CheckInRepository` — all queries
- [x] `LogProgressRequest`, `ProgressLogResponse`, `CreateCheckInRequest`, `CheckInResponse` (records)
- [x] `ProgressMapper` (MapStruct) — ProgressLog + CheckIn → response
- [x] `ProgressIntegrationTest` — 15 tests (progress CRUD, upsert, chart, check-in CRUD, 409 duplicate, 401)

### Day 16 ✅ Photo Upload (S3)
- [x] `S3Service` — presigned PUT (upload) + GET (download), 10-min TTL
- [x] `S3Config` + `AwsProperties` (`@ConfigurationProperties`)
- [x] `ProgressPhotoController` — POST `/api/v1/clients/{id}/progress/{logId}/photos` (presigned URL), GET (list), DELETE
- [x] `ProgressPhotoRepository`
- [x] Local dev mode: returns dummy URLs without hitting AWS
- [x] AWS SDK v2 (`s3`, `sts`) added to pom.xml
- [x] `ProgressPhotoIntegrationTest`
- [x] Frontend: photo upload component with FRONT/SIDE/BACK grid (on `nutricoach-web`)
- [ ] IAM policy for ap-south-1 bucket (deferred until first deploy)

### Day 17 ✅ Razorpay Subscriptions
- [x] `RazorpayProperties` (@ConfigurationProperties) + `RazorpayConfig` (@Bean)
- [x] `RazorpayService` — create customer, create subscription, cancel, webhook verification (dev-mode bypass)
- [x] `SubscriptionRepository`, `InvoiceRepository`
- [x] `BillingController` — POST `/api/v1/billing/subscribe`, GET `/api/v1/billing/status`, DELETE `/api/v1/billing/cancel`
- [x] `WebhookController` — POST `/api/v1/billing/webhook` (public, HMAC-SHA256 verified)
- [x] Webhook events: `subscription.activated`, `subscription.charged`, `subscription.cancelled`, `subscription.halted`
- [x] GST invoice generation (18%) with invoice number `NC-YYYY-NNNN`
- [x] Idempotent payment processing (duplicate webhook safe)
- [x] `BillingIntegrationTest` — 12 tests

### Day 18 ✅ Feature Gating
- [x] `SubscriptionGate` service — enforces per-tier client limits
  - TRIAL: 5 clients, STARTER: 25, PROFESSIONAL: 100, ENTERPRISE: unlimited
- [x] `ClientService.create()` calls `requireClientSlot()` — returns HTTP 402 when over limit
- [x] `NutriCoachException.paymentRequired()` factory method (HTTP 402)
- [ ] Frontend: pricing page, upgrade prompt, subscription management page

### Day 19 ✅ WhatsApp Integration (WATI)
- [x] `WatiService` — send template + text messages via WATI API
- [x] `NotificationService` — unified SMS + WhatsApp abstraction over MSG91 + WATI
- [x] `MealPlanShareController` — POST `/api/v1/meal-plans/{id}/share` (WhatsApp deep link to client)
- [x] `NotificationLogRepository` — persists every send (channel, recipient, status, providerRef)
- [x] Local dev mode: logs to console + returns mock provider ID
- [x] `MealPlanShareIntegrationTest` (full share flow + 401/403/404 paths)
- [ ] Frontend: WhatsApp share button on meal plan page (next task)

### Day 20 ✅ Client Check-in Reminders
- [x] `CheckInReminderScheduler` — `@Scheduled(cron = "0 30 2 * * *", zone = "UTC")` runs daily 8 AM IST
- [x] Sends WhatsApp reminder to ACTIVE clients with no check-in in last 7 days
- [x] De-dup: skip if a reminder was already sent within 24h
- [x] All sends logged via `NotificationService` → `notification_logs`
- [ ] Coach alert: "3 clients haven't checked in this week" (Day 27 dashboard polish)
- [ ] Low adherence detection (< 70% → flag in dashboard) (Day 27 dashboard polish)

### Day 21 🔲 Buffer / Polish Week 3
- [ ] End-to-end billing flow test
- [ ] Security audit: tenant isolation checks
- [ ] Performance: add missing DB indexes
- [ ] Week 3 review & plan Week 4

---

## Week 4 — AI + Launch (Days 22–30)

### Day 22 ✅ AI — Meal Plan Generation (Backend)
> **Architectural divergence**: implemented with raw OpenAI `RestClient` instead of `spring-ai-openai-spring-boot-starter`. Reason: avoids one more dependency at MVP stage; the async job pattern isolates the LLM call so swapping to Spring AI later is a one-class change.
- [x] `OpenAiProperties` (`@ConfigurationProperties`) — endpoint, key, model
- [x] `AiMealPlanService` — `@Async` GPT-4o job submission via `RestClient`
- [x] Prompt engineering: client profile → 7-day Indian meal plan JSON
- [x] `AiJobController` — POST `/api/v1/ai/meal-plans/generate`, GET `/api/v1/ai/jobs/{id}`
- [x] Poll-based status check (PENDING → PROCESSING → COMPLETED/FAILED)
- [x] Parses GPT-4o JSON response into `MealPlan` + day/meal/item entities
- [x] Local dev mode: returns hardcoded 7-day Indian plan without calling OpenAI
- [x] `AiJobRepository`, `AiJob` entity (status + result JSONB)
- [x] `AiJobIntegrationTest` (Awaitility async polling)

### Day 23 🟡 AI Meal Plan Generation (Frontend)
- [x] "Generate with AI" modal scaffolded in meal plan builder
- [ ] Wire modal to backend `POST /api/v1/ai/meal-plans/generate` (currently shows toast stub)
- [ ] Client profile summary sent to AI (goals, dietary prefs, allergies)
- [ ] Loading state with job polling on `GET /api/v1/ai/jobs/{id}`
- [ ] Generated plan preview + edit before saving

### Day 24 ✅ Landing Page (on `nutricoach-web`)
- [x] Next.js 16 landing page at `/`
- [x] Hero section with primary CTA
- [x] 6 feature cards
- [x] 3-tier pricing comparison
- [x] Sticky navbar with "Start Free Trial" → phone OTP → coach dashboard
- [ ] Mobile-responsive

### Day 25 🔲 Branding + Polish
- [ ] Logo + color palette (finalize)
- [ ] Consistent typography (Inter font)
- [ ] Loading skeletons + empty states
- [ ] Error boundaries + friendly error messages
- [ ] Toast notifications (success/error)

### Day 26 🟡 Security Hardening (rate limiting done, rest pending)
- [x] `RateLimiterService` (bucket4j) — 5 OTP sends/hour/phone, 10 verify attempts/hour/phone
- [x] Wired into `AuthService`; throws `NutriCoachException.tooManyRequests()` (HTTP 429)
- [x] `RateLimiterServiceTest` (unit, in-memory bucket state)
- [x] `bucket4j-core` 8.10.1 in pom.xml
- [ ] Input sanitization audit
- [ ] SQL injection prevention review (JPA params already safe; spot-check JPQL)
- [ ] JWT expiry + refresh token flow
- [ ] HTTPS enforcement (Railway level)
- [ ] Security headers (CORS, CSP, HSTS)

### Day 27 🔲 Performance + Monitoring
- [ ] Slow query detection (log queries > 100ms)
- [ ] Database connection pooling (HikariCP config)
- [ ] Spring Boot Actuator metrics
- [ ] Sentry or Axiom for error tracking
- [ ] Uptime monitoring

### Day 28 🔲 Coach Acquisition Prep
- [ ] Demo data seed (1 coach, 3 clients, sample meal plans)
- [ ] Onboarding email/WhatsApp sequence draft
- [ ] Coach referral tracking (UTM params)
- [ ] Analytics: coach signup funnel events

### Day 29 🔲 Beta Testing
- [ ] Deploy to prod (Railway + Vercel)
- [ ] Test full flow: signup → add client → create plan → billing
- [ ] Fix critical bugs
- [ ] Share with 2-3 nutritionist friends for feedback

### Day 30 🔲 Launch
- [ ] Announce in nutritionist WhatsApp groups
- [ ] Post in relevant Indian startup/nutrition communities
- [ ] First 3 paying coaches target
- [ ] Post-launch fixes backlog

---

## Bonus — Client Portal (not in original 30-day plan)

> Branch: `feat/client-portal-auth` (pushed, awaiting PR — not yet in `master`). Adds a separate client-facing surface so clients can log in and view their own meal plans / progress without going through the coach.

### Phase 1 ✅ Client-role auth
- [x] `ClientAuthService` + `ClientAuthController` — phone OTP login for clients
- [x] `JwtService.generateClientToken()` — claims: `sub` (phone), `clientId`, `coachId`, `role=CLIENT`
- [x] `ClientUserDetailsService` — looks up clients by phone
- [x] Dual-role JWT routing in `JwtAuthenticationFilter` (COACH vs CLIENT)
- [x] `SecurityUtils.getCurrentClientId()` + `getCurrentCoachIdFromToken()`
- [x] DB index on `clients(phone, deleted_at)` for auth performance
- [x] `ClientAuthIntegrationTest` (9 tests)

### Phase 2 ✅ Read-only portal endpoints (`ROLE_CLIENT` scoped)
- [x] `ClientProfileController` — GET `/api/v1/portal/profile`
- [x] `ClientMealPlanController` — GET `/api/v1/portal/meal-plans`, GET `/api/v1/portal/meal-plans/{id}`
- [x] `ClientProgressController` — GET `/api/v1/portal/progress`, GET `/api/v1/portal/progress/chart`
- [x] `ClientCheckInController` — GET `/api/v1/portal/check-ins`
- [x] All controllers verify `clientId` + `coachId` ownership: 403 cross-tenant, 404 cross-client
- [x] `ClientPortalIntegrationTest`

---

## Completed Features (Running Log)

> **Mapping convention:** Use MapStruct `@Mapper(componentModel = "spring")` for all DTO↔entity conversions. Place in `module/mapper/` package. For patch ops: `@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)`.

| Feature | Day | Notes |
|---------|-----|-------|
| Spring Boot scaffold (8 modules) | 1 | Modular monolith, Java 21 |
| OTP auth (send + verify) | 1 | MSG91, dev-mode logging |
| JWT issuance | 1 | coachId + role claims, 72h expiry |
| Spring Security (stateless) | 1 | JwtAuthenticationFilter, 401 on missing token |
| ApiResponse wrapper | 1 | |
| GlobalExceptionHandler | 1 | |
| Liquibase schema (11 changesets) | 2 | All tables, multi-tenant |
| All JPA entities (14 total) | 2 | |
| Client CRUD + soft delete | 8 | Multi-tenant, status filter, 409 on duplicate phone |
| Indian food DB seed (100 items) | 9 | IFCT data, 8 cuisines, MapStruct mapper |
| Meal plan builder API | 10 | 15 endpoints, nutrition auto-calc, no N+1 |
| Coach profile API | 13 | GET/PUT /api/v1/coach/me, GSTIN validation |
| Coach dashboard API | 13 | Counts by status, needs-plan detection, recent 5 clients |
| Progress logging + chart API | 15 | Upsert semantics, one log per client per day |
| Check-in API | 15 | 409 on duplicate date, meal plan ownership verified |
| Food item search API tests | 9 | FoodItemIntegrationTest (8 tests): search all/query/cuisine/category, getById, 404, 401 |
| Meal plan builder API tests | 10 | MealPlanIntegrationTest (15 tests): full plan lifecycle, day/meal/item CRUD, nutrition calc, 401 |
| Razorpay subscriptions | 17 | subscribe, cancel, webhook (activated/charged/cancelled/halted), GST invoices, idempotency |
| Feature gating (client limits) | 18 | SubscriptionGate: TRIAL=5, STARTER=25, PROFESSIONAL=100, ENTERPRISE=∞ |
| S3 photo upload (presigned URLs) | 16 | S3Service, ProgressPhotoController, FRONT/SIDE/BACK photo types, dev-mode dummy URLs |
| WATI WhatsApp integration | 19 | WatiService, NotificationService, MealPlanShareController, NotificationLog persistence |
| Check-in reminder scheduler | 20 | CheckInReminderScheduler, daily 8 AM IST, 7-day inactivity trigger, 24h de-dup |
| AI meal plan generation | 22 | Async @Async pattern, raw OpenAI RestClient (NOT Spring AI), AiJob polling, dev-mode hardcoded plan |
| Rate limiting (bucket4j) | 26 | RateLimiterService: 5 OTP/hr, 10 verify/hr per phone, HTTP 429 |
| Demo data seed | bonus | 013-demo-data.xml — local-profile demo coach + clients |
| Client Portal auth (Phase 1) | bonus | Separate ROLE_CLIENT JWT, ClientAuthService, dual-role JwtAuthenticationFilter |
| Client Portal read endpoints (Phase 2) | bonus | /api/v1/portal/* — profile, meal-plans, progress, check-ins (read-only) |
| Integration tests (~14 files) | — | Auth, ClientAuth, Coach, Dashboard, Progress, FoodItem, MealPlan, Billing, ProgressPhoto, MealPlanShare, AiJob, ClientPortal + base |

---

## Architecture Decisions Log

| Decision | Reason | Alternative Rejected |
|----------|--------|---------------------|
| Liquibase XML only | Explicit rollbacks, audit trail | Flyway (no XML-first support) |
| coach_id on every table | Multi-tenancy without schema-per-tenant | Schema isolation (complex) |
| Phone-first auth | Indian market, no email barrier | Email+password |
| Razorpay not Stripe | UPI, INR settlement, GST invoicing | Stripe (no UPI) |
| Async AI jobs (AiJob table) | GPT-4o latency ~5-10s | Sync HTTP call |
| JSONB for health data | Flexible, no schema migration for new fields | Relational columns |
| ap-south-1 S3 | DPDP Act 2023 data residency | eu-west or us-east |

---

## Environment Setup

```bash
# Required env vars (see .env.example)
DATABASE_URL=jdbc:postgresql://...supabase.co:5432/postgres
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=...
JWT_SECRET=...  # openssl rand -base64 32
MSG91_AUTH_KEY=...
MSG91_TEMPLATE_ID=...
MSG91_DEV_MODE=true  # set false in prod
WATI_API_ENDPOINT=https://live-mt-server.wati.io/...
WATI_API_TOKEN=...
RAZORPAY_KEY_ID=rzp_test_...
RAZORPAY_KEY_SECRET=...
RAZORPAY_WEBHOOK_SECRET=...
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
S3_BUCKET_NAME=nutricoach-progress-photos
OPENAI_API_KEY=sk-...
```

```bash
# Run locally
./mvnw spring-boot:run
# App: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui/index.html
```

---

## Blocked / Decisions Needed

> Add items here when blocked waiting for a decision or external setup.

- [ ] Supabase project created? (need DATABASE_URL for first deploy)
- [ ] Railway project created?
- [ ] Vercel project created?
- [ ] MSG91 account + template approved?
- [ ] Razorpay test credentials obtained?
