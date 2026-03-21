---
name: java-code-review
description: >
  Java code review checklist for NutriCoach. Use this skill when asked to
  review any Java code, before committing, or when refactoring existing code.
  Trigger on "review this", "check this code", "is this correct", or any
  request to evaluate Java code quality.
---

# Java code review checklist for NutriCoach

Review the code in context (or $ARGUMENTS if specified). Read the relevant files first, then go through each section. Report ✓ pass, ✗ fail, or N/A for each item. Summarise critical issues at the top.

---

## Security checks (review first)
- [ ] Every repository query filters by `coachId` (multi-tenancy)
- [ ] No user-supplied data in JPQL without `@Param` (SQL injection)
- [ ] Razorpay webhook signature verified before processing payload
- [ ] JWT not logged anywhere (no `log.info("token: {}", jwt)`)
- [ ] No sensitive data (phone, OTP, token) in exception messages returned to client
- [ ] Input validated with `@Valid` and Bean Validation annotations on controller params

## Correctness checks
- [ ] `@Transactional` on all write operations
- [ ] `@Transactional(readOnly = true)` on all read operations
- [ ] `@Transactional(noRollbackFor = NutriCoachException.class)` where state must persist on business error
- [ ] `Optional` handled correctly — no `.get()` without `.isPresent()` check; prefer `.orElseThrow()`
- [ ] Async methods annotated with `@Async` return `CompletableFuture<T>`
- [ ] Scheduled tasks use `zone = "Asia/Kolkata"` for Indian time
- [ ] Soft deletes: all queries include `deletedAt IS NULL` filter

## Performance checks
- [ ] No N+1 queries — use batch fetching, `JOIN FETCH`, or `@EntityGraph` for associations
- [ ] List endpoints use `Page<T>` + `Pageable` — never return unbounded `List<T>`
- [ ] No blocking I/O calls inside `@Async` methods
- [ ] New FK columns have Liquibase `<createIndex>` changesets

## Code quality checks
- [ ] Methods under 20 lines — extract helpers if longer
- [ ] No raw types (`List` instead of `List<T>`)
- [ ] DTOs are records — not mutable classes with setters
- [ ] Entities use Lombok — not records (Hibernate needs mutability)
- [ ] No `@Data` on `@Entity` classes — causes `toString()` infinite loop via lazy associations; use `@Getter @Setter` instead
- [ ] All entity fields have explicit `nullable` annotation in Liquibase (`nullable="false"` or `nullable="true"`)
- [ ] No `NutriCoachException` constructed directly — use static factories (`.notFound()`, `.badRequest()`, `.conflict()`)
- [ ] MapStruct used for DTO↔entity mapping — no manual `from()` factory methods

## Testing checks
- [ ] Integration test extends `AbstractIntegrationTest` and run with `TEST_DB_URL`
- [ ] Covers all HTTP status codes: 200/201, 400, 401, 403, 404, 409
- [ ] `@BeforeEach` cleans child entities before parent (FK constraint order)- [ ] Webhook handler tested with both valid and invalid signatures