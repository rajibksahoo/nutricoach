# Go-Live Checklist

Everything that must change before NutriCoach takes a real payment from a real
coach. Written 2026-09-18 against `master` in all three repos.

Every item below was verified against the code, not assumed. Where something is
**unverifiable locally**, it says so rather than pretending it passed.

---

## 1. Blockers — the product is wrong or unsafe without these

### 1.1 Business identity is still placeholder text

`nutricoach-web/lib/legal.ts` holds **10 `REPLACE_ME` values**. Razorpay merchant
onboarding and the DPDP Act 2023 both require these to be real, public and
consistent with your registration.

| Field | What it needs |
|---|---|
| `legalEntityName` | Registered entity, e.g. "Acme Wellness Technologies Pvt Ltd" |
| `registeredAddress` | Full registered address incl. PIN |
| `gstin` | 15-character GSTIN as printed on invoices |
| `cin` | CIN / LLPIN, or blank for a proprietorship |
| `supportEmail` | Monitored support address |
| `grievanceEmail` | Grievance officer's address (DPDP requirement) |
| `grievanceOfficer` | Grievance officer's name |
| `supportPhone` | Reachable Indian number |
| `jurisdictionCity` | City whose courts have jurisdiction |
| `policiesLastUpdated` | The date you publish the policies |

Find them with `rg REPLACE_ME lib/legal.ts`.

**Safety net that already exists:** `LegalShell` calls
`hasUnreplacedPlaceholders()` and renders a visible draft banner on every policy
page while any placeholder remains. So shipping with these unset is loud, not
silent — but it is still shipping a draft.

### 1.2 The policy copy has not been reviewed by a lawyer

`/terms`, `/privacy`, `/refund` are a starting draft written to the shape
Razorpay expects. They are **not legal advice**. Get your own counsel to read
them before you accept money.

### 1.3 `MSG91_DEV_MODE` must be false in production

This is the single most dangerous environment variable in the system. When true
it enables **both**:

- `POST /api/v1/auth/demo-login` — issues a valid coach JWT for any phone number
  with no OTP at all (`AuthService.demoLogin`)
- the universal OTP `111111`, for **both** coach login and the client portal
  (`AuthService`, `ClientAuthService`)

It defaults to `false` in `application.yml` and the endpoint returns 403 when
off, so the default is safe. The risk is someone setting it in a hosting
dashboard to debug and leaving it on. **Verify it is unset in prod after every
deploy.**

### 1.4 Razorpay must be live, not dev

- `RAZORPAY_DEV_MODE` unset/false — when true the backend skips real Razorpay
  calls and returns a dummy checkout URL
- `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` — live keys
- `RAZORPAY_WEBHOOK_SECRET` — the webhook is the **only** source of truth for
  subscription activation; if it is wrong, payments succeed and nobody is
  activated
- `RAZORPAY_PLAN_ID_STARTER` / `_PROFESSIONAL` / `_ENTERPRISE` — three plans must
  actually exist in the Razorpay dashboard at ₹999 / ₹2,499 / ₹4,999
- Register the webhook URL in the Razorpay dashboard and point it at the
  deployed backend

Prices live in `nutricoach-web/lib/plans.ts` and must match the Razorpay plans.
Nothing checks this automatically.

### 1.5 The Android app points at a placeholder domain

`nutricoach-android/app/build.gradle.kts` sets the release `BASE_URL` to
`https://api.nutricoach.example/`. Change it to the real API host before
building a release APK, or the app silently fails against a domain you do not own.

---

## 2. Environment variables

### Backend (`nutricoach`) — every one is required, none have safe defaults

```
DATABASE_URL  DATABASE_USERNAME  DATABASE_PASSWORD
JWT_SECRET                      # base64-encoded 256-bit key
MSG91_AUTH_KEY  MSG91_TEMPLATE_ID
WATI_API_ENDPOINT  WATI_API_TOKEN
RAZORPAY_KEY_ID  RAZORPAY_KEY_SECRET  RAZORPAY_WEBHOOK_SECRET
RAZORPAY_PLAN_ID_STARTER  RAZORPAY_PLAN_ID_PROFESSIONAL  RAZORPAY_PLAN_ID_ENTERPRISE
S3_BUCKET_NAME  AWS_ACCESS_KEY_ID  AWS_SECRET_ACCESS_KEY
CORS_ALLOWED_ORIGINS            # the real frontend origin, not a wildcard
OPENAI_API_KEY
```

Optional but strongly recommended:

```
SENTRY_DSN                      # unset = Sentry off and you fly blind on 500s
SENTRY_ENVIRONMENT              # defaults to "production"
```

Leave `MSG91_DEV_MODE` and `RAZORPAY_DEV_MODE` **unset** — both default to false.

JWT expiry is 72 hours in `application.yml` (local dev uses 720 to avoid
re-login). No change needed.

### Frontend (`nutricoach-web`)

```
NEXT_PUBLIC_API_URL=https://<your-backend>     # required; wrong value 404s every request
NEXT_PUBLIC_RAZORPAY_KEY_ID=rzp_live_...       # without it, Billing refuses to open checkout
```

Optional but strongly recommended:

```
NEXT_PUBLIC_POSTHOG_KEY=phc_...                # product analytics; EU cloud
NEXT_PUBLIC_POSTHOG_HOST=https://eu.i.posthog.com
NEXT_PUBLIC_SENTRY_DSN=https://...             # browser error monitoring
SENTRY_ORG  SENTRY_PROJECT  SENTRY_AUTH_TOKEN  # build-time source map upload
```

Both analytics and browser Sentry require `NODE_ENV=production` **and** their
key, so a dev build sends nothing even if the keys leak into `.env.local`. The
three `SENTRY_*` build vars are optional: without them the build still succeeds,
you just get minified stack traces.

`NEXT_PUBLIC_DEV_MODE` must be **unset**. `IS_DEV_MODE` also requires
`NODE_ENV !== "production"`, so a production build cannot show the "OTP is
111111" banner even if the flag is set — but unset it anyway.

Everything `NEXT_PUBLIC_*` is inlined into the browser bundle. Never put the
Razorpay **secret** or the webhook secret here.

---

## 3. Infrastructure

- **There is no CI/CD.** Neither repo has `.github/workflows`, and there is no
  `Dockerfile`, `Procfile` or `vercel.json`. Deployment is manual today, and
  nothing runs the test suites automatically. The backend and e2e suites only
  run when someone runs them.
- **Observability is configured but unproven.** Sentry (both repos) and PostHog
  (web) are wired and inert until their keys are set. Nothing has ever reported
  a real event, so treat the first deploy as the test: throw one error and
  confirm it lands.
- **S3 bucket** in `ap-south-1` (Mumbai) for DPDP data residency. CORS must allow
  browser `PUT` (program cover uploads) and `GET` (progress photos) from the
  frontend origin.
- **Database** is Supabase Postgres. Liquibase runs on boot; changesets `013`,
  `020` and `023` are seed data marked `context="local"` and — verified in
  `db.changelog-master.xml` — will not run outside local dev.

---

## 4. Never verified outside local dev

These are not known failures. They are things nobody has ever seen work.

- **Progress photos rendering.** `S3Service` returns
  `https://local-dummy-download-url.example.com/...` in local dev, which never
  resolves. The photo grid has only ever been observed in its placeholder
  fallback state. Whether a real photo displays is untested.
- **Program cover uploads.** Same dummy-URL bypass locally; the browser `PUT`
  against real S3 CORS has never run.
- **The Razorpay webhook → activation round trip.** Local dev skips real
  Razorpay calls entirely, so activation has only been exercised against the
  dev-mode stub.
- **Planner drag/drop** (move and Shift-copy) — verified by hand locally, worth
  one pass on the deployed build.

---

## 5. Unfinished UI a paying coach will see

This is the honest state of the product surface, not a bug list.

- **8 Library sub-pages are placeholder stubs**: `/library/forms`, `/habits`,
  `/ingredients`, `/meal-plans`, `/nutrition`, `/recipes`, `/sections`,
  `/tasks`. Each renders a "coming soon" description. They are reachable from
  the Library section pane.
- **16 buttons show a "coming soon" toast** rather than doing anything —
  concentrated in the Exercises and Workouts library screens (tag browser, AI
  suggestions, bulk actions, share, row actions) plus `Master Planner` on the
  program planner.
- **One client-detail tab** ("part of the broader client roadmap") is a stub.
- **Live Sync** column on the Programs list is decorative by design.
- **Package / pkgEnd** slot on client detail is unwired — it needs a per-client
  billing model, which does not exist (subscriptions are coach-tier).

Decide per item whether to finish it, hide it, or leave it visibly labelled.
Eight dead nav entries is a lot for a product someone is paying ₹999+/month for.

---

## 6. Suggested order

1. Fill in `lib/legal.ts`, get the policies reviewed (§1.1, §1.2)
2. Create the Razorpay live plans and wire the webhook (§1.4)
3. Set up hosting + all environment variables, confirm both dev-mode flags are
   off (§1.3, §2)
4. Deploy, then work through §4 — the four things nobody has seen work
5. Decide what to do about §5 before inviting the first paying coach
6. Point the Android release build at the real host (§1.5) if shipping the app

Related: `PROGRESS.md` (feature history and open backlog), and P6 in that file,
which tracks the §4 items.
