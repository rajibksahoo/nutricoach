# NutriCoach — Design Match Plan

> **Plan reset (2026-05-08):** the original 30-day MVP plan is archived at the bottom of this file. The single active goal now is to **match the design produced by Claude Design** for the NutriCoach product surface, screen by screen.
>
> Source of truth: `nutricoach-workout-builder/` design bundle (Workout Builder.html + sibling JSX prototypes + `colors_and_type.css`). Implement in `nutricoach-web` (Next.js + Tailwind). Backend (`nutricoach`) only changes if a screen needs an API that does not yet exist — design parity is not blocked on backend work.

---

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

**The design-match queue is complete**, the Programs screen has shipped, and **P1 (assigned programs now reach the client portal)** is done. Next work comes from the backlog below — recommended order: assign-modal parity (view/undo for programs), then populate the empty planner/list columns.

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
- Program → client visibility (`feat/programs-assign-to-client`) — **P1 closed** via a **derive-live** approach (no materialized schedule rows): `PortalWorkoutService` expands each active `ClientProgramAssignment` into dated upcoming workouts (start date + dayNumber − 1, today onward) with exercise-line previews; exposed at `GET /api/v1/portal/workouts` (`ClientWorkoutController`, role CLIENT). Frontend: `lib/client-workouts-api.ts`, new `/portal/workouts` page (grouped-by-date list + empty state), "Today's Workout" card on the portal home, "Workouts" tab in `ClientNav`. `PortalWorkoutIntegrationTest` (4 cases) green; frontend tsc clean.

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
1. **Schedule/assignment visibility in the client experience** — coaches can assign workouts and programs, but assignments don't surface back. For **workouts** the assign modal now shows current assignees + unassign (`GET`/`DELETE …/assignments`), but the **client detail Training tab** still doesn't list what's assigned or scheduled (`GET /clients/{clientId}/schedules` is unused). For **programs**, the client portal now shows upcoming workouts (P1, done); the coach-side Training tab is still a stub.
2. **Progress-photos grid on Clients screen** — `client.photos` is always empty; backend only has per-log photos. Needs a small all-photos-per-client endpoint + thumbnail grid consumption.

> Shipped since the 2026-06-11 audit: **Programs screen** (be #24 / web #43), **library row actions** for workouts (edit / duplicate / assign / delete in `app/(dashboard)/library/workouts/page.tsx`), and **P1 — assigned programs reach the client portal** (`feat/programs-assign-to-client`). All removed from this list.

### Known gaps / follow-ups from the Programs work (recommended next, in order)
- **P1 — "Assign Program" reaches the client.** ✅ **Done** (`feat/programs-assign-to-client`) — taken via derive-live rather than materializing `client_workout_schedules`: `PortalWorkoutService` + `GET /api/v1/portal/workouts` + portal `/portal/workouts` page. If a coach-facing calendar or per-day completion tracking is later needed, revisit materialization.
- **P2 — Assign-Program modal parity.** Add the "currently assigned + unassign" list to `AssignProgramModal` (backend `GET`/`DELETE /programs/{id}/assignments` already exist), matching `AssignWorkoutModal`. **Recommended next.**
- **P3 — Real data in empty columns.** Render persisted **tags**; derive **Equipment** for list rows from the assigned workouts' exercises. (Live Sync stays decorative.)
- **P4 — Replace hardcoded trial UI.** "29 days left … / Upgrade" is hardcoded in `ProgramListView`/`ProgramPlannerView`; wire to the coach's real `trialEndsAt` (ideally via a shared header so it's app-wide).
- **P5 — Wire/scope remaining stubs.** `Explore Templates` → a program-template flow mirroring `workout_templates`; re-add per-day notes editing (`ProgramDay.notes` exists); decide on `Master Planner` / `Filter` / `Tags`.
- **P6 — Prod-only verification.** Manually confirm planner drag/drop (move + Shift-copy); confirm the prod S3 bucket CORS allows browser `PUT` for cover uploads (local dev uses the `local-` dummy-URL bypass).

### Other unwired Client fields
The redesigned `/clients` screen has visual slots for several things the backend doesn't track yet. Decide per-field whether to build the backend or drop the slot:

- **Notes** — coach-authored notes per client. Probably worth a `client_notes` table.
- **Limitations / injuries** — currently piggybacks on `clients.health_conditions` (string list). Promote to its own table if we want dated entries (the design shows a date per entry).
- **Progress photos** — see item 4 above (backend supports per-log photos; needs an all-photos-per-client endpoint).
- **Updates feed** — generic activity log of "client did X / coach did Y" events. Needs an `activity_log` table or a derived view over existing tables.
- **Training stats** (last 7 / 30 days, next week assigned, last workout) — depends on the workout-assignment story landing first.
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