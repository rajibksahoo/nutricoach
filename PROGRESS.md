# NutriCoach — Design Match Plan

> **Plan reset (2026-05-08):** the original 30-day MVP plan is archived at the bottom of this file. The single active goal now is to **match the design produced by Claude Design** for the NutriCoach product surface, screen by screen.
>
> Source of truth: `nutricoach-workout-builder/` design bundle (Workout Builder.html + sibling JSX prototypes + `colors_and_type.css`). Implement in `nutricoach-web` (Next.js + Tailwind). Backend (`nutricoach`) only changes if a screen needs an API that does not yet exist — design parity is not blocked on backend work.

---

## Quick Status

| Area | Status |
|------|--------|
| Backend (auth, clients, plans, billing, progress, AI, WhatsApp) | ✅ Built (no design-driven changes pending) |
| Frontend design tokens (Indigo + Teal, Inter, new radii/shadows) | 🔲 Not started |
| Workout Builder screen | 🔲 Not started |
| Library (Exercises, Workouts, …) | 🟡 Sub-nav matches; content still old palette |
| Clients screen | 🔲 Not redesigned |
| Inbox / Messaging screen | 🔲 Not redesigned |
| Modals (Exercise, Assign, Schedule, Create chooser, Choose template, Workout editor) | 🔲 Not started |
| App shell sidebar (sidebar + section pane pattern) | 🟡 Old colors / icons; structure ok |

**Next task:** lock the new design tokens in `nutricoach-web` (CSS variables, Inter fonts, indigo primary, focus ring) so every redesigned screen pulls from one place. Then implement `Workout Builder.html`.

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
8. **Library screen** (`feat/design-library`) — `library.jsx` + `workouts.jsx`; reconcile with the existing `/library/*` route group (the design shows tabs; the current app uses a section sidebar — pick one and update `DESIGN.md`)
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

- Library section sidebar — icons removed to match design (small step, kept).

---

## Decisions (locked 2026-05-08)

- **Library:** adopt the design's **top tabs** (Fitness / Nutrition / Habits / Forms). Retire the 11-entry section pane.
- **Fonts:** load Inter via `next/font/google`. No self-hosted TTFs. Inter Display / Inter XL fall back to Inter weights — close enough at our scale.
- **Brand migration:** **per-screen**. Each redesign branch migrates its own screen from emerald to indigo. No global flag-day rewrite. `globals.css` keeps both palettes available until the queue is done.

---

## Archived: original 30-day MVP plan

The original 30-day plan that drove backend implementation is preserved at `PROGRESS.archive.md` for historical reference. Backend status as of the reset:

- Week 1 (scaffold + auth + schema): complete
- Week 2 (clients + meal plans + dashboard): backend complete
- Week 3 (progress + billing + WhatsApp): backend complete
- Week 4 (AI + branding + launch): partially complete (Day 22, 24, 26 done; 23 partial; 25, 27, 28, 29, 30 deferred until design parity is reached)

Backend modules and integration tests remain green and are not part of this redesign queue.