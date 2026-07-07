---
name: shipping-backend-changes
description: Use when writing or modifying any nutricoach backend code — controllers, services, entities, migrations, or tests — before claiming the change is complete.
---

# Shipping Production-Ready Backend Changes

## Overview
CLAUDE.md defines the conventions; this skill defines the bar a change must clear before it is "done". A change that compiles but skips a rollback block, a tenant filter, or a test update is not done — it is a production incident scheduled for later.

**REQUIRED BACKGROUND:** Read `CLAUDE.md` (Code Patterns, Multi-Tenancy, Testing sections) before writing code. It wins on any conflict.

## Definition of done — all six, no exceptions
1. **Schema changes ship as Liquibase XML** (`db/changelog/changes/NNN-*.xml` + master include) with a `<rollback>` block. No Flyway, no raw SQL files, no `ddl-auto` reliance.
2. **Tenant safety proven, not assumed** — every new repository query filters by `coachId`; every service method validates ownership via `SecurityUtils.getCurrentCoachId()`. Grep your diff for queries missing the filter before committing.
3. **The full failure matrix is tested** — integration test (extends `AbstractIntegrationTest`, real Postgres `nutricoach_test_it`, never H2/mocked DB) covering 200/201, 400 validation, 401 no token, 403 wrong tenant, 404, and 409 where applicable.
4. **Constructor/bean changes propagate** — after touching any service constructor, `@Bean`, or entity, update every test that mocks or instantiates it in the same commit.
5. **`mvn test` green against `pg-test`** — run it; do not infer from compilation. `docker start pg-test` first.
6. **API contract stays consistent** — `ResponseEntity<ApiResponse<T>>`, 201 for POST, `NutriCoachException.notFound()/badRequest()/conflict()` for errors. Both the web app and the Android app parse this envelope; an inconsistent response breaks two clients at once.

## Traps
| Trap | Reality |
|---|---|
| Hard delete | Soft delete only (`deletedAt`). Data loss is unrecoverable in prod. |
| Manual `from()` mapper or Lombok DTO | DTOs are records mapped by MapStruct only. |
| Tenant id from request param | Always `SecurityUtils.getCurrentCoachId()` — a param is an IDOR. |
| `@Transactional` missing on write path | Partial writes on failure. Reads get `readOnly = true`. |
| `@BeforeEach` deleting parents first | FK violations — delete children before parents. |
| Testing against `nutricoach_test` | Tests use `nutricoach_test_it`; the other DB is the dev app's. |

## Red flags — stop and fix before proceeding
- "The migration is additive, rollback is unnecessary"
- "This query is internal, tenant filtering doesn't apply"
- "Tests pass locally in my head" (you didn't run `mvn test`)
- "I'll update the mocks in a follow-up"
