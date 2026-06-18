# PRD: Cardio Page Polish

## Introduction

Two small UX improvements to the Cardio feature:
1. Add a consistent screen header to the Cardio tab (title + subtitle) matching the pattern used in Settings and Food Diary.
2. Surface the weekly cardio total on the main dashboard Balance tab so users see their cardio activity without switching tabs.

## Goals

- Cardio tab looks visually consistent with the rest of the app (no abrupt "just a card" start)
- Weekly cardio minutes visible at a glance from the main dashboard
- Zero new dependencies; reuse existing components and data already in memory

## User Stories

### US-001: Cardio screen consistent header
**Description:** As a user, I want the Cardio tab to have a header like other screens so the app feels cohesive.

**Acceptance Criteria:**
- [ ] Header at the top of `CardioScreen` with title "Cardio" (`displayMedium` or `headlineMedium`) and subtitle "Weekly Training Log" (`labelSmall`, `MiOrange`)
- [ ] Header uses `statusBarsPadding()` + `background(MaterialTheme.colorScheme.surface)` + `horizontal padding 24.dp` — same as `MacroProfilesSettings` header (lines 83–99)
- [ ] Header is outside the `LazyColumn` / scrollable area so it stays pinned at top; sessions list scrolls beneath it
- [ ] `WeeklySummaryCard` ("This week: X min") moves below the header, inside the scrollable content — unchanged otherwise
- [ ] Typecheck passes: compileDebugKotlinAndroid + compileKotlinIosSimulatorArm64

### US-002: Weekly cardio total on main dashboard
**Description:** As a user, I want to see my weekly cardio total on the main dashboard so I don't need to switch to the Cardio tab to check it.

**Acceptance Criteria:**
- [ ] A compact `CardioSummaryRow` composable rendered in `DashboardContent` between `SlidingWindowInsightCard` and the "History" section header
- [ ] Displays: a run icon (`Icons.AutoMirrored.Filled.DirectionsRun`, `MiOrange`), label "Cardio this week", and the total minutes value (e.g. "120 min") right-aligned; shows "0 min" when no sessions
- [ ] `weeklyTotalMinutes` sourced from `CardioStateHolder.state` — `CardioStateHolder` is already instantiated in `App.kt`; pass `weeklyTotalMinutes: Int` as a parameter down to `DashboardContent` (or pass the value directly from `App.kt`)
- [ ] Row uses same horizontal padding (24.dp) and `MiTextSecondary` for the label; value in `bodyLarge`, `onSurface`
- [ ] Only shown on the BALANCE view mode (hidden when INTAKE or EXPENDITURE toggle is active)
- [ ] Typecheck passes: compileDebugKotlinAndroid + compileKotlinIosSimulatorArm64

## Functional Requirements

- FR-1: `CardioScreen` header: title "Cardio", subtitle "Weekly Training Log", pinned above scroll, `statusBarsPadding` + surface background + 24.dp horizontal padding
- FR-2: `DashboardContent` accepts a new `weeklyCardioMinutes: Int` parameter (default 0 for backward compat)
- FR-3: `App.kt` passes `cardioStateHolder.state.collectAsState().value.weeklyTotalMinutes` into `DashboardContent`
- FR-4: `CardioSummaryRow` is a private composable in `DashboardScreen.kt`; not a new file
- FR-5: Row hidden (not just 0-padded) when `viewMode != DashboardViewMode.BALANCE`

## Non-Goals

- No chart or trend for cardio minutes on the dashboard
- No tapping the dashboard row to navigate to Cardio tab
- No cardio goal / target minutes feature
- No changes to `CardioStateHolder`, `CardioRepository`, or `CardioState`

## Technical Considerations

- `CardioScreen.kt` currently wraps content in a `Scaffold` with FAB; the header goes above the `Scaffold` or inside it as a `topBar` slot using a plain `Column` (not `TopAppBar` — just a styled `Box`/`Column` to match existing pattern)
- `DashboardContent` signature change: add `weeklyCardioMinutes: Int = 0` — all existing call sites compile without change
- `CardioStateHolder` already available in `App.kt`; no new wiring needed beyond passing the value

## Success Metrics

- Cardio tab header visually indistinguishable in style from Settings and Food Diary headers
- Weekly cardio total visible on dashboard without any tap
