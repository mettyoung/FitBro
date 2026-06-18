# PRD: Nav Reorder + Cardio Date Range Sync

## Introduction

Three related improvements to the dashboard navigation and cardio data scoping:
1. Reorder bottom nav tabs to Balance → Cardio → Macros → Settings
2. Cardio tab shows sessions filtered to the same date range selected in the Balance dashboard
3. Balance banner cardio total reflects the selected date range (not a fixed 7-day window)

## Goals

- Cardio is promoted to second tab for faster access
- Cardio history and totals are always consistent with the date range visible on the Balance dashboard
- Balance banner shows an accurate cardio total for whatever date window the user has selected

## User Stories

### US-001: Reorder bottom nav tabs
**Description:** As a user, I want Balance, Cardio, Macros, Settings tab order so that Cardio is one tap from Balance.

**Acceptance Criteria:**
- [ ] Bottom nav order is: 0=Balance, 1=Cardio, 2=Macros, 3=Settings
- [ ] Icons and labels unchanged; only order changes
- [ ] `when(targetIndex)` block in `DashboardWithTabs.kt` updated to match new indices
- [ ] Typecheck passes: compileDebugKotlinAndroid + compileKotlinIosSimulatorArm64

### US-002: CardioStateHolder reactive to dashboard date range
**Description:** As a user, I want the Cardio tab to show only sessions within the Balance dashboard's selected date range so the two screens stay in sync.

**Acceptance Criteria:**
- [ ] `CardioStateHolder` gains `fun setDateRange(dateRange: DateRange)` that updates an internal `MutableStateFlow<DateRange>`
- [ ] `val state: StateFlow<CardioState>` is derived via `flatMapLatest` on that flow, querying `repository.sessionsForRange(range.startDate, range.endDate)` — removes the hardcoded `today.minusDays(6)` default range
- [ ] Default initial range = `DateRange(todayString().minusDays(6), todayString())` (same 7-day default as before, so first load is unchanged)
- [ ] `DashboardWithTabs` observes `stateHolder.state.map { it.selectedDateRange }` via `LaunchedEffect` and calls `cardioStateHolder.setDateRange(it)` whenever it changes
- [ ] `CardioState.weeklyTotalMinutes` renamed to `totalMinutes` (reflects any range, not just a week); all references updated
- [ ] Typecheck passes: compileDebugKotlinAndroid + compileKotlinIosSimulatorArm64

### US-003: Balance banner shows cardio total for selected date range
**Description:** As a user, I want the Balance banner to show how many cardio minutes I logged within the selected date range, not always "this week".

**Acceptance Criteria:**
- [ ] `weeklyCardioMinutes` param in `DashboardScreen` / `DashboardContent` renamed to `cardioMinutes` (or kept same name — rename optional if it causes churn; value must change)
- [ ] Value passed from `DashboardWithTabs` is `cardioState.totalMinutes` (range-scoped, from US-002)
- [ ] Banner label changes from `"🏃 $n min this week"` to `"🏃 $n min"` in `DashboardScreen.kt` `extraContent` lambda
- [ ] Typecheck passes: compileDebugKotlinAndroid + compileKotlinIosSimulatorArm64

## Functional Requirements

- FR-1: Tab index 0=Balance, 1=Cardio, 2=Macros, 3=Settings in `DashboardWithTabs.kt` items list and `when` block
- FR-2: `CardioStateHolder` internal `_dateRange: MutableStateFlow<DateRange>` drives `state` via `flatMapLatest`
- FR-3: `DashboardWithTabs` calls `cardioStateHolder.setDateRange(selectedDateRange)` in a `LaunchedEffect(selectedDateRange)` block
- FR-4: `CardioState.weeklyTotalMinutes` → `totalMinutes`; `DashboardWithTabs` passes `cardioState.totalMinutes` as the cardio minutes param
- FR-5: Banner label text drops "this week" suffix

## Non-Goals

- No UI changes to the Cardio screen itself (date display, header, etc.)
- No persisting of cardio's own date range separately — it always follows the dashboard range
- No changes to how cardio sessions are stored or logged
- No changes to Macros or Settings tabs

## Technical Considerations

- `CardioStateHolder.kt` line 19-21: replace hardcoded `startDate = today.minusDays(6)` + `val state = repository.sessionsForRange(startDate, today).map { ... }` with `MutableStateFlow<DateRange>` + `flatMapLatest`
- `DashboardWithTabs.kt` line 108-112: `cardioState.weeklyTotalMinutes` → `cardioState.totalMinutes`; add `LaunchedEffect` for date range sync
- `DashboardScreen.kt` `extraContent` lambda: `"🏃 $weeklyCardioMinutes min this week"` → `"🏃 $weeklyCardioMinutes min"`
- `DateRange` is already imported everywhere it's needed; `flatMapLatest` requires `import kotlinx.coroutines.flow.flatMapLatest`

## Success Metrics

- Cardio tab accessible in 1 tap from Balance
- Cardio history matches the date range shown in Balance dashboard
- Balance banner cardio count equals sum of cardio sessions in the selected range
