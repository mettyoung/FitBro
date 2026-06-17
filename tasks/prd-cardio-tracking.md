# PRD: Cardio Tracking

## Introduction

Add a Cardio tab to the dashboard so users can log daily cardio sessions (date + minutes + optional note) and track their weekly total at a glance. No health-platform sync — purely manual logging via SQLDelight.

## Goals

- Log cardio sessions per day with minutes and an optional free-text note
- View per-day breakdown and weekly total minutes on a dedicated Cardio tab
- Edit or delete any logged session

## User Stories

### US-001: Cardio schema + repository
**Description:** As a developer, I need to store cardio sessions in the local database so they persist across app restarts.

**Acceptance Criteria:**
- [ ] New `cardio_session` table: `id INTEGER PRIMARY KEY`, `date TEXT NOT NULL` (YYYY-MM-DD), `minutes INTEGER NOT NULL`, `note TEXT` (nullable)
- [ ] SQLDelight migration file created and applies cleanly on fresh + existing installs
- [ ] `CardioRepository` interface with: `logSession`, `updateSession`, `deleteSession`, `sessionsForWeek(startDate)`, `sessionsForDate(date)`
- [ ] `CardioRepositoryImpl` backed by SQLDelight, all ops run on `Dispatchers.Default`
- [ ] Typecheck passes

### US-002: Cardio tab on dashboard
**Description:** As a user, I want a dedicated Cardio tab on the dashboard so I can access my cardio log without leaving the app.

**Acceptance Criteria:**
- [ ] "Cardio" tab added to `DashboardWithTabs` tab row, after existing tabs
- [ ] Tab shows `CardioScreen` composable
- [ ] Selecting the tab does not affect other tabs' state
- [ ] Typecheck passes

### US-003: Weekly summary header
**Description:** As a user, I want to see my total cardio minutes for the current week at the top of the Cardio tab so I know where I stand.

**Acceptance Criteria:**
- [ ] Header card displays "This week: X min" summed across Mon–Sun of the current week
- [ ] Updates reactively when sessions are added/edited/deleted
- [ ] Shows "0 min" when no sessions logged
- [ ] Typecheck passes

### US-004: Per-day session list
**Description:** As a user, I want to see my cardio sessions grouped by day so I can review what I logged on each day this week.

**Acceptance Criteria:**
- [ ] Sessions for the current week listed below the header, grouped by date (most recent day first)
- [ ] Each row shows: date label (e.g. "Mon Jun 9"), minutes, and note (if present)
- [ ] Days with no sessions are not shown (no empty rows)
- [ ] Tapping a session row opens the edit/delete dialog (US-005)
- [ ] Typecheck passes

### US-005: Log / edit / delete session
**Description:** As a user, I want to log a new cardio session or edit/delete an existing one so I can keep my data accurate.

**Acceptance Criteria:**
- [ ] FAB ("+") on Cardio tab opens a log dialog with: date picker (defaults today), minutes field (numeric, required, > 0), note field (optional free text)
- [ ] Tapping an existing session row opens same dialog pre-filled with that session's data + a "Delete" option
- [ ] Save validates minutes > 0; shows inline error if not
- [ ] Save/delete updates list and weekly total immediately (no manual refresh)
- [ ] Typecheck passes

## Functional Requirements

- FR-1: `cardio_session` table with `id`, `date` (YYYY-MM-DD), `minutes` (integer), `note` (nullable text)
- FR-2: `CardioRepository` exposes `sessionsForRange(startDate: String, endDate: String): Flow<List<CardioSession>>` — caller passes today-6 and today
- FR-3: Dashboard gains a "Cardio" tab; tab wiring in `DashboardWithTabs`
- FR-4: Weekly total = sum of `minutes` for all sessions where `date` is in the rolling 7-day window [today-6 .. today] inclusive
- FR-5: Session list groups by day, sorted descending by date
- FR-6: Log/edit dialog: date picker + integer minutes field (whole numbers only, KeyboardType.Number) + optional note text field
- FR-7: Delete available from the edit dialog only (not swipe)
- FR-8: All state managed via a `CardioStateHolder` (StateFlow pattern, matches existing holders)

## Non-Goals

- No Health Connect / HealthKit sync for cardio
- No cardio history beyond the current week (no calendar navigation)
- No calorie burn estimation from cardio
- No cardio type taxonomy / predefined categories (note field covers ad-hoc labelling)
- No charts or trend graphs

## Design Considerations

- Reuse existing `ModalBottomSheet` pattern for log/edit dialog (matches `CustomFoodManagerSheet` style)
- Weekly header card style consistent with `MacroSummaryCard`
- Session rows consistent with existing diary entry rows
- FAB uses `MiOrange` color (matches app theme)
- Date picker reuse: existing `DatePickerDialog` composable

## Technical Considerations

- New `.sq` file: `CardioSession.sq` under `sqldelight/com/mettyoung/fitbro/data/db/`
- SQLDelight schema version bump + migration `.sqm` file
- `CardioStateHolder` receives `CardioRepository` + `CoroutineScope`; wired in `App.kt`
- Rolling window: compute `today` via `kotlinx.datetime` `Clock.System.todayIn(TimeZone.currentSystemDefault())`; pass `today.minus(6, DateTimeUnit.DAY)` as `startDate`
- `CardioSession` domain model in `data/model/`

## Success Metrics

- User can log a cardio session in under 3 taps
- Weekly total reflects all entries immediately after save
- No regression on existing dashboard tabs

## Open Questions

None — rolling 7-day window, integer minutes confirmed.
