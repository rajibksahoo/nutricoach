# NutriCoach — Design Match Plan

> **Plan reset (2026-05-08):** the original 30-day MVP plan is archived at the bottom of this file. The single active goal now is to **match the design produced by Claude Design** for the NutriCoach product surface, screen by screen.
>
> Source of truth: `nutricoach-workout-builder/` design bundle (Workout Builder.html + sibling JSX prototypes + `colors_and_type.css`). Implement in `nutricoach-web` (Next.js + Tailwind). Backend (`nutricoach`) only changes if a screen needs an API that does not yet exist — design parity is not blocked on backend work.

---

> **Before going live:** `GO-LIVE.md` lists everything that must change before
> taking a real payment — business details, Razorpay live setup, environment
> variables, the four things never verified outside local dev, and the unfinished
> UI a paying coach would see.

## Quick Status (updated 2026-06-11 after full audit)

| Area | Status |
|------|--------|
| Backend (auth, clients, plans, billing, progress, AI, WhatsApp, messaging, library) | ✅ Built — all 10 modules with integration tests |
| Frontend design tokens (Indigo + Teal, Inter, new radii/shadows) | ✅ Done |
| App shell sidebar | ✅ Done — 212px slate-900 surface with indigo active state |
| Workout Builder screen + modals | ✅ Done |
| Library (section pane + right-pane) | ✅ Done — right-pane migrated to indigo (web PR #36) |
| Clients screen | ✅ Done — design ported, list + sparklines wired to backend |
| Inbox / Messaging screen | ✅ Done — backend shipped (PR #19), InboxScreen wired to `/api/v1/messages` (web PR #37) |
| Polish pass (toasts, focus rings, empty states, hover states) | ✅ Done (web PR #38) |
| Client portal (login, home, meal-plans, chat, check-ins, progress, profile) | ✅ Done — wired to `/api/v1/portal/*` |
| Programs screen (list + 4-week planner + create/edit/assign) | ✅ Done — Claude-design port (be #24, web #43) |
| Android client app (`nutricoach-android`) | ✅ Working end-to-end — all screens + client write-backs (be #29, android #1, 2026-07-08) |
| Dashboard (daily action queue) | ✅ Done — rebuilt on a new `/coach/dashboard/overview` aggregate (be #31, web #50) |
| Revenue path (onboarding, legal/pricing pages, Razorpay Checkout) | ✅ Done — web `feat/billing-legal-onboarding`; **business details in `lib/legal.ts` are still placeholders** |
| Coach-side Training tab (client detail) | ✅ Done — programs + workouts + schedules on one tab (be/web `feat/client-training-assignments`) |
| Programs list Tags + Equipment columns (P3) | ✅ Done — tags input + derived equipment (be/web `feat/program-equipment-tags`) |
| Progress-photos grid (client Overview) | ✅ Done — real thumbnails + View All (be/web `feat/client-progress-photos`); **real image rendering is unverified until prod** |
| Program templates + planner notes + list filters (P5) | ✅ Done — coach-owned templates, not a curated catalogue (be/web `feat/program-templates`) |
| Client-detail cards: Training, Notes, Updates, Limitations | ✅ Done — (be/web `feat/client-detail-data`); only **Package / pkgEnd** is left unwired |

**The design-match queue is complete**, the Programs screen has shipped, **P1/P2/P4 are done**, the **Dashboard has been rebuilt as a daily action queue**, and the **revenue path (onboarding + legal pages + real Razorpay Checkout) has shipped**. Next work comes from the backlog below. The design-match queue and the unwired-slot list are now clear except for two items that need **product decisions, not code**: `Master Planner` (undefined anywhere) and **Package / pkgEnd** (needs a per-client billing model — today subscriptions are coach-tier). The remaining engineering work is **P6, prod-only verification**, and filling in `nutricoach-web/lib/legal.ts` before taking a payment. **Before going live:** fill in the `REPLACE_ME` business details in `nutricoach-web/lib/legal.ts` and have the policy copy reviewed.

- Revenue path (web `feat/billing-legal-onboarding`, frontend-only — `Coach` already had `businessName`/`gstin`) — the first run and the paying path. **Onboarding:** a skippable 3-step setup at `/onboarding` (identity → practice + GSTIN validation → first client); `auth/otp` now routes on the backend's `isNewCoach`, falling back to "no name set" so an interrupted first run resumes. **Legal:** `/terms`, `/privacy`, `/refund`, `/pricing`, `/contact` under a public `(legal)` group with a shared `LegalShell`, linked from the landing footer — Razorpay merchant onboarding and the DPDP Act both require these to be public and consistent. Every business detail lives in `lib/legal.ts` as an explicit `REPLACE_ME` placeholder (`rg REPLACE_ME lib/legal.ts`); **the copy is a draft and is not legal advice.** **Billing:** real in-page Razorpay Checkout via `lib/use-razorpay.ts` (the old flow used `window.open`, which was popup-blocker-prone and gave no success signal) plus `pollUntilActive` — the webhook, not the browser, is the source of truth for activation. One `lib/plans.ts` catalogue now feeds the landing page, `/pricing` and `/billing`, mirroring `SubscriptionGate.clientLimitFor`. Also hardened dev affordances: `IS_DEV_MODE` requires `NODE_ENV !== "production"` **and** the flag, so a stray env var can't ship an "OTP is 111111" banner to real users. 43/43 e2e, tsc + build clean.
  - **Note:** the new-coach redirect broke `auth.spec.ts`, which logs in with a fresh phone and expected `/dashboard`. The spec was the stale side; it now expects `/onboarding`.
- **P4 — hardcoded trial UI.** ✅ **Done.** `ProgramListView`/`ProgramPlannerView` render `TrialChip` fed by a new `lib/use-subscription.ts` hook over `GET /billing/status` (the overview endpoint is dashboard-only). **Caught in review:** the hook derived its day count with `Math.ceil` while `DashboardOverviewService` uses `ChronoUnit.DAYS.between` (truncating), so the same coach read "14 days left" on Programs and "13" on the dashboard. Now floored to match, gated on `status === "TRIAL"`, and reusing `getBillingStatus` from `lib/billing-api.ts` instead of a hand-rolled response type. Guarded by a cross-screen e2e that signs in as a **fresh** coach on purpose — the shared E2E coach's trial expired in July, so both formulas clamp to 0 and the test would have passed either way.

- Coach-side Training tab (be/web `feat/client-training-assignments`) — a coach can attach training three ways (assign a program, assign a workout, schedule a workout for a date) but the client-detail Training tab showed only the dated schedules, so assigning an 8-week program left the tab reading "nothing assigned" while the client's own portal listed the workouts. **Backend:** new `GET /api/v1/library/clients/{clientId}/assignments` (`ClientTrainingService` + `ClientTrainingController`) returning programs and workouts for one client — assignments were previously queryable only *by program* or *by workout*, the wrong axis for a client screen. Both repository finders already existed, so no schema or query work. Unassign reuses the existing per-program/per-workout DELETEs. Assignments whose program or workout was since deleted are dropped rather than rendered nameless. **Names are resolved server-side** — `ScheduleResponse` gained `workoutName` too — which let the frontend delete `workoutNamesCache`, a module-level mutable Map that fetched every workout once per session and never invalidated, so a rename showed stale text until reload. `ClientTrainingIntegrationTest` (6 cases incl. tenant isolation); 282 backend tests green. **Frontend:** `TrainingTab` extracted out of `ClientsScreen.tsx` (985 → 836 lines) into its own file with shared `Card`/`CardTitle`/`iconBtnStyle` in `detail-ui.tsx`; three cards, each hidden when empty, optimistic remove with rollback. 4 new Playwright specs; 47/47 e2e, tsc + build clean, verified by screenshot against the live stack.
  - **Note:** the new integration test first collided with `WorkoutTemplateIntegrationTest` on test phone `9800050001`. That class's `@BeforeEach` deletes its coach without deleting clients first (it never had any), so the leftover client tripped `fk_clients_coach` and failed 4 unrelated tests. Test phone numbers are a shared namespace — `grep -rho "98000[0-9]\{5\}" src/test` before picking one.
  - **Also fixed:** `client-detail.spec.ts` asserted `getByText("Profile")` unscoped, which matches both the sidebar nav link and the Settings panel heading — it tripped strict mode when the panel rendered in time and passed on the *sidebar* when it didn't. Now scoped to `getByRole("main")`.

- P3 — Tags and Equipment columns (be/web `feat/program-equipment-tags`). **Tags** turned out to be half-built: the list already rendered `p.tags` and the backend already accepted tags on create *and* update — but no modal had a tags field, so the column was permanently a dash. Added a chip input to `CreateProgramModal` (Enter or comma commits, Backspace deletes the last chip, case-insensitive dedupe, whitespace normalised) wired through both the library and planner submit paths. **Equipment** is genuinely derived, not stored: one batched native query in `ProgramDayRepository.findEquipmentByProgramIds` walks `program_days → workouts → workout_section_assignments → workout_sections → workout_section_exercises → exercises.equipment`, skipping soft-deleted rows and blank equipment, grouped so the whole list costs one extra query rather than one per row. `ProgramSummaryResponse` gained `equipment`; `ProgramEquipmentIntegrationTest` (6 cases: derivation, dedupe across days, no days, bodyweight/blank, deleted exercise, deleted workout). 288 backend tests green; 4 new Playwright specs, 51/51 e2e, tsc + build clean.
  - **Layout regression caught by looking at it, not by the tests:** the row grid gave Tags **90px** and neither data cell truncated. That was harmless while both columns were dashes; with real values the text spilled over the program name, which was itself starved (`1fr` resolved to ~144px including the cover tile, wrapping "8-Week Hypertrophy" across three lines). Retightened to `1fr 120px 150px 80px 60px 90px 84px` with `gap: 12` and ellipsis truncation on both cells (full value in `title`). Beside the Library section pane this list only gets ~940px — worth remembering before adding another column.

- Progress-photos grid (be/web `feat/client-progress-photos`) — the Progress Photos card on client Overview rendered three grey boxes labelled "progress 1/2/3" and `photos` was hardcoded `[]`, because a `ProgressPhoto` belongs to a *progress log*, not a client, and nothing could ask for a client's photos without walking every log. **Backend:** `GET /api/v1/clients/{clientId}/progress/photos` (`ProgressService.getPhotos`, beside `/chart`) joining `ProgressPhoto` to `ProgressLog` to filter by client and carry each photo's log date back; new `ClientPhotoResponse` (named apart from the log-scoped `PhotoResponse` — springdoc keys components by simple name). `ClientProgressPhotosIntegrationTest` (5 cases incl. excluding another client *of the same coach*, which is the join's real failure mode); 293 tests green. **Frontend:** `components/clients/ProgressPhotos.tsx` — three most recent thumbnails with log dates, **View All** modal grouping every photo by date, and the dead **Compare** button removed rather than left decorative. `ClientDetail.photos` deleted: photos are now fetched per selected client like the chart, so a field permanently set to `[]` was pure cruft. 4 new Playwright specs; 55/55 e2e (three consecutive clean runs), tsc + build clean.
  - **Pre-signed URLs expire after 60 minutes, and local dev never resolves them at all** (`S3Service` returns a `local-dummy-download-url.example.com` host). So a dead image is a normal state: every tile has an `onError` fallback to a labelled placeholder. **Real image rendering is therefore unverified** — it needs the prod bucket; folded into P6 below alongside the CORS check.
  - **Caught by a flaky test, and it was a real defect:** `photos` defaulted to `[]` while the fetch was in flight, so the card flashed "No photos uploaded yet." on every client before the photos arrived. Fixed by keeping `photos` **undefined** until resolved and rendering a distinct loading state — the test only failed under parallel load, but the flash was there for every user.

- P5 — Programs stubs (be/web `feat/program-templates`). Four of the five stubs closed; each turned out to be a different shape.
  - **Per-day notes** — `ProgramDay.notes`, `SetProgramDayRequest.notes` and `setProgramDay(…, notes)` had all existed since the Programs build; only the affordance was missing, so notes could be stored but never written. Added a note editor to the day-card menu and a note indicator on cards. **Caught while wiring it:** the drag/move and paste handlers called `setProgramDay` *without* notes, and that endpoint replaces the row — so once notes existed, dragging a card would have silently erased its note. Both call sites now resend them.
  - **Filter / Tags toolbar** — both buttons now open popovers that filter the loaded list (no new endpoint). Options are derived from the coach's own data rather than a fixed list, so the panel can never offer a value matching nothing. The empty state's "Clear search" now clears filters too, which would otherwise have been a dead end.
  - **Save to Library** — **deleted rather than built.** A planner day stores a `workoutId` picked from the coach's library, so the workout is *already* in the library; the action had no meaning. What it was reaching for is the template feature below.
  - **Explore Templates** — built as **coach-owned templates, not the curated global catalogue** the banner implied. Mirroring `workout_templates` (global, no `coach_id`, seeded) would have needed someone to author credible training blocks before the feature did anything; letting a coach flag a program they already built is useful on day one and covers the missing "duplicate program" action too. Backend: `is_template` on `programs` (changeset 025), `GET /programs?templates=`, `PUT /programs/{id}/template`, `POST /programs/{id}/instantiate` deep-copying the program and its days. 300 backend tests green.
  - **`Master Planner` is still a `coming soon` toast** — deliberately untouched, see the backlog note.
  - **MapStruct silently mapped `isTemplate` to false.** Lombok names the getter `isTemplate()`, so the bean property is `template` while the DTO component is `isTemplate` — no match, no warning, just `false` on every response. Fixed with an explicit `@Mapping(target = "isTemplate", source = "program.template")`. Watch for this on any other boolean DTO field.
  - **`uniquePhone()` in the e2e helpers was a bare millisecond timestamp**, so parallel workers could mint the same phone and race the unique-phone constraint — surfacing as an occasional `demo-login failed: 500` once the suite passed ~60 tests. Now timestamp + random suffix. 61/61 green across three consecutive runs.

- Client-detail cards (be/web `feat/client-detail-data`) — four of the five unwired slots on the Clients screen, each a different shape.
  - **Training stats** — real planned-vs-completed over 7 and 30 days, next week, and the last completion. This was blocked when the screen was designed and became cheap once completions, schedules and program assignments all landed. The date-range expansion was **extracted out of `PortalWorkoutService.listUpcoming`** into a shared `plannedBetween`, which is the same walk bounded to a range — so the coach's Training card and the client's own workout list can never disagree about what was planned.
  - **Notes** — new `client_notes` table (changeset 026) + CRUD + inline add/edit/delete. A note is addressed through its client, so the service checks the note actually belongs to that client; otherwise one coach's note id could be edited under a different client of theirs.
  - **Updates feed** — **derived at read time, no `activity_log` table.** Unions messages, check-ins, progress logs, workout completions and the join event. The alternative (a table written from every mutation path) touches five modules and is easy to leave half-wired; this costs a handful of small indexed reads per view and cannot drift from the truth. Filter menu is wired to the types the feed can actually contain.
  - **Limitations** — **a fix, not a feature.** The design shows a date per entry, but `health_conditions` is a plain `string[]`, so the UI filled in the *client's join date*. A coach reading "Shoulder impingement · 12 Mar" would think that was when it was recorded. The date is simply gone now; promoting to a dated table stays available if it ever matters.
  - Also removed the **`Check Result`** button on the Training card — never wired, and a dead button beside real numbers reads worse than no button.
  - **`Logged weight 72.50 kg`** — `weight_kg` is `numeric(5,2)`, so both this feed and the dashboard's rendered the trailing zero. Now formatted through `common/util/Measures.formatKg`; fixing only the new feed would have made two views disagree about the same event.
  - **Self-inflicted:** inserting the new method above `listUpcoming` stranded its `@Transactional`, giving one method two and the other none. Lombok then stopped processing and ~40 errors appeared in unrelated files (`BillingService` "cannot find symbol: log"). When Lombok symbols vanish en masse, look for an annotation error, not a real break.
- 313 backend tests green (13 new); 66/66 e2e (6 new), tsc + build clean.

---

## Design source

Canonical bundle (extracted, do not check in):
`C:\Users\rajib\AppData\Local\Temp\design\nutricoach-workout-builder\`

| File | Implements |
|------|------------|
| `project/Workout Builder.html` | App shell + screen wiring |
| `project/colors_and_type.css` | Brand tokens, Inter font stack |
| `project/shared.jsx` | AppSidebar, Toast, helpers, palettes |
| `project/clients.jsx` | Clients screen |
| `project/inbox.jsx` | Messaging / Inbox screen |
| `project/library.jsx` + `workouts.jsx` | Library screen + Workouts tab |
| `project/builder-screen.jsx` + `builder.jsx` | Workout Builder canvas |
| `project/workout-editor.jsx` | Workout Editor modal |
| `project/exercise-modal.jsx` | Exercise create/edit modal |
| `project/workout-create-modals.jsx` | Create-chooser + template picker |
| `project/modals.jsx` | Assign + Schedule modals |
| `project/tweaks-panel.jsx` | Dev-only — **skip** |

Three chat transcripts live alongside (`chats/chat1.md` … `chat3.md`) — re-read before iterating on a screen so the user's intent stays intact.

---

## Implementation queue (in order)

Each item lands on its own `feat/design-…` branch. After PR + review the next item starts.

1. **Design tokens** (`feat/design-tokens`)
   - Inter (self-host or `next/font/google`), Inter Display, Inter XL fallbacks
   - Move palette in `globals.css` from emerald to indigo `#4F46E5` + teal `#14B8A6`
   - Replicate full token set from `colors_and_type.css` as CSS variables
   - Update `lib/utils.ts`/Tailwind config if any tokens are referenced through Tailwind utility shortcuts
   - Refresh `Button`, `Card`, `Badge`, `Input`, `Spinner` to consume new tokens
2. **App shell** (`feat/design-app-shell`)
   - Port `AppSidebar` from `shared.jsx` to `components/layout/Sidebar.tsx`
   - Update sidebar nav items + active state to match design (indigo accent, slate-900 surface)
3. **Workout Builder screen** (`feat/design-workout-builder`)
   - Replace `app/(dashboard)/workout-builder/_components/builder-screen.tsx` and `builder.tsx`-equivalents with the design's `builder-screen.jsx` + `builder.jsx`
   - Keep wiring to existing backend APIs where present; mock the rest with the design's static fixtures until APIs land
4. **Workout Editor modal** (`feat/design-workout-editor`) — `workout-editor.jsx`
5. **Exercise modal** (`feat/design-exercise-modal`) — `exercise-modal.jsx`
6. **Create-workout modals** (`feat/design-create-workout-modals`) — `workout-create-modals.jsx`
7. **Assign + Schedule modals** (`feat/design-assign-schedule-modals`) — `modals.jsx`
8. **Library screen** (`feat/design-library`) — `library.jsx` + `workouts.jsx`; the design's outer chrome is a **232px grouped section pane** (not tabs), already matching the existing `/library/*` route group. The redesign work here is the right-pane content: Everfit-style table, filter chips, bulk-action bar.
9. **Clients screen** (`feat/design-clients`) — `clients.jsx`
10. **Inbox / Messaging screen** (`feat/design-inbox`) — `inbox.jsx`
11. **Polish pass** — toast, focus rings, empty states, keyboard hints, hover states audited against the prototypes

---

## Working rules

- **Match the visual output, not the prototype's structure.** The prototypes are React + inline styles loaded by Babel-standalone; production code stays Next.js + TypeScript + Tailwind, with design tokens in CSS variables and component primitives in `components/ui/*`.
- **One screen per branch.** Open a PR per branch and wait for explicit approval before merging.
- **No backend changes** unless a screen needs an endpoint that does not exist. If one does, note it here and call it out in the PR.
- **Skip the Tweaks panel.** It is a design-time dev affordance and should not ship.
- **Don't render the prototype in a browser** unless explicitly asked — read HTML/CSS/JSX directly per the bundle README.

---

## Done

- Design tokens (`feat/design-tokens`) — Indigo + Teal palette, JetBrains Mono added.
- App shell (`feat/design-app-shell`) — sidebar at 212px with indigo active state.
- Workout Builder canvas (`feat/design-workout-builder`) — already ported; redundant tokens stripped from `workout-builder.css`, font binaries deleted, route opted into fullBleed.
- Workout Editor modal (`feat/design-workout-editor`) — already ported; entrance animations added.
- Library section pane (`feat/design-library-and-modal-polish`) — restored to match `library.jsx` `navGroups`: 232px white surface, four uppercase group headers (Fitness / Nutrition / Habits / Forms), indigo active state, violet `NEW` pill. Modal animations (`exercise`, `create-workout`, `assign`, `schedule`) revived from broken `fadeIn`/`slideUp` references to the actual `wb-fadeIn`/`wb-slideUp` keyframes.
- Clients + Inbox screens (`feat/design-clients-and-inbox`) — ported `clients.jsx` (sub-pane + tabbed detail with Overview/Metrics, sparkline charts, status pills, profile card, updates feed) and `inbox.jsx` (3-pane: conversations + thread bubbles + profile/notes/updates). Replaced the old `(list)` route group + `ClientsSidebar`. Added `/clients` and `/messages` to the dashboard `fullBleed` paths.
- Clients backend wiring (`feat/clients-backend-wiring`) — `ClientsScreen` now fetches `GET /api/v1/clients` on mount and lazy-loads `GET /api/v1/clients/{id}/progress/chart` for the selected client; Weight + Body Fat sparklines come from real progress logs. Mapper in `lib/clients-api.ts` handles status enum, goal label, deterministic avatar tone, joined date, and limitations from `healthConditions`. Falls back to the design fixture in dev when the API URL is unset / returns empty / errors.
- Programs (`feat/programs-design-match`, be #24 / web #43) — Program Library list (gradient/initial cover tiles, weeks, row actions) + in-page **1/2/4-week drag/drop planner** (move + Shift-copy, Add Week, per-day workout picker, exercise-line card previews) + **Create/Edit modal** with cover-image upload via the existing S3 presign flow + **Assign-to-clients** modal. Backend `Program` gained `weeks`/`modality`/`experienceLevel`/`tags`/`cover_*` (weeks authoritative, `duration_days = weeks*7`); new `client_program_assignments` table + `ProgramAssignmentService`. Rendered from both `/library/programs` and `/library/fitness/programs`; old `programs/[id]` detail routes removed. Backend 206 tests green; frontend typecheck + build pass.
- Program → client visibility (`feat/programs-assign-to-client`) — **P1 closed** via a **derive-live** approach (no materialized schedule rows): `PortalWorkoutService` expands each active `ClientProgramAssignment` into dated upcoming workouts (start date + dayNumber − 1, today onward) with exercise-line previews; exposed at `GET /api/v1/portal/workouts` (`ClientWorkoutController`, role CLIENT). Frontend: `lib/client-workouts-api.ts`, new `/portal/workouts` page (grouped-by-date list + empty state), "Today's Workout" card on the portal home, "Workouts" tab in `ClientNav`. `PortalWorkoutIntegrationTest` (4 cases) green; frontend tsc cle- Program Calendar card menu (`feat/program-calendar-card-menu`, frontend-only) — the planner workout-card ••• button (in `ProgramPlannerView.tsx`), previously a one-click delete, is now a **meatballs menu**: hover shows a black "More Options" tooltip; click opens **Save to Library / Copy / Delete**. Save → green top-right toast ("Workout saved to Library", UI-only — no save-as-template endpoint yet). Copy → toast + click-to-paste mode where hovering any day cell reveals a blue **Paste** button that writes the workout to that day via `setProgramDay`. Delete → confirm dialog ("Are you sure…") with Cancel + red **OK** routed to the existing `removeDay`. `Esc` cancels paste/dialog; the existing Shift-drag copy/move flow is untouched. Verified via headless-Chrome screenshots of all five states; tsc clean.

- Android client wave (2026-07-08, be #29 / android #1 / #30) — turned the read-mostly Android app into a **working client app**, built by parallel Opus subagents per `architect-parallel-dispatch`. **Backend** (`feat/portal-client-writes`): `POST /api/v1/portal/check-ins` (client-supplied `coachNotes` stripped) and `POST /api/v1/portal/progress` reusing existing services; workout completion via changeset 024 `client_workout_completions` + idempotent `POST /api/v1/portal/workouts/complete` + `completed` flag on `GET /portal/workouts` (260 tests green). **Android** (`feat/fitcoach-app`): Meal Plans list + detail (consuming the previously unused endpoints), Mark-done on the Today hero + Done pills on Coaching, real Task card (last-7-days check-ins) + check-in form overlay (meal-plan picker, adherence slider), Log-weight dialog on You with chart refresh, Settings Profile/About subviews, `nutricoach://join?coach=` deep-link prefill. Every flow verified on the emulator against the live backend with DB-row checks. Deferred to a later wave: push/FCM, meal logging + macro-goal sync, Google Fit steps, streaks (still stubs per `DESIGN_SPEC.md`). Also merged #30 fixing CLAUDE.md's stale React-Native mobile row.

- Dashboard revamp (be #31 / web #50) — replaced the four vanity counters + recent-clients list with a **one-window daily action queue**. Backend: new `GET /api/v1/coach/dashboard/overview` (`DashboardOverviewService`) aggregating counts, a prioritised action queue (unanswered messages → overdue check-ins → expiring plans → no plan → onboarding), today's sessions/check-ins, roster + tier limit, subscription/trial and an activity feed. Built server-side because every signal was previously per-client only (~3 requests × roster from the browser); it now runs ~10 coach-scoped queries flat, using new `GROUP BY` aggregates (`findUnreadConversations`, `findLastCheckInPerClient`) and loading the roster once into a map. Overdue reuses `CheckInReminderScheduler`'s 7-day rule; the cap comes from `SubscriptionGate.clientLimitFor` (now public) so the reported and enforced limits can't drift. Old `/coach/dashboard` kept but `@Deprecated` — no contract break. `DashboardOverviewIntegrationTest` (16 cases incl. tenant isolation) green; full suite 276 green. Frontend: `lib/dashboard-api.ts` typed off the regenerated `types/api.ts`, `components/dashboard/*`, `/dashboard` added to `fullBleed`. The queue **groups by client** (one row per person, top-priority reason leading, others as secondary pills) — showing one client under several reasons read as noise. Also extracted the duplicated `Spark`/`Delta`/`ClientAvatar`/`StatusPill` into `components/ui/` (Clients + Inbox now import them) and replaced two hardcoded values with real data: the sidebar's fake `badge: 3` and the trial chip's copy. 5 new Playwright specs; 24/24 e2e, tsc + build clean.
  - **Note:** `DashboardOverviewResponse.RecentClient` had to be renamed `RosterClient` — it collided with the deprecated `DashboardResponse.RecentClient` in the OpenAPI component namespace, so springdoc emitted only one schema and the generated client type silently lost `createdAt`. Watch for this whenever two nested DTO records share a simple name.

---

## Decisions (locked 2026-05-08)

- **Library:** adopt the design's **232px grouped section pane** — Fitness / Nutrition / Habits / Forms as uppercase group headers with items underneath each (matches `library.jsx` `navGroups`). _Earlier note here said "top tabs" — that was a misread of the design from only `shared.jsx`; corrected 2026-05-09 after the implementation didn't match._
- **Fonts:** load Inter via `next/font/google`. No self-hosted TTFs. Inter Display / Inter XL fall back to Inter weights — close enough at our scale.
- **Brand migration:** **per-screen**. Each redesign branch migrates its own screen from emerald to indigo. No global flag-day rewrite. `globals.css` keeps both palettes available until the queue is done.

---

## Backlog (audit 2026-06-11)

### Shipped since last update
Messaging is **done**: `messaging/` module (entity/repo/service/mapper, `MessageController` at `/api/v1/messages/*` + `ClientMessagingController` at `/api/v1/portal/messages`, changeset `015-create-messages.xml`, `MessagingIntegrationTest`) and `InboxScreen` is wired via `lib/messaging-api.ts` (web PR #37). The original messaging proposal that lived here is obsolete and was removed. Still open from that proposal (now backlog): mark-as-read endpoint, WhatsApp notification on in-app send, WATI inbound webhook, real-time delivery (polling/SSE).

### Backend exists, UI missing (frontend-only work, highest value first)
1. ~~**Schedule/assignment visibility in the client experience**~~ ✅ **Done** — the client-detail Training tab now shows all three sources (assigned programs, assigned workouts, dated schedules) with inline unassign. See the shipped log above.
2. ~~**Progress-photos grid on Clients screen**~~ ✅ **Done** — new all-photos-per-client endpoint + real thumbnails and a View All modal. See the shipped log above.

> Shipped since the 2026-06-11 audit: **Programs screen** (be #24 / web #43), **library row actions** for workouts (edit / duplicate / assign / delete in `app/(dashboard)/library/workouts/page.tsx`), **P1 — assigned programs reach the client portal** (`feat/programs-assign-to-client`), and **P2 — assign-modal parity for programs** (`feat/assign-program-modal-parity`). All removed from this list.

### Known gaps / follow-ups from the Programs work (recommended next, in order)
- **P1 — "Assign Program" reaches the client.** ✅ **Done** (`feat/programs-assign-to-client`) — taken via derive-live rather than materializing `client_workout_schedules`: `PortalWorkoutService` + `GET /api/v1/portal/workouts` + portal `/portal/workouts` page. If a coach-facing calendar or per-day completion tracking is later needed, revisit materialization.
- **P2 — Assign-Program modal parity.** ✅ **Done** (`feat/assign-program-modal-parity`, web-only — the backend endpoints already existed). `AssignProgramModal` takes a `programId` prop, fetches assignees on open via new `listProgramAssignments`/`unassignProgram` in `lib/programs-api.ts`, and renders a "Currently assigned (N)" block with inline remove (optimistic + rollback, no confirm dialog, matching `AssignWorkoutModal`). The row subtitle adds `Starts {date}` — programs carry `startDate`, workouts don't. Already-assigned clients are **disabled** in the picker with an `ASSIGNED` pill, because backend `assign` is idempotent: re-selecting them would silently no-op rather than update their start date.
- **P3 — Real data in empty columns.** ✅ **Done** — Tags are now settable and Equipment is derived; see the shipped log above. (Live Sync stays decorative.)
- **P4 — Replace hardcoded trial UI.** ✅ **Done** — see the shipped log above (`lib/use-subscription.ts` + `TrialChip`, day-count formula reconciled with the backend).
- **P5 — Wire/scope remaining stubs.** 🟡 **Mostly done** — per-day notes, Filter/Tags and coach-owned templates shipped; see the log above. **Still open: `Master Planner`**, which has no definition anywhere in the repo or the design bundle and needs a product decision before it can be built.
- **P6 — Prod-only verification.** Manually confirm planner drag/drop (move + Shift-copy); confirm the prod S3 bucket CORS allows browser `PUT` for cover uploads (local dev uses the `local-` dummy-URL bypass); **confirm progress photos actually render** — pre-signed `GET` URLs never resolve locally, so the photo grid has only ever been seen in its placeholder state.

### Other unwired Client fields
The redesigned `/clients` screen has visual slots for several things the backend doesn't track yet. Decide per-field whether to build the backend or drop the slot:

- ~~**Notes**~~ ✅ **Done** — `client_notes` table + CRUD + an editable card.
- ~~**Limitations / injuries**~~ ✅ **Resolved as a fix, not a feature** — still `clients.health_conditions`, but the UI no longer invents a date. Revisit only if dated injury history is genuinely wanted.
- ~~**Progress photos**~~ ✅ **Done** — all-photos-per-client endpoint + grid.
- ~~**Updates feed**~~ ✅ **Done** — derived at read time, **no `activity_log` table**.
- ~~**Training stats**~~ ✅ **Done** — the assignment story landed, so this became cheap.
- **Package / pkgEnd** — billing-side fields. Tie into `subscriptions` once per-client packages exist (today subscriptions are coach-tier, not per-client).

Until each is implemented, the design's empty-state placeholders ("No notes yet", "No photos uploaded yet", "Not tracked", etc.) carry the screen.

---

## Archived: original 30-day MVP plan

The original 30-day plan that drove backend implementation is preserved at `PROGRESS.archive.md` for historical reference. Backend status as of the reset:

- Week 1 (scaffold + auth + schema): complete
- Week 2 (clients + meal plans + dashboard): backend complete
- Week 3 (progress + billing + WhatsApp): backend complete
- Week 4 (AI + branding + launch): partially complete (Day 22, 24, 26 done; 23 partial; 25, 27, 28, 29, 30 deferred until design parity is reached)

Backend modules and integration tests remain green and are not part of this redesign queue.