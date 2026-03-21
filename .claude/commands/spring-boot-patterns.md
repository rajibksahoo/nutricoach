---
name: spring-boot-patterns
description: Review any new feature or module against NutriCoach's production-quality Spring Boot patterns and checklists. Invoke before writing new code to align on conventions, or after writing to audit compliance.
user-invocable: true
argument-hint: [feature-or-module-name]
allowed-tools: Read, Grep, Glob
---

Review the task "$ARGUMENTS" (or the current context if no argument given) against the NutriCoach production patterns below.

For each section relevant to the task, go through the checklist and flag any deviations or missing pieces before writing code. If auditing existing code, read the relevant files first then report compliance status.

---

## ENTITY CHECKLIST
- [ ] Extends `BaseEntity` (UUID PK, `createdAt`, `updatedAt`, JPA auditing)
- [ ] Has `coach_id UUID NOT NULL` as tenant discriminator (every user-scoped table)
- [ ] Soft deletes via `deletedAt Instant` column (never hard delete)
- [ ] Enums as nested static inner classes (`Client.Status`, `MealPlan.Status`)
- [ ] **Lombok** `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` — JPA needs mutable classes, do NOT use records for entities
- [ ] Liquibase XML changeset in `changes/NNN-description.xml` with `<rollback>` block
- [ ] DB indexes on: `coach_id`, `status`, `deleted_at`, all foreign keys

## DTO CHECKLIST
- [ ] Use **Java records** (immutable) — no Lombok needed, no plain classes with setters
- [ ] Input: `CreateXxxRequest`, `UpdateXxxRequest` with Jakarta `@Valid` annotations
- [ ] Output: `XxxResponse` record — mapped via **MapStruct** (`@Mapper(componentModel = "spring")`)
- [ ] **Never use manual `from()` factory methods** — always MapStruct (project rule)
- [ ] Validation: `@NotBlank`, `@Pattern`, `@Size`, `@Min`/`@Max`
- [ ] Phone numbers: `@Pattern(regexp = "^[6-9]\\d{9}$")` for Indian mobile

## REPOSITORY CHECKLIST
- [ ] Extends `JpaRepository<Entity, UUID>`
- [ ] All queries include `DeletedAtIsNull` filter
- [ ] All multi-tenant queries filter by `coachId`
- [ ] List endpoints return `Page<T>` not `List<T>` (pagination)
- [ ] `@Query` JPQL for complex filters; Spring Data naming for simple ones

## SERVICE CHECKLIST
- [ ] `@Transactional` for writes, `@Transactional(readOnly = true)` for reads
- [ ] `@Transactional(noRollbackFor = NutriCoachException.class)` when state must persist on business error
- [ ] Tenant guard: `requireXxxOwned(coachId, entity)` before any write
- [ ] Fetch coachId via `SecurityUtils.getCurrentCoachId()` — never trust client input
- [ ] Throw `NutriCoachException.notFound()` / `.badRequest()` / `.conflict()` — never return null
- [ ] Under 150 lines — extract sub-services if larger

## CONTROLLER CHECKLIST
- [ ] `@PreAuthorize("hasRole('COACH')")` at **class** level
- [ ] `@Operation`, `@Tag`, `@SecurityRequirement(name = "bearerAuth")` on class
- [ ] Returns `ResponseEntity<ApiResponse<T>>` always
- [ ] HTTP status: `201 CREATED` for POST, `200 OK` for GET/PUT/PATCH/DELETE
- [ ] `@Valid` on all `@RequestBody` params
- [ ] coachId from `SecurityUtils.getCurrentCoachId()` only — never path/query param

## SECURITY CHECKLIST
- [ ] New public endpoints added to `PUBLIC_PATHS` in `SecurityConfig`
- [ ] Never log sensitive data (OTP, tokens, phone numbers)
- [ ] Tenant violations → `NutriCoachException.forbidden()` with `ACCESS_DENIED`

## TESTING CHECKLIST
- [ ] Integration test class extends `AbstractIntegrationTest`
- [ ] Run with: `TEST_DB_URL=jdbc:postgresql://localhost:5433/nutricoach_test mvn test`
- [ ] `@BeforeEach` cleans up created data (child entities before parents due to FK)
- [ ] Covers: 200/201 happy path, 400 validation, 401 no token, 403 wrong tenant, 404 not found, 409 duplicate

## 10 PRODUCTION QUALITY GATES
Before marking any feature complete:
```
✓ MapStruct mapper (no manual from() methods)
✓ Liquibase migration with rollback block
✓ DB indexes for all FK columns
✓ Integration test covering all HTTP status cases
✓ No cross-tenant data leakage possible
✓ Soft delete respected in all queries
✓ Pagination (Page<T>) on list endpoints
✓ @Transactional boundaries correct
✓ No System.out or e.printStackTrace()
✓ No sensitive data in logs
```

## KNOWN GAPS (fix in priority order)
1. **CORS** — `WebMvcConfigurer` for Next.js (`localhost:3000` + Vercel domain)
2. **Rate limiting** — bucket4j on `/api/v1/auth/**`
3. **Pagination** — `Pageable` + `Page<T>` on all list endpoints
4. **SecurityUtils** — move coachId into JWT claims to avoid DB call per request
5. **MealPlanService** — extract `MealService` + `MealItemService` (300+ lines)
6. **i18n** — move error messages to `messages.properties`
7. **Billing module** — Subscription + Invoice entities exist, need service + controller + Razorpay
8. **Progress module** — ProgressLog + Photo entities exist, need S3 upload + service
9. **AI module** — AiJob entity exists, need async Spring AI + GPT-4o processor
10. **Notifications** — NotificationLog entity exists, need WATI WhatsApp + MSG91 integration

## MODULE STATUS
| Module       | Entity | Repo | Service | Controller | Tests |
|--------------|--------|------|---------|------------|-------|
| auth         | ✓      | ✓    | ✓       | ✓          | ✓     |
| coach        | ✓      | ✓    | —       | —          | —     |
| client       | ✓      | ✓    | ✓       | ✓          | ✓     |
| plans        | ✓      | ✓    | ✓       | ✓          | —     |
| billing      | ✓      | —    | —       | —          | —     |
| progress     | ✓      | —    | —       | —          | —     |
| ai           | ✓      | —    | —       | —          | —     |
| notifications| ✓      | —    | —       | —          | —     |
