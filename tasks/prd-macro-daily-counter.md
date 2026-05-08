# PRD: Macro Daily Counter with Configurable Goals

## Introduction

Extend daily health tracking to include macronutrient balance. Users can view daily Protein, Carbs, and Fat intake against configurable goals. Main screen adds a Macros tab alongside the existing Calories tab, letting users switch views. A macro detail screen shows daily P/C/F intake vs configurable goals with progress bars and inline editing. Goals are also editable in a dedicated settings screen.

## Goals

- Track daily Protein, Carbs, and Fat intake against user-defined goals
- Display visual progress bars with color-coded feedback (green/orange/red)
- Allow goal configuration both inline in the detail view and in a settings screen
- Provide tabbed navigation on main screen to switch between Calories and Macros
- Persist macro goals across app sessions via existing multiplatform-settings library

## User Stories

### US-001: Add macro goal fields to user settings
**Description:** As a developer, I need to persist user macro goals so they survive app restarts and can be referenced from multiple screens.

**Acceptance Criteria:**
- [ ] Create `UserSettingsDataSource` interface in `data/cache/` with `getProteinGoalG`, `getCarbsGoalG`, `getFatGoalG`, `setProteinGoalG`, `setCarbsGoalG`, `setFatGoalG`
- [ ] Create `UserSettingsDataSourceImpl` using `com.russhwolf.settings.Settings` with keys `macro_goal_protein_g`, `macro_goal_carbs_g`, `macro_goal_fat_g`
- [ ] Defaults: Protein=150g, Carbs=200g, Fat=65g
- [ ] Add `expect fun createUserSettingsDataSource(): UserSettingsDataSource` in commonMain
- [ ] Android actual: `SharedPreferencesSettings` with prefs name `fitbro_settings`
- [ ] iOS actual: `NSUserDefaultsSettings` with `NSUserDefaults.standardUserDefaults`
- [ ] Typecheck passes

### US-002: Create MacroGoalsSettings screen
**Description:** As a user, I want to configure my daily macro targets in a settings screen so I can tune goals to my diet plan.

**Acceptance Criteria:**
- [ ] Composable `MacroGoalsSettings(userSettingsDataSource, modifier)` in `ui/settings/`
- [ ] Three `TextField` inputs labelled "Protein (g)", "Carbs (g)", "Fat (g)"
- [ ] `LaunchedEffect(Unit)` loads current goals into field state on open
- [ ] Save button disabled when any field is empty
- [ ] On save: parse Doubles, skip fields that are ≤ 0, call appropriate setter
- [ ] Typecheck passes

### US-003: Add macro fields to DailyBalance model
**Description:** As a developer, I need DailyBalance to carry P/C/F grams so the UI can display macro intake without re-querying the data source.

**Acceptance Criteria:**
- [ ] Add `proteinG: Double = 0.0`, `carbG: Double = 0.0`, `fatG: Double = 0.0` to `DailyBalance` data class
- [ ] Update `CalorieMathRepositoryImpl.computeDailyBalance` to copy `intake.proteinG`, `intake.carbG`, `intake.fatG` into the returned `DailyBalance`
- [ ] Typecheck passes

### US-004: Create MacroDailyCounterDetail screen
**Description:** As a user, I want to see a full-screen view of my daily macro intake vs goals with progress indicators so I can track my nutrition at a glance.

**Acceptance Criteria:**
- [ ] Composable `MacroDailyCounterDetail(balances, userSettingsDataSource, modifier)` in `ui/dashboard/`
- [ ] Header Surface with back icon, title "Macros", subtitle "Protein, Carbs & Fat", calendar icon
- [ ] Date navigation row (Prev/date label/Next) using `minusDays(1)` / `plusDays(1)` on selectedDate state; Next disabled when `selectedDate == todayString()`
- [ ] Three `MacroCard` composables (Protein, Carbs, Fat) each showing:
  - Label, current intake (Int), goal (Int), unit "g"
  - `LinearProgressIndicator` with progress = `(intake / goal).toFloat().coerceIn(0f, 1f)`
  - Progress color: green `0xFF43A047` if progress ≤ 1.0, orange `0xFFFFA726` if ≤ 1.2, red `0xFFEF5350` above
  - Remaining text: "Remaining: Xg" if intake < goal, "Over by Xg" if over (red text)
  - "Edit Goal" TextButton that opens `GoalEditDialog`
- [ ] `GoalEditDialog`: Dialog with TextField pre-filled with current goal, Cancel + Save buttons; Save calls `userSettingsDataSource.set*GoalG(newGoal)` only when `toDoubleOrNull() != null && value > 0`
- [ ] Summary Box at bottom: "Total Calories from Macros" label, computed as `(proteinG * 4) + (carbG * 4) + (fatG * 9)`
- [ ] Typecheck passes

### US-005: Create MacroSummaryCard
**Description:** As a user, I want to see a macro summary card on the main screen so I can check my P/C/F progress without navigating to the detail screen.

**Acceptance Criteria:**
- [ ] Composable `MacroSummaryCard(balance, userSettingsDataSource, onClick, modifier)` in `ui/dashboard/`
- [ ] Card layout matching existing calorie card: emoji icon box (📊), title "Macros", subtitle "Track your nutritional breakdown", arrow indicator
- [ ] Three `MacroProgressBar` sub-composables for P/C/F showing `label`, `"intake/goalg"` text, and `LinearProgressIndicator` using the same color rules as US-004
- [ ] Renders empty/zero state gracefully when `balance == null`
- [ ] Tapping the card calls `onClick`
- [ ] Typecheck passes

### US-006: Add Calories/Macros tabs to main screen
**Description:** As a user, I want to switch between calorie and macro views on the main dashboard so I can focus on what I'm tracking today.

**Acceptance Criteria:**
- [ ] Create `DashboardWithTabs(stateHolder, balances, userSettingsDataSource, modifier)` composable in `ui/dashboard/`
- [ ] `TabRow` with two `Tab` entries: "Calories" (index 0) and "Macros" (index 1)
- [ ] Tab 0 renders existing `DashboardScreen(stateHolder)`
- [ ] Tab 1 renders `MacroDailyCounterDetail(balances, userSettingsDataSource)`
- [ ] Tab selection held in local `mutableStateOf(0)` — no ViewModel needed
- [ ] Typecheck passes

### US-007: Wire DashboardWithTabs into App entry point
**Description:** As a developer, I need to replace the direct DashboardScreen call in App.kt with DashboardWithTabs so tabs appear in the running app.

**Acceptance Criteria:**
- [ ] `App.kt` instantiates `createUserSettingsDataSource()` via `remember`
- [ ] `App.kt` collects `balances` from `stateHolder.state` (extract from `DashboardUiState.Success`, else empty list)
- [ ] Replace `DashboardScreen(stateHolder)` call with `DashboardWithTabs(stateHolder, balances, userSettingsDataSource)`
- [ ] MacroSummaryCard shown in Macros tab receives today's balance (last element of balances list)
- [ ] Typecheck passes

## Functional Requirements

- FR-1: `UserSettingsDataSource` stores three Double goals (protein/carbs/fat) with platform-specific `Settings` backend
- FR-2: `DailyBalance` carries `proteinG`, `carbG`, `fatG` populated from `DailyIntake` by `CalorieMathRepositoryImpl`
- FR-3: `MacroDailyCounterDetail` navigates by single day; selected day defaults to today
- FR-4: Progress bars color rules: ≤ 100% green, 101–120% orange, > 120% red
- FR-5: Inline goal edit persists immediately to `UserSettingsDataSource` on save
- FR-6: `MacroGoalsSettings` reads current goals on open and saves on explicit button tap
- FR-7: `DashboardWithTabs` wraps existing `DashboardScreen` unchanged — no modifications to that composable
- FR-8: Tab state is local — not stored in ViewModel or DataStore
- FR-9: `MacroSummaryCard` shows today's balance (last item); shows zero-state gracefully if no data

## Non-Goals

- Fiber, sodium, micronutrient tracking
- Macro goal presets (keto, vegan, etc.)
- Meal-level macro breakdown
- Weekly macro averages or trend charts
- Macro-based food suggestions

## Design Considerations

- Progress bar height: 6–8dp, gapSize = 0dp, rounded caps
- MacroCard elevation: 2dp shadow, `RoundedCornerShape(12.dp)`, 16dp padding
- Color palette: Protein = `#1976D2`, Carbs = `#43A047`, Fat = `#FFA726`
- MacroSummaryCard matches existing calorie card shape (`RoundedCornerShape(20.dp)`, 24dp padding)
- GoalEditDialog width: 280dp, Material3 `Dialog` wrapper

## Technical Considerations

- `multiplatform-settings` already declared in `libs.versions.toml` at version 1.3.0 — no new dependency
- Use `expect/actual` pattern matching existing `createCacheDataSource()` pattern in `data/cache/`
- `DailyBalance` fields default to 0.0 — backward compatible with existing serialized cache
- `MacroDailyCounterDetail` receives `List<DailyBalance>` directly; goal reads happen at composition time via `remember { userSettingsDataSource.get*() }` — re-composition picks up inline edits because goal state is derived from remembered mutable state updated in onSave lambda
- `LinearProgressIndicator` progress lambda must return `Float` — always call `.toFloat()` when dividing Doubles

## Success Metrics

- All 7 user stories pass typecheck without errors
- Macro goals survive app kill/reopen
- Inline goal edit updates progress bar in the same screen session without navigation
- Calories tab behaviour unchanged after wrapping in DashboardWithTabs

## Open Questions

- Should the back button in MacroDailyCounterDetail wire to `appState.goBack()` or remain a no-op stub until full navigation is added?
- Should `DashboardWithTabs` eventually replace `App.kt`'s direct screen routing, or remain a standalone composable?
- Should MacroGoalsSettings be accessible from within MacroDailyCounterDetail (settings icon in header) or only from a top-level settings route?
