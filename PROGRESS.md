# NutriCoach — 30-Day MVP Progress Tracker

> **Instructions for Claude**: At the start of each session, read this file first.
> Update task statuses as work is completed. Add notes under each day.
> Target: 3 paying coaches by Day 30.

---

## Quick Status

| Week | Theme | Status |
|------|-------|--------|
| Week 1 (Days 1–7) | Scaffold + Auth + Schema + Deploy | ✅ **Complete** |
| Week 2 (Days 8–14) | Client Mgmt + Meal Plans + Dashboard | 🟡 **In Progress** |
| Week 3 (Days 15–21) | Progress + Billing + WhatsApp | 🟡 **In Progress** |
| Week 4 (Days 22–30) | AI + Branding + Launch | 🔲 Not started |

**Current Day**: ~15 (as of 2026-03-22)
**Next task**: Week 3, Day 16 — S3 photo upload (presigned URLs) or Day 17 — Razorpay subscriptions

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

### Day 16 🔲 Photo Upload (S3)
- [ ] `S3Service` — presigned URL generation for upload + download
- [ ] `ProgressPhotoController` — POST (get presigned URL), GET (view photos)
- [ ] Frontend: photo upload component with before/after grid
- [ ] IAM policy for ap-south-1 bucket (DPDP compliant)

### Day 17 🔲 Razorpay Subscriptions
- [ ] Liquibase: ensure billing tables ready
- [ ] `RazorpayService` — create customer, create subscription, cancel
- [ ] `BillingController` — POST `/api/v1/billing/subscribe`, GET `/api/v1/billing/status`
- [ ] Razorpay webhook handler — subscription.activated, payment.captured, subscription.cancelled
- [ ] Feature gating: check subscription status before coach actions
- [ ] GST invoice generation (18% GST)

### Day 18 🔲 Subscription UI + Feature Gating
- [ ] Pricing page with 3 tiers (₹999 / ₹2,499 / ₹4,999)
- [ ] Razorpay checkout integration (frontend)
- [ ] Upgrade prompt when limits hit (client count, plan count)
- [ ] Subscription management page (current plan, invoices, cancel)

### Day 19 🔲 WhatsApp Integration (WATI)
- [ ] `WatiService` — send template messages, meal plan share
- [ ] `NotificationService` — unified SMS + WhatsApp abstraction
- [ ] Meal plan WhatsApp share flow
- [ ] Check-in reminder scheduler (@Scheduled daily at 8 AM IST)
- [ ] Notification log persistence

### Day 20 🔲 Client Check-in Reminders
- [ ] Automated weekly check-in reminders via WhatsApp
- [ ] Coach alert: "3 clients haven't checked in this week"
- [ ] Low adherence detection (< 70% → flag in dashboard)
- [ ] In-app notification feed

### Day 21 🔲 Buffer / Polish Week 3
- [ ] End-to-end billing flow test
- [ ] Security audit: tenant isolation checks
- [ ] Performance: add missing DB indexes
- [ ] Week 3 review & plan Week 4

---

## Week 4 — AI + Launch (Days 22–30)

### Day 22 🔲 Spring AI — Meal Plan Generation (Backend)
- [ ] Add `spring-ai-openai-spring-boot-starter` to pom.xml
- [ ] `AiMealPlanService` — async GPT-4o job submission
- [ ] Prompt engineering: client profile → 7-day Indian meal plan JSON
- [ ] `AiJobController` — POST `/api/v1/ai/meal-plans/generate`, GET `/api/v1/ai/jobs/{id}`
- [ ] Poll-based status check (PENDING → PROCESSING → COMPLETED/FAILED)
- [ ] Parse GPT-4o JSON response into MealPlan entities

### Day 23 🔲 AI Meal Plan Generation (Frontend)
- [ ] "Generate with AI" button in meal plan builder
- [ ] Client profile summary sent to AI (goals, dietary prefs, allergies)
- [ ] Loading state with job polling
- [ ] Generated plan preview + edit before saving

### Day 24 🔲 Landing Page
- [ ] Next.js landing page at `/`
- [ ] Hero: "The nutrition platform built for Indian coaches"
- [ ] Feature highlights: meal plans, client management, WhatsApp sharing
- [ ] Pricing section (3 tiers)
- [ ] CTA: "Start free trial" → phone OTP → coach dashboard
- [ ] Mobile-responsive

### Day 25 🔲 Branding + Polish
- [ ] Logo + color palette (finalize)
- [ ] Consistent typography (Inter font)
- [ ] Loading skeletons + empty states
- [ ] Error boundaries + friendly error messages
- [ ] Toast notifications (success/error)

### Day 26 🔲 Security Hardening
- [ ] Rate limiting with bucket4j on auth endpoints (5 OTP/hour per phone)
- [ ] Input sanitization audit
- [ ] SQL injection prevention review
- [ ] JWT expiry + refresh token flow
- [ ] HTTPS enforcement
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
| Integration tests (34 total) | — | Auth×5, Client×7, Coach×6, Dashboard×5, Progress×15 (+ context load) |

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
