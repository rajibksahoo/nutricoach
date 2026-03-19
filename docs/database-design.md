# NutriCoach — Database Design

## Principles

| Rule | Detail |
|---|---|
| **Multi-tenancy** | Every table has `coach_id UUID NOT NULL` — the tenant discriminator |
| **PKs** | UUID everywhere (PostgreSQL `gen_random_uuid()`) |
| **Migrations** | Liquibase XML only — never Flyway, never raw SQL files |
| **Hibernate** | `ddl-auto: validate` — Liquibase owns the schema |
| **Auditing** | `created_at`, `updated_at` on every table via JPA `@CreatedDate` / `@LastModifiedDate` |
| **Soft deletes** | `deleted_at` on client-facing tables (coaches, clients, meal_plans) |
| **Amounts** | Monetary values stored in **paise** (integer), never decimal |
| **Timestamps** | `TIMESTAMPTZ` (UTC) in DB, converted to `Instant` in Java |

---

## ERD

```mermaid
erDiagram

    COACHES {
        uuid id PK
        varchar phone UK
        varchar name
        varchar email
        varchar subscription_tier "STARTER | PROFESSIONAL | ENTERPRISE"
        varchar subscription_status "TRIAL | ACTIVE | PAST_DUE | CANCELLED"
        varchar razorpay_customer_id
        varchar gstin
        varchar business_name
        timestamptz trial_ends_at
        timestamptz deleted_at
        timestamptz created_at
        timestamptz updated_at
    }

    CLIENTS {
        uuid id PK
        uuid coach_id FK
        varchar phone
        varchar name
        varchar email
        date date_of_birth
        varchar gender "MALE | FEMALE | OTHER"
        int height_cm
        decimal weight_kg
        varchar goal "WEIGHT_LOSS | WEIGHT_GAIN | MAINTENANCE | MUSCLE_GAIN"
        varchar dietary_pref "VEG | NON_VEG | VEGAN | JAIN | EGGETARIAN"
        jsonb health_conditions
        jsonb allergies
        varchar activity_level "SEDENTARY | LIGHT | MODERATE | ACTIVE | VERY_ACTIVE"
        varchar whatsapp_number
        varchar status "ONBOARDING | ACTIVE | INACTIVE"
        timestamptz deleted_at
        timestamptz created_at
        timestamptz updated_at
    }

    MEAL_PLANS {
        uuid id PK
        uuid coach_id FK
        uuid client_id FK
        varchar name
        text description
        date start_date
        date end_date
        varchar status "DRAFT | ACTIVE | COMPLETED | ARCHIVED"
        boolean ai_generated
        int total_calories_target
        timestamptz deleted_at
        timestamptz created_at
        timestamptz updated_at
    }

    MEAL_PLAN_DAYS {
        uuid id PK
        uuid meal_plan_id FK
        int day_number "1-7"
        int total_calories
        decimal total_protein_g
        decimal total_carbs_g
        decimal total_fat_g
        timestamptz created_at
        timestamptz updated_at
    }

    MEALS {
        uuid id PK
        uuid meal_plan_day_id FK
        varchar meal_type "BREAKFAST | LUNCH | DINNER | SNACK | PRE_WORKOUT | POST_WORKOUT"
        varchar name
        time time_of_day
        int sequence_order
        timestamptz created_at
        timestamptz updated_at
    }

    MEAL_ITEMS {
        uuid id PK
        uuid meal_id FK
        uuid food_item_id FK
        decimal quantity_grams
        varchar quantity_unit "g | ml | cup | tbsp | piece"
        int calories
        decimal protein_g
        decimal carbs_g
        decimal fat_g
        timestamptz created_at
        timestamptz updated_at
    }

    FOOD_ITEMS {
        uuid id PK
        varchar name
        varchar name_hindi
        varchar name_regional
        varchar cuisine_type "SOUTH_INDIAN | GUJARATI | PUNJABI | JAIN | BENGALI | RAJASTHANI | MAHARASHTRIAN | NORTH_EAST | PAN_INDIAN"
        varchar category "GRAIN | PROTEIN | VEGETABLE | FRUIT | DAIRY | FAT | BEVERAGE | SPICE | OTHER"
        decimal calories_per_100g
        decimal protein_per_100g
        decimal carbs_per_100g
        decimal fat_per_100g
        decimal fiber_per_100g
        varchar source "IFCT | CUSTOM"
        timestamptz created_at
        timestamptz updated_at
    }

    PROGRESS_LOGS {
        uuid id PK
        uuid coach_id FK
        uuid client_id FK
        date logged_date
        decimal weight_kg
        decimal body_fat_percent
        decimal waist_cm
        decimal chest_cm
        decimal hip_cm
        int adherence_percent "0-100"
        text notes
        timestamptz created_at
        timestamptz updated_at
    }

    PROGRESS_PHOTOS {
        uuid id PK
        uuid coach_id FK
        uuid progress_log_id FK
        varchar s3_key
        varchar photo_type "FRONT | SIDE | BACK"
        timestamptz created_at
    }

    CHECK_INS {
        uuid id PK
        uuid coach_id FK
        uuid client_id FK
        uuid meal_plan_id FK
        date check_in_date
        int adherence_percent "0-100"
        text client_notes
        text coach_notes
        timestamptz created_at
        timestamptz updated_at
    }

    SUBSCRIPTIONS {
        uuid id PK
        uuid coach_id FK
        varchar plan_tier "STARTER | PROFESSIONAL | ENTERPRISE"
        varchar status "TRIAL | ACTIVE | PAST_DUE | CANCELLED"
        varchar razorpay_subscription_id UK
        int amount_paise
        timestamptz current_period_start
        timestamptz current_period_end
        timestamptz cancelled_at
        timestamptz created_at
        timestamptz updated_at
    }

    INVOICES {
        uuid id PK
        uuid coach_id FK
        uuid subscription_id FK
        varchar razorpay_payment_id UK
        int amount_paise
        int gst_amount_paise
        varchar status "PAID | PENDING | FAILED | REFUNDED"
        date invoice_date
        varchar invoice_number UK
        timestamptz created_at
        timestamptz updated_at
    }

    OTP_REQUESTS {
        uuid id PK
        varchar phone
        varchar otp_hash
        int attempts
        boolean verified
        timestamptz expires_at
        timestamptz created_at
    }

    AI_JOBS {
        uuid id PK
        uuid coach_id FK
        uuid client_id FK
        varchar job_type "MEAL_PLAN_GENERATION"
        varchar status "PENDING | PROCESSING | COMPLETED | FAILED"
        jsonb input_payload
        jsonb output_payload
        text error_message
        timestamptz started_at
        timestamptz completed_at
        timestamptz created_at
        timestamptz updated_at
    }

    NOTIFICATION_LOGS {
        uuid id PK
        uuid coach_id FK
        uuid client_id FK
        varchar channel "SMS | WHATSAPP"
        varchar type "OTP | MEAL_PLAN_SHARE | CHECK_IN_REMINDER | PROGRESS_UPDATE"
        varchar status "PENDING | SENT | FAILED"
        varchar external_id
        text message_body
        timestamptz sent_at
        timestamptz created_at
    }

    COACHES ||--o{ CLIENTS : "manages"
    COACHES ||--o{ MEAL_PLANS : "creates"
    COACHES ||--o{ SUBSCRIPTIONS : "has"
    COACHES ||--o{ INVOICES : "billed"
    COACHES ||--o{ AI_JOBS : "triggers"
    COACHES ||--o{ NOTIFICATION_LOGS : "sends"

    CLIENTS ||--o{ MEAL_PLANS : "assigned"
    CLIENTS ||--o{ PROGRESS_LOGS : "logs"
    CLIENTS ||--o{ CHECK_INS : "checks in"
    CLIENTS ||--o{ NOTIFICATION_LOGS : "receives"

    MEAL_PLANS ||--o{ MEAL_PLAN_DAYS : "contains"
    MEAL_PLANS ||--o{ CHECK_INS : "tracked by"

    MEAL_PLAN_DAYS ||--o{ MEALS : "has"

    MEALS ||--o{ MEAL_ITEMS : "contains"

    MEAL_ITEMS }o--|| FOOD_ITEMS : "references"

    PROGRESS_LOGS ||--o{ PROGRESS_PHOTOS : "has photos"

    SUBSCRIPTIONS ||--o{ INVOICES : "generates"

    AI_JOBS }o--|| CLIENTS : "generates plan for"
```

---

## Tables by Module

### `auth` module
| Table | Purpose |
|---|---|
| `otp_requests` | Phone OTP flow — hashed OTP, expiry, attempt counter |

### `coach` module
| Table | Purpose |
|---|---|
| `coaches` | Auth principal + tenant root. One row = one paying customer |

### `client` module
| Table | Purpose |
|---|---|
| `clients` | Coach's clients with dietary profile, goals, anthropometrics |

### `plans` module
| Table | Purpose |
|---|---|
| `meal_plans` | Top-level plan assigned to a client |
| `meal_plan_days` | Day breakdown (day 1–7 for weekly plans) with macro totals |
| `meals` | Individual meals (Breakfast, Lunch, etc.) within a day |
| `meal_items` | Food items within a meal with per-item macros |
| `food_items` | Indian food database (IFCT source) — shared, no `coach_id` |

### `progress` module
| Table | Purpose |
|---|---|
| `progress_logs` | Weight, body fat %, measurements per date |
| `progress_photos` | S3 keys for before/after photos, linked to a log entry |
| `check_ins` | Daily adherence tracking against an active meal plan |

### `billing` module
| Table | Purpose |
|---|---|
| `subscriptions` | Razorpay subscription lifecycle |
| `invoices` | Per-payment invoice with GST breakdown |

### `ai` module
| Table | Purpose |
|---|---|
| `ai_jobs` | Async GPT-4o job queue with input/output JSON payloads |

### `notifications` module
| Table | Purpose |
|---|---|
| `notification_logs` | Audit trail for every MSG91 OTP and WATI WhatsApp message |

---

## Index Strategy

```sql
-- coaches
CREATE UNIQUE INDEX ON coaches(phone);

-- clients
CREATE INDEX ON clients(coach_id);           -- tenant filter (all client queries)
CREATE INDEX ON clients(coach_id, status);   -- dashboard status filter

-- meal_plans
CREATE INDEX ON meal_plans(coach_id);
CREATE INDEX ON meal_plans(client_id);
CREATE INDEX ON meal_plans(coach_id, status);

-- progress_logs
CREATE INDEX ON progress_logs(client_id, logged_date DESC);

-- check_ins
CREATE INDEX ON check_ins(client_id, check_in_date DESC);

-- otp_requests
CREATE INDEX ON otp_requests(phone, created_at DESC);

-- ai_jobs
CREATE INDEX ON ai_jobs(coach_id, status);

-- food_items
CREATE INDEX ON food_items(cuisine_type);
CREATE INDEX ON food_items(category);
```

---

## Subscription Tiers & Feature Gates

| Feature | Starter ₹999 | Professional ₹2,499 | Enterprise ₹4,999 |
|---|---|---|---|
| Active clients | 10 | 50 | Unlimited |
| AI meal plans / month | 5 | 25 | Unlimited |
| Progress photos | No | Yes | Yes |
| WhatsApp reminders | No | Yes | Yes |
| Custom food items | No | Yes | Yes |
| White-label portal | No | No | Yes |

---

## Migration Order (Liquibase)

```
001-create-coaches.xml
002-create-clients.xml
003-create-meal-plans.xml
004-create-meal-plan-days-meals-items.xml
005-create-food-items.xml
006-create-progress-logs-photos.xml
007-create-check-ins.xml
008-create-subscriptions-invoices.xml
009-create-otp-requests.xml
010-create-ai-jobs.xml
011-create-notification-logs.xml
012-seed-food-items-ifct.xml
```

---

## Key Design Decisions

**Why no separate `users` table?**
Coaches are the only auth principals on the backend. Clients log in via a separate client portal (future) with their own lighter auth. Keeping them separate avoids a polymorphic users table and keeps tenant isolation trivial.

**Why `jsonb` for `health_conditions` and `allergies`?**
These are unstructured, coach-defined tags. No need to normalize until query patterns emerge. Postgres `jsonb` gives us indexability if needed later.

**Why `food_items` has no `coach_id`?**
It's a shared reference table (IFCT data). Coaches can add custom items — those will have a `coach_id` column added in a later migration (012+) to distinguish custom from global.

**Why store macros on `meal_items` redundantly?**
Denormalized for read performance — dashboard macro totals without joining `food_items` every time. Updated on write.
