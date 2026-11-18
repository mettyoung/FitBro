# PRD: Dashboard (replaces Trends) + Body-Composition Trends

## Introduction

Trends becomes **Dashboard**, the first and default tab. It answers three weekly questions at a glance — *am I in the deficit I want, did I lift enough, did I do enough cardio* — then shows average intake/expenditure and longer-horizon body-composition trends (weight, skeletal muscle, body fat, estimated expenditure). Navigation shrinks to four tabs: **Dashboard / Food log / Cardio / More** (More → Compare weeks, Settings).

Mockup agreed in chat (v3). Layout reference: MacroFactor dashboard + Weight Trend detail.

## Decisions taken (defaults — override before implementation if wrong)

| # | Question | Default chosen |
|---|----------|----------------|
| D1 | Skeletal muscle source | Neither Health Connect nor HealthKit has a skeletal-muscle type. Use **lean body mass** (`LeanBodyMassRecord` / `HKQuantityTypeIdentifierLeanBodyMass`) and label the tile **"Lean body mass"**. Readings only — never derived from weight × body fat %. Manual skeletal-muscle entry is a non-goal for v1. |
| D2 | Estimated expenditure | Energy-balance estimate from intake + weight-trend change (FR-14), distinct from Average expenditure (Health Connect burn). |
| D3 | Trend window | Body-composition tiles + estimated expenditure use the **28 days ending on the selected week's last completed day**; intake/expenditure/balance stay calendar-week. |
| D4 | Balance goal | None in v1 (number + daily bars only). |
| D5 | Today in week-to-date balance | Excluded — completed days only (`Week.averagingDays`), same as averages. |
| D6 | Resistance definition | Sessions whose `WorkoutType.category == STRENGTH`, counted regardless of the Cardio tab's hidden-types filter. HIIT stays cardio. |
| D7 | Cardio minutes definition | Same sessions/minutes as the Cardio tab (visible types only, through today). |
| D8 | Where targets are set | Settings → "Weekly goals". Defaults: 3 resistance sessions, 150 cardio minutes. |
| D9 | Week stepper on Dashboard | Kept (shared with Compare and Cardio). |

## Goals

- Dashboard is the first thing seen on launch; weekly progress readable in under 3 seconds.
- Weekly targets for resistance sessions and cardio minutes, with progress.
- Body-composition trends smoothed so daily scale noise doesn't drive decisions.
- All aggregation (trend smoothing, estimated expenditure, goal progress) in common code as pure, unit-tested functions.

## Definitions

- **Selected week / averaging days / logged day**: as in `prd-weekly-trends.md`.
- **Weigh-in**: a body measurement from Health Connect / HealthKit on a given date (weight, body fat %, lean mass — any subset). Multiple per day → use the day's last reading.
- **Trend value**: exponentially smoothed series, `trend_d = trend_{d-1} + α · (scale_d − trend_{d-1})`, α = 0.1, seeded with the first weigh-in; days without a weigh-in carry the previous trend forward.
- **Trend window**: the 28 days ending on the selected week's last averaging day (current week: yesterday; past week: its last day). Reads extend 28 extra days back as EMA warm-up.
- **Body fat (kg)**: `weightKg × bodyFatPct / 100` on days with both readings, then smoothed.

## User Stories

### US-001: Dashboard is the first tab
**Description:** As a user, I want to land on my weekly dashboard, not the food log.

**Acceptance Criteria:**
- [ ] Bottom nav: Dashboard (default on launch) / Food log / Cardio / More
- [ ] "Today" tab renamed "Food log" (same screen)
- [ ] More opens a list page: "Compare weeks", "Settings"; each opens full-screen with a back arrow (system back works)
- [ ] Compare keeps sharing the Dashboard week
- [ ] Typecheck passes: compileDebugKotlinAndroid + compileKotlinIosSimulatorArm64

### US-002: Week-to-date calorie balance
**Description:** As a user, I want to see my cumulative deficit/surplus so far this week.

**Acceptance Criteria:**
- [ ] Card at top: "Week-to-date balance", signed total kcal (deficit = negative, `NegativeColor`/`PositiveColor` as elsewhere)
- [ ] Caption: span + avg, e.g. "Mon–Tue · avg −620/day"; past weeks "Full week · avg …"; first day of current week "No completed days yet"
- [ ] 7 daily balance bars diverging around zero; future days empty, today a dashed placeholder
- [ ] Tap → existing Balance detail page

### US-003: Resistance training vs weekly target
**Description:** As a user, I want to see how many resistance sessions I've done this week against my target.

**Acceptance Criteria:**
- [ ] Card: "Resistance", "2 / 3", one segment per target session (filled = done); count above target shows "4 / 3" with all segments filled + "+1"
- [ ] Counts STRENGTH-category sessions in the selected week, including today, regardless of hidden workout types
- [ ] Target editable in Settings → Weekly goals (1–14, default 3)

### US-004: Cardio minutes vs weekly target
**Acceptance Criteria:**
- [ ] Card: "Cardio", "95 / 150 min", progress bar (caps visually at 100%, text shows real value)
- [ ] Minutes = Cardio tab's visible sessions in the selected week, through today
- [ ] Target editable in Settings → Weekly goals (10–2000 min, step 10, default 150)
- [ ] Tap → Cardio tab

### US-005: Average intake and average expenditure
**Acceptance Criteria:**
- [ ] Two tiles side by side under Resistance/Cardio: "Average intake" (subtitle "goal X kcal") and "Average expenditure" (subtitle "Health Connect burn"), each with 7-day mini bars
- [ ] Same averaging rules as today (logged, completed days; "—" if none)
- [ ] Tap Average intake → Intake detail; history rows show kcal **and P / C / F grams** per day
- [ ] Tap Average expenditure → Expenditure detail
- [ ] Average protein / carbs / fat tiles removed from Dashboard

### US-006: Body-composition trends
**Description:** As a user, I want smoothed weight, lean mass and body fat trends so I can see real change through scale noise.

**Acceptance Criteria:**
- [ ] Insights section with tiles: Weight trend, Lean mass trend, Body fat trend (kg)
- [ ] Each tile: latest trend value in the window, "▲/▼ X kg vs 4 wks ago" (trend at window end − trend at window start), sparkline of the trend (weight tile also shows faint scale line)
- [ ] No data in window → value "—", subtitle "No weigh-ins" (lean/body fat: "Not synced from your scale")
- [ ] Tap → trend detail page: header "Average" (mean trend over window) + "Difference", chart with scale dots + trend line, legend "Scale / Trend", list of weigh-ins newest first

### US-007: Estimated expenditure
**Description:** As a user, I want an expenditure estimate based on what I ate and how my weight actually changed.

**Acceptance Criteria:**
- [ ] Insights tile "Estimated expenditure": kcal/day, subtitle "intake + weight trend", sparkline of rolling estimate
- [ ] Shows "—" + "Needs 14 logged days and 2 weigh-ins" when FR-14 preconditions fail
- [ ] Tap → detail page: estimate, vs Average expenditure (Health Connect), avg intake and trend change used, short "How it's calculated" line

### US-008: Health permissions for body composition
**Acceptance Criteria:**
- [ ] Android: read permissions for `WeightRecord`, `BodyFatRecord`, `LeanBodyMassRecord` added to the permission gate set
- [ ] iOS: HealthKit read types `bodyMass`, `bodyFatPercentage`, `leanBodyMass`
- [ ] Denied/missing permission → tiles show "—" with "Allow body data in Health Connect"; rest of Dashboard unaffected
- [ ] Body permissions are optional: requested with the required set on first grant, and once (remembered) for existing installs; never block the app

## Functional Requirements

- FR-1: `AppTab` = DASHBOARD, FOOD_LOG, CARDIO, MORE; default DASHBOARD. More is a list page; Compare and Settings are sub-pages of More with back navigation (BackHandler).
- FR-2: Dashboard layout order: header + week stepper → week-to-date balance → [Resistance | Cardio] → [Average intake | Average expenditure] → "Insights" → [Estimated expenditure | Weight trend] → [Lean mass trend | Body fat trend] → warnings → sync status.
- FR-3: Week-to-date balance = sum of `balance` over logged days in `Week.averagingDays(today)`; average = that sum ÷ logged-day count.
- FR-4: Resistance count = number of sessions with `type.category == STRENGTH` dated in the selected week (all 7 days, today included), read from the unfiltered session list.
- FR-5: Cardio minutes = `CardioState.totalMinutes` for the selected week.
- FR-6: New settings in `UserSettingsDataSource`: `weekly_resistance_target` (Int, default 3), `weekly_cardio_minutes_target` (Int, default 150). Settings → "Weekly goals" section with two steppers.
- FR-7: `HealthDataSource.readBodyMeasurements(start, end): HealthResult<List<BodyMeasurement>>`; `BodyMeasurement(date, weightKg?, bodyFatPct?, leanMassKg?)`, one per date (last reading of the day per field). Android: Health Connect records above; iOS: HealthKit quantity samples.
- FR-8: Body measurements cached like activity burn (fallback to cache on Health failure).
- FR-9: `trendSeries(points: List<Pair<date, value>>, days: List<String>, alpha = 0.1): List<Double?>` — pure; null before the first weigh-in; carries forward on gaps.
- FR-10: Read range for trends = trend window start − 28 days … window end.
- FR-11: Tile value = trend at window end; delta = trend at window end − trend at window start (null if either null).
- FR-12: Body fat kg series derived per FR definition before smoothing.
- FR-13: Trend detail "Average" = mean of non-null trend values in the window; "Difference" = FR-11 delta.
- FR-14: Estimated expenditure over the trend window: `EE = avgIntake − (Δtrend_kg × 7700) / spanDays`, where avgIntake is over logged days in the window, Δtrend = trend weight at the window's last weigh-in − trend at its first weigh-in, spanDays = days between those weigh-ins. Requires ≥ 14 logged days and ≥ 2 weigh-ins ≥ 14 days apart; else null.
- FR-15: Estimated-expenditure sparkline = FR-14 evaluated at each day of the selected week (rolling 28-day window ending that day); null where preconditions fail.
- FR-16: Intake detail history rows include protein / carbs / fat grams.
- FR-16a: Intake / Expenditure detail layout (health-app style): big value (completed-day average; tapped bar's day value when selected), 7-day bar chart with 0 / mid / max grid + right axis (tap selects, others dim), "Weekly summary" card (Highest · day, Lowest · day, Average, days logged — completed days only), then History. Week-to-date balance bars show a compact value label at the bar end; tapping a bar opens the daily breakdown.
- FR-17: All new tiles reuse `MetricTileCard` / `MiniTicks` / `TwoColumnTiles`; a new `MiniLine` sparkline component for trends. Theme tokens only (no raw colors in screens).
- FR-18: The uncommitted Trends health-app layout (rings + 3-up stat card + 8 metric tiles) is replaced by this layout. Compare tab layout unchanged.

## Non-Goals

- Range chips (1W / 1M / 3M / 6M / 1Y / All) on detail pages — fixed 28-day window in v1.
- Manual weight / skeletal-muscle / body-fat entry; writing body measurements back to Health Connect.
- True skeletal muscle mass (no platform source).
- Weekly calorie-balance goal.
- Adaptive calorie targets / coaching from estimated expenditure.
- Weekly Nutrition grid (reference screenshot) — explicitly not wanted.

## Technical Considerations

- `CardioStateHolder` exposes the unfiltered week sessions (or a `resistanceCount`) so FR-4 ignores hidden types.
- `DashboardStateHolder.refresh()` gains a parallel `readBodyMeasurements` call (`async`), plus settings for targets; new state: `wtdBalance`, `resistanceCount`, targets, `bodyTrends` (per metric: series, value, delta, average), `estimatedExpenditure`.
- Pure functions live in `data/model/BodyTrend.kt` (`trendSeries`, `estimateExpenditure`, `bodyFatKg`) beside `Week.kt`.
- Health Connect `LeanBodyMassRecord`/`BodyFatRecord` may be empty if Mi Fitness doesn't sync them — verify on device before polishing those tiles.
- `AppTab` rename changes `rememberSaveable` ordinals — acceptable (state is per-process).
- iOS HealthKit: request new read types in the existing authorization call.

## Testing

- `BodyTrendTest` (commonTest): EMA seeding, gap carry-forward, null-before-first-weigh-in, delta/average, body-fat kg derivation, estimated expenditure (happy path, < 14 logged days → null, single weigh-in → null, weight gain → EE below intake).
- `WeekTest` additions: week-to-date balance sum/avg incl. first-day and past-week cases; resistance count ignores hidden filter.
- Manual on device: Mi Fitness → Health Connect weight / body fat / lean mass present? Tiles show "—" gracefully when absent.

## Open Questions

- Does Mi Fitness write `BodyFatRecord` and `LeanBodyMassRecord` to Health Connect, or only `WeightRecord`? (Check on device after FR-7.)
- If lean mass is absent, keep the tile as "—" or hide it?
