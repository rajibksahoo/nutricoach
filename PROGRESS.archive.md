# NutriCoach — 30-Day MVP Progress Tracker (ARCHIVED)

> Archived 2026-05-08 when the active plan was reset to "match Claude Design output". See current `PROGRESS.md`. This file is kept for historical context — do not update.

---

## Quick Status (frozen at archive time)

| Week | Theme | Status |
|------|-------|--------|
| Week 1 (Days 1–7) | Scaffold + Auth + Schema + Deploy | ✅ Complete (frontend scaffold deferred, now done on web repo) |
| Week 2 (Days 8–14) | Client Mgmt + Meal Plans + Dashboard | ✅ Backend complete (frontend on `nutricoach-web`) |
| Week 3 (Days 15–21) | Progress + Billing + WhatsApp | ✅ Backend complete |
| Week 4 (Days 22–30) | AI + Branding + Launch | 🟡 Days 22, 24, 26 done; 23 partial; 25/27–30 pending |

**Bonus delivered**: Client Portal — separate client-role JWT auth (Phase 1) + read-only portal endpoints under `/api/v1/portal/*` (Phase 2). On branch `feat/client-portal-auth`, awaiting PR.

---

## Week 1 — Foundation (Days 1–7)

### Day 1 ✅ Spring Boot Scaffold + Auth
8-module modular monolith, JWT auth, OTP via MSG91 (dev-mode logging), `BaseEntity`, `ApiResponse`, `GlobalExceptionHandler`, `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, `AuthController` + `AuthService`, `OpenApiConfig`, `JpaConfig`, `NutriCoachApplication` (@EnableAsync).

### Day 2 ✅ Liquibase Schema (11 changesets)
coaches, clients, meal_plans, meal_plan_days/meals/items, food_items, progress, check_ins, billing (subscriptions + invoices + GST), otp_requests, ai_jobs, notification_logs. All 14 JPA entities created.

### Days 3–7 🔲 Frontend scaffold + CI/CD + Deploy (deferred — backend first)

---

## Week 2 — Core Product (Days 8–14)

### Day 8 ✅ Client Management
`SecurityUtils`, `ClientRepository`, `CreateClientRequest`, `UpdateClientRequest`, `ClientResponse`, `ClientService`, `ClientController` (GET/POST `/api/v1/clients`, GET/PUT/DELETE by id, `?status=` filter).

### Day 9 ✅ Indian Food Database Seed
100 IFCT items in `012-seed-food-items.xml`, `FoodItemRepository`, `FoodItemMapper` (MapStruct), `FoodItemService`, `FoodItemController` with search.

### Day 10 ✅ Meal Plan Builder API
4-level repos + DTOs, `MealPlanMapper`, `MealPlanService` with auto nutrition recalc + batch food name lookup, `MealPlanController` with 15 endpoints, full tenant isolation.

### Day 11 🔲 Meal Plan Builder Frontend (pending)

### Day 12 🔲 Client Portal read-only (delivered as bonus, see below)

### Day 13 ✅ Coach Profile + Dashboard API
`UpdateCoachRequest` w/ GSTIN regex, `CoachResponse`, `CoachMapper`, `CoachService`, `CoachController`, `DashboardResponse`, `DashboardService`. Tests: `CoachProfileIntegrationTest` (6), `DashboardIntegrationTest` (5).

### Day 14 🔲 Buffer / polish

---

## Week 3 — Monetization + Engagement (Days 15–21)

### Day 15 ✅ Progress Logging
`ProgressController` + `CheckInController`, upsert one log per client per day, chart query, 409 on duplicate check-in date. 15-test integration suite.

### Day 16 ✅ Photo Upload (S3)
`S3Service` (presigned PUT/GET, 10-min TTL), `S3Config`, `AwsProperties`, `ProgressPhotoController`, `ProgressPhotoRepository`, FRONT/SIDE/BACK photo grid on web. Dev-mode dummy URLs.

### Day 17 ✅ Razorpay Subscriptions
`RazorpayService` (subscribe / cancel / webhook), `BillingController`, `WebhookController` (HMAC-verified), GST invoice numbering, idempotent processing. 12-test suite.

### Day 18 ✅ Feature Gating
`SubscriptionGate` enforcing TRIAL=5 / STARTER=25 / PROFESSIONAL=100 / ENTERPRISE=∞ via HTTP 402.

### Day 19 ✅ WhatsApp (WATI)
`WatiService`, `NotificationService` (SMS+WhatsApp abstraction), `MealPlanShareController`, `NotificationLogRepository`. Dev-mode console output.

### Day 20 ✅ Check-in Reminders
`CheckInReminderScheduler` (`@Scheduled`, daily 8 AM IST, 7-day inactivity, 24h de-dup).

### Day 21 🔲 Buffer

---

## Week 4 — AI + Launch (Days 22–30)

### Day 22 ✅ AI Meal Plan Generation (Backend)
Raw OpenAI `RestClient` (no Spring AI starter), `AiMealPlanService` `@Async` GPT-4o, `AiJobController` poll-based status, dev-mode hardcoded plan, `AiJob` entity + repo, integration test with Awaitility.

### Day 23 🟡 AI Frontend (modal scaffolded, wiring incomplete)

### Day 24 ✅ Landing Page (on `nutricoach-web`)
Hero + 6 features + 3-tier pricing + sticky navbar + Start Free Trial → OTP → dashboard.

### Day 25 🔲 Branding + Polish

### Day 26 🟡 Security Hardening (rate limiting done)
`RateLimiterService` (bucket4j) — 5 OTP/hr, 10 verify/hr per phone, HTTP 429.

### Days 27–30 🔲 Performance/monitoring, acquisition prep, beta, launch

---

## Bonus — Client Portal

> Branch `feat/client-portal-auth` (pushed, awaiting PR).

### Phase 1 ✅ Client-role auth
`ClientAuthService`, `ClientAuthController`, `JwtService.generateClientToken()` with `clientId`/`coachId`/`role=CLIENT`, `ClientUserDetailsService`, dual-role routing in `JwtAuthenticationFilter`, `SecurityUtils.getCurrentClientId()`, DB index on `(phone, deleted_at)`. 9-test suite.

### Phase 2 ✅ Read-only portal endpoints (`ROLE_CLIENT`)
`/api/v1/portal/profile`, `/meal-plans`, `/meal-plans/{id}`, `/progress`, `/progress/chart`, `/check-ins`. 403 cross-tenant, 404 cross-client. Integration test.

---

## Architecture Decisions Log

| Decision | Reason |
|----------|--------|
| Liquibase XML only | Explicit rollbacks, audit trail |
| coach_id on every table | Multi-tenancy without schema-per-tenant |
| Phone-first auth | Indian market, no email barrier |
| Razorpay not Stripe | UPI, INR settlement, GST invoicing |
| Async AI jobs (AiJob table) | GPT-4o latency ~5–10s |
| JSONB for health data | Flexible, no schema migration for new fields |
| ap-south-1 S3 | DPDP Act 2023 data residency |