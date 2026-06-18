# Ralph Agent — FitBro nav-cardio-daterange

You are an autonomous coding agent on the FitBro KMP project. Repo root is the current working directory.

## Each iteration

1. Read `prd.json` (repo root).
2. Read `progress.txt` (repo root) — read the `## Codebase Patterns` section FIRST.
3. Ensure you are on branch `ralph/nav-cardio-daterange`. If not, create it from `main` (`git checkout -b ralph/nav-cardio-daterange`) or check it out if it exists.
4. Pick the HIGHEST priority user story with `passes: false`. Work ONE story only.
5. Implement it following existing code patterns. KMP shared logic in commonMain; platform code only if forced.
6. Quality gate — ALL must pass, no broken commits:
   - `./gradlew :composeApp:compileDebugKotlinAndroid`
   - `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
7. If gates pass: `git add -A` and commit with `feat: [US-XXX] - [Story Title]`.
8. Set `passes: true` for that story in `prd.json`.
9. APPEND a dated block to `progress.txt` (never overwrite): what changed, files touched, and a `Learnings:` list.

## Project specifics (nav-cardio-daterange)
- Full spec: `tasks/prd-nav-cardio-daterange.md`.
- **US-001**: Reorder bottom nav in `DashboardWithTabs.kt` to 0=Balance, 1=Cardio, 2=Macros, 3=Settings. Update both the `items` list order AND the `when(targetIndex)` dispatch block.
- **US-002**: Refactor `CardioStateHolder.kt`: replace hardcoded startDate with `private val _dateRange = MutableStateFlow(DateRange(todayString().minusDays(6), todayString()))`. Derive `val state: StateFlow<CardioState>` via `_dateRange.flatMapLatest { range -> repository.sessionsForRange(range.startDate, range.endDate).map { sessions -> CardioState(sessions, totalMinutes = sessions.sumOf { it.minutes }) } }.stateIn(scope, SharingStarted.WhileSubscribed(5000), CardioState(emptyList(), 0))`. Add `fun setDateRange(dateRange: DateRange) { _dateRange.value = dateRange }`. Rename `weeklyTotalMinutes` → `totalMinutes` in `CardioState` data class and all references. In `DashboardWithTabs`, add `LaunchedEffect` that observes `stateHolder.state.map { it.selectedDateRange }` and calls `cardioStateHolder.setDateRange(it)`.
- **US-003**: In `DashboardWithTabs.kt`, pass `cardioState.totalMinutes` (not `weeklyTotalMinutes`) to `DashboardScreen`. In `DashboardScreen.kt`, change banner `extraContent` label from `"🏃 $weeklyCardioMinutes min this week"` to `"🏃 $weeklyCardioMinutes min"`.

## Rules
- No new dependencies. No browser test. Run Gradle UNSANDBOXED.
- Never commit failing typechecks.

## Stop condition
If ALL stories `passes: true`, reply with exactly:
<promise>COMPLETE</promise>
Otherwise end normally.
