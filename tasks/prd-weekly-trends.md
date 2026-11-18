# PRD: Weekly Trends with Week-over-Week Comparison

## Introduction

Trends currently shows a free-form date range (start/end pickers, persisted start date, ‹ › arrows that only shift the start date). Replace it with fixed calendar weeks: the user steps week by week, and every week is compared with the week before it. The Cardio tab follows the same week.

## Goals

- One mental model: "this week vs last week". No date pickers on Trends.
- Fair comparison for the current, partial week (like-for-like days).
- Week-over-week (WoW) deltas for the numbers that matter: intake, burn, balance, protein, cardio minutes.
- All aggregation in common code as pure functions, unit-tested.

## Definitions

- **Week start**: user setting, any day of the week; default Monday.
- **Week**: 7 days from the week-start day, local time (e.g. Mon–Sun, or Sun–Sat). Identified by its first date `"YYYY-MM-DD"`.
- **Selected week**: week shown on Trends/Cardio. Defaults to the current week on every app start (not persisted).
- **Elapsed days**: for the current week, Monday..today (1–7); for past weeks, 7.
- **Logged day**: a day with food diary intake > 0.
- **Averaging days**: days that count toward averages and the week-over-week window. Past weeks: all 7. Current week: completed days only (week start..yesterday) — today is still in progress, so it's excluded. On the week's first day there are none ("—").

## User Stories

### US-001: Step through calendar weeks
**Description:** As a user, I want to move week by week instead of picking dates, so trends line up with how I plan.

**Acceptance Criteria:**
- [ ] Trends header shows a week stepper: ‹ "This week" / "Last week" / "Nov 3 – 9" › (label rules in FR-4)
- [ ] ‹ goes to the previous week; › disabled on the current week
- [ ] Tapping the label jumps back to the current week (no-op when already there)
- [ ] Start/end date pickers removed from Trends
- [ ] Chart shows 7 bars in week order (starting at the configured week-start day); future days of the current week shown as empty slots
- [ ] Typecheck passes: compileDebugKotlinAndroid + compileKotlinIosSimulatorArm64

### US-002: Week-over-week comparison
**Description:** As a user, I want to see how this week compares with last week so I know if I'm trending the right way.

**Acceptance Criteria:**
- [ ] New "vs last week" card under the chart with rows: Avg intake, Avg burn, Avg balance, Net balance, Avg protein, Cardio minutes
  - *Revised:* Week-over-week lives only on the **Compare** bottom-nav tab (no entry card on Trends; (shares the Trends week + stepper). Layout follows a health-app home: three rings (Intake, Protein, Cardio; fill = this week ÷ last week, second lap when over), a 3-up stat card ("this /last unit") with the like-for-like caption row, then a 2-column tile grid — one tile per metric with icon badge, value, "was X · delta" and a mini day-by-day tick chart (last week muted, this week coloured; Net balance plots the running total; today/future days have no current tick)
- [ ] Avg carbs and Avg fat rows hidden by default; a "Show carbs & fat" / "Hide carbs & fat" toggle in the card reveals them; choice persists
- [ ] Each row: this week's value, delta vs previous week (▲/▼ + absolute value, e.g. "▲ 120 kcal"), and previous week's value in secondary text
- [ ] Current week compares like-for-like: previous week truncated to the same elapsed days (e.g. Mon–Wed vs Mon–Wed); card caption says so ("Mon–Wed vs Mon–Wed last week")
- [ ] Delta hidden ("—") when either side has no logged days
- [ ] Delta colouring is neutral (no good/bad colours) — direction depends on the user's goal, which FitBro doesn't know
- [ ] Typecheck passes

### US-003: Configurable week start
**Description:** As a user, I want to choose which day my week starts on so weeks match my routine.

**Acceptance Criteria:**
- [ ] Settings → "Week starts on" picker with all 7 days; default Monday
- [ ] Changing it re-aligns the selected week to the week containing the previously selected week's first day, and refreshes Trends and Cardio immediately
- [ ] Persists across restarts

### US-004: Cardio tab follows the selected week
**Description:** As a user, I want the Cardio tab to show the same week as Trends.

**Acceptance Criteria:**
- [ ] Cardio header overline shows the selected week label
- [ ] Summary card shows minutes and sessions for the selected week plus "▲/▼ N min vs last week" (like-for-like for current week)
- [ ] Session list shows only the selected week's sessions

## Functional Requirements

- FR-1: `data class Week(val start: String)` in `data/model/` with `end` (start + 6), `previous()`, `next()`, `contains(date)`, `days(): List<String>`, and `companion fun of(date: String, weekStart: WeekStart): Week` (most recent `weekStart` day on or before `date`). Uses existing `util` date helpers.
- FR-1a: `enum class WeekStart(val label: String) { MONDAY … SUNDAY }` in `data/model/`. `UserSettingsDataSource.getWeekStart()/setWeekStart()`, stored by enum name under `week_start`; missing/unknown → `MONDAY`.
- FR-2: `DashboardStateHolder` replaces `selectedDateRange` with `selectedWeek: Week` (initial = `Week.of(todayString(), weekStart)`), exposes `selectWeek(week)`, `previousWeek()`, `nextWeek()` (no-op past current week), `currentWeek()`, `setWeekStart(weekStart)` (persists, re-aligns per US-003, refreshes).
- FR-3: Each refresh reads **14 days** (previous week Monday .. selected week Sunday, clamped to today) in one pass and splits balances into `current` / `previous` lists. `onBalancesComputed` still receives all computed balances (MCP expenditure publish unaffected, now covers both weeks).
- FR-4: Week label: current week → "This week"; previous → "Last week"; otherwise "MMM d – MMM d" (`toShortDate`); add year suffix when the week isn't in the current year.
- FR-5: Pure `fun summarizeWeek(balances: List<DailyBalance>, days: List<String>, cardioMinutes: Int): WeekSummary` in common. `WeekSummary(loggedDays, avgIntake, avgBurn, avgBalance, netBalance, avgProteinG, avgCarbG, avgFatG, cardioMinutes)`. Averages over **logged days** only; net balance = sum over logged days; `null` averages when `loggedDays == 0`.
- FR-6: Pure `fun compareWeeks(current: WeekSummary, previous: WeekSummary): WeekComparison` with per-metric `delta: Double?` (null when either side has no logged days, or for cardio never null).
- FR-7: Like-for-like (FR-6 inputs): when the selected week is the current week, the previous-week summary uses only its first N days, where N = number of averaging days (completed days, today excluded) — balances and cardio alike. The Cardio tab's running total ("▲ N min vs last week") instead compares through today on both sides (`includeToday = true`), since it's a total, not an average.
- FR-7a: Top average cards (balance, intake, expenditure) also average over averaging days only and show "Completed days only · today excluded" on the current week. The chart still shows today's bar.
- FR-8: `DashboardState` gains `weekStart`, `selectedWeek`, `weekSummary`, `previousWeekSummary`, `showCarbsFat`. Cardio minutes live in `CardioState`, so the UI merges them and calls the pure `compareWeeks` (memoised) rather than coupling the two holders.
- FR-9: `CardioStateHolder.setDateRange` → `setWeek(week)`; reads both weeks (14 days), exposes visible sessions for the selected week, `totalMinutes`, and `previousWeekMinutes` (like-for-like per FR-7). Hidden workout types filter applies to both weeks.
- FR-10: `DashboardWithTabs` drives Cardio from `dashboardState.selectedWeek` (replaces the `selectedDateRange` `LaunchedEffect`). Trends cardio minutes = selected week's visible cardio minutes; WoW cardio delta from FR-9.
- FR-11: Remove `getDashboardStartDate/EndDate` + setters from `UserSettingsDataSource` and the date pickers/handlers from `DashboardContent`. Stored keys left in place (harmless, unread). `DatePickerDialog` stays (still used by Today tab).
- FR-11a: `UserSettingsDataSource.getShowCarbsFatComparison()/set…()`, key `wow_show_carbs_fat`, default `false`.
- FR-12: Chart (`SlidingWindowInsightCard` / `CalorieBalanceChart`) takes `days: List<String>` (always 7, in week order) + the selected week's balances; missing days render as empty slots, not skipped. Day-of-week axis labels follow week order.

## Non-Goals

- No custom date ranges or month view (multi-week trends can come later as "last 8 weeks" weekly averages)
- No previous-week markers on the chart (comparison lives in the WoW card only)
- No good/bad colouring of deltas; no goal direction (cut/bulk) setting
- No change to the Today tab's "Weekly Activity" card (rolling last 7 days)
- No change to MCP tools or sync wire format

## Technical Considerations

- **Losing long ranges**: today you can select e.g. 30 days and see one average. After this change the longest view is one week (plus its comparison). Acceptable per request; the natural follow-up is a multi-week view built on the same `WeekSummary`.
- **Why averages over logged days**: a day with no diary entries has 0 intake and a huge fake deficit. Excluding it keeps averages honest; `loggedDays` is shown ("5 of 7 days logged") so gaps are visible.
- **Like-for-like vs full previous week**: comparing a 3-day current week with a full 7-day previous week skews totals (net balance, cardio minutes). Truncating the previous week fixes totals; averages are mostly unaffected.
- **Fetch cost**: one 14-day Health Connect read per refresh vs a variable range today — usually smaller. Cache (`CacheDataSource`) keys by start/end so it works unchanged.
- **Cardio holder reads 14 days** independently of the dashboard holder (two HC reads). Acceptable; sharing would couple the holders.
- **Delta formatting**: kcal rounded to integer, protein to 1 g, minutes to integer; "▲ 0" shown as "same".

## Testing

- `commonTest`: `Week.of` for Monday and Sunday starts (start day itself, day before, mid-week, year boundary, leap day); `WeekStart` parse fallback, `previous/next/days`; `summarizeWeek` (logged-days averaging, empty week → nulls); `compareWeeks` (deltas, null on empty side); like-for-like truncation (current week Wed → previous Mon–Wed only).
- State holders: logic kept in the pure functions above (no coroutine test lib).

## Success Metrics

- Trends opens on "This week" with a WoW card, no date picking needed
- Stepping back a week is one tap; Cardio tab shows the same week
