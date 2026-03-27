# NutriCoach

B2B SaaS for nutritionists and fitness coaches in India. Coaches manage clients, create meal plans, track progress, and handle billing — all in one platform.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker (for the local PostgreSQL container)

## Running Locally

```bash
# 1. Start the local database (first time)
docker run -d --name pg-test -p 5433:5432 \
  -e POSTGRES_DB=nutricoach_test \
  -e POSTGRES_USER=nutricoach \
  -e POSTGRES_PASSWORD=nutricoach \
  postgres:16-alpine

# Subsequent runs — just start the existing container
docker start pg-test

# 2. Start the backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The API is available at `http://localhost:8080`.

## Testing the API

### Swagger UI

Open `http://localhost:8080/swagger-ui.html` to browse and try all endpoints interactively.

**To authenticate in Swagger:**
1. Call `POST /api/v1/auth/demo-login` (see below)
2. Copy the `token` value from the response
3. Click **Authorize** (lock icon, top right) and enter `Bearer <token>`

All secured endpoints will now work for the rest of the session.

### Demo Login (no OTP required)

The local profile ships with a demo login endpoint that skips OTP entirely — useful since MSG91 SMS is not required for local testing.

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/demo-login \
  -H "Content-Type: application/json" \
  -d '{"phone":"9999999999","name":"Demo Coach"}' | jq .
```

- Use any valid 10-digit Indian mobile number (`6–9` prefix) as the phone
- Use different numbers to create multiple isolated demo accounts
- Calling the same number again logs into the existing account
- **This endpoint returns `403` in production** — it only works when `app.msg91.dev-mode=true`

### Real OTP Login (once MSG91 is set up)

```bash
# 1. Request OTP (prints to console in local profile — no SMS sent)
curl -s -X POST http://localhost:8080/api/v1/auth/otp/send \
  -H "Content-Type: application/json" \
  -d '{"phone":"9999999999"}'

# 2. Verify — use 111111 as the universal bypass OTP in local profile
curl -s -X POST http://localhost:8080/api/v1/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{"phone":"9999999999","otp":"111111","name":"Demo Coach"}' | jq .
```

### Authenticated request example

```bash
TOKEN="<paste token here>"

curl -s http://localhost:8080/api/v1/coach/profile \
  -H "Authorization: Bearer $TOKEN" | jq .
```

## Running Tests

```bash
# Start the database first (see above), then:
TEST_DB_URL=jdbc:postgresql://localhost:5433/nutricoach_test mvn test

# Single test class
TEST_DB_URL=jdbc:postgresql://localhost:5433/nutricoach_test mvn test -Dtest=AuthIntegrationTest
```

Tests use a real PostgreSQL instance — no H2, no mocks for the database layer.
