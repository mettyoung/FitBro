# PRD: Rolling Date Range with Persisted Start Date

## Introduction

Change the dashboard date range from a fixed 7-day window to a rolling range: the user picks a start date once, it persists across sessions, and the end date is always today. History and chart show all days from start date to today inclusive. Removes the week constraint so users can track any custom time horizon.

## Goals

- Start date persists across app restarts; end date always = today (auto-rolling)
- History list and chart show all days from start date to today — no 7-day cap
- "Total Week" label renamed to "Total" (no longer week-limited)
- No change to the date picker UI — user still opens the calendar to pick a start date

## User Stories

### US-001: Persist start date + rolling end date
**Description:** As a user, I want my chosen start date to be remembered across sessions and the end date to always be today so my view is always up to date.

**Acceptance Criteria:**
- [ ] New key `"dashboard_start_date"` added to `UserSettingsDataSource` interface and `UserSettingsDataSourceImpl`: `getDashboardStartDate(): String` (returns stored value or default = today minus 6 days), `setDashboardStartDate(date: String)`
- [ ] `App.kt`: `initialDateRange` reads `userSettingsDataSource.getDashboardStartDate()` for start; end = `todayString()` always
- [ ] When user picks a date via `DatePickerDialog` in `DashboardContent`, only `range.startDate` is used; end date is always recomputed as `todayString()` — the picked end date from the dialog is ignored
- [ ] After user picks a new start date, `userSettingsDataSource.setDashboardStartDate(range.startDate)` persists it immediately (called from `DashboardContent` `onDateRangeSelected` handler or `DashboardStateHolder.setDateRange`)
- [ ] On next cold start, the persisted start date is restored and range = startDate..today
- [ ] Typecheck passes: compileDebugKotlinAndroid + compileKotlinIosSimulatorArm64

### US-002: Remove 7-day cap + rename "Total Week" → "Total"
**Description:** As a user, I want the chart and history to show all days from my start date to today, not just the last 7.

**Acceptance Criteria:**
- [ ] `CalorieBalanceChart.kt:70` — remove `balances.takeLast(7)`; chart renders all balances passed to it (already normalized per-max so adding more bars works correctly)
- [ ] `DashboardScreen.kt:523` — "Total Week" renamed to "Total"
- [ ] History list (already uses `balances.reversed()` over the full list) — verify no other `takeLast` or size cap exists in history rendering; remove if found
- [ ] `SlidingWindowInsightCard` window metrics (`calculateWindowMetrics`) already operates over all passed balances — no change needed there
- [ ] Typecheck passes: compileDebugKotlinAndroid + compileKotlinIosSimulatorArm64

## Functional Requirements

- FR-1: `UserSettingsDataSource.getDashboardStartDate()` / `setDashboardStartDate()` — default = `todayString().minusDays(6)` when no persisted value
- FR-2: `App.kt` `initialDateRange` = `DateRange(startDate = getDashboardStartDate(), endDate = todayString())`
- FR-3: `onDateRangeSelected` in `DashboardContent` (or `setDateRange` in StateHolder) always sets `endDate = todayString()`; persists `startDate` via `userSettingsDataSource.setDashboardStartDate()`
- FR-4: `CalorieBalanceChart` renders all entries (no `takeLast` cap)
- FR-5: Label "Total Week" → "Total" in `SlidingWindowInsightCard` footer

## Non-Goals

- No UI change to the date picker dialog itself
- No end-date picker — end is always today, not user-configurable
- No migration of previously selected date range (cold start defaults to today-6 if no stored value)
- No maximum range cap (user can pick any past date)

## Technical Considerations

- `UserSettingsDataSourceImpl` add two methods alongside existing macro goal keys
- `DashboardStateHolder.setDateRange(range)` currently just sets `_state.update { it.copy(...) }` — add `userSettingsDataSource.setDashboardStartDate(range.startDate)` call here (requires passing `userSettingsDataSource` into `DashboardStateHolder`, which already receives it or can receive it)
- Alternatively persist from `App.kt` via a `LaunchedEffect` watching `selectedDateRange` — simpler if StateHolder doesn't already hold the settings source
- Chart bar width: with many bars the chart may get crowded — acceptable for now (no-scroll chart, bars just get thinner)

## Success Metrics

- App restart restores exactly the same start date user previously set
- Chart + history show all days from start to today with no artificial cut-off
- "Total Week" → "Total" everywhere it appears
