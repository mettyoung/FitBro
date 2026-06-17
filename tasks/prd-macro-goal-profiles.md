# PRD: Per-Day Macro Goal Profiles

## Introduction

Replace the single global macro goal with named goal profiles (e.g. "Training Day", "Rest Day", "Refeed"). Users create as many profiles as they need, then assign each day of the week to a profile. The dashboard reads today's weekday, finds the mapped profile, and uses those targets for macro progress tracking — automatically, without any daily manual action.

## Goals

- Let users create unlimited named macro goal profiles with independent P/C/F/kcal targets
- Let users assign any profile to any day of the week via a simple 7-row dropdown in Settings
- Auto-migrate the existing single global goal into a profile named "Default" so existing data is preserved
- Dashboard `MacroSummaryCard` and macro counters always reflect the active day's assigned profile
- If a day has no assignment, fall back to the "Default" profile

## User Stories

### US-001: MacroGoalProfile schema + repository
**Description:** As a developer, I need to persist named macro goal profiles and weekday mappings in the local database so they survive app restarts.

**Acceptance Criteria:**
- [ ] New `macro_goal_profile` table: `id INTEGER PRIMARY KEY AUTOINCREMENT`, `name TEXT NOT NULL`, `protein_g REAL NOT NULL`, `carbs_g REAL NOT NULL`, `fat_g REAL NOT NULL`, `calories_kcal REAL NOT NULL`
- [ ] New `weekday_goal_mapping` table: `weekday INTEGER PRIMARY KEY` (0=Mon … 6=Sun), `profile_id INTEGER NOT NULL REFERENCES macro_goal_profile(id)`
- [ ] Migration 6.sqm applies both CREATE TABLEs on existing installs
- [ ] `MacroGoalProfile` domain model in `data/model/`
- [ ] `MacroGoalRepository` interface: `getAllProfiles(): Flow<List<MacroGoalProfile>>`, `addProfile(name, protein, carbs, fat, calories)`, `updateProfile(id, name, protein, carbs, fat, calories)`, `deleteProfile(id)`, `getMappingForWeekday(weekday): MacroGoalProfile?`, `setMappingForWeekday(weekday, profileId)`, `getActiveProfileForDate(date: String): MacroGoalProfile`
- [ ] `MacroGoalRepositoryImpl` backed by SQLDelight; all ops on `Dispatchers.Default`
- [ ] Typecheck passes: compileDebugKotlinAndroid + compileKotlinIosSimulatorArm64

### US-002: Seed migration — existing goal → "Default" profile
**Description:** As a user upgrading the app, I want my existing macro goal preserved so I don't lose my current targets.

**Acceptance Criteria:**
- [ ] On first run after migration, `MacroGoalRepositoryImpl` seeds a "Default" profile using the values from `UserSettingsDataSource` (protein, carbs, fat, calories)
- [ ] Seeding is idempotent: runs once (guard: profile table non-empty → skip)
- [ ] All 7 weekday mappings default to the "Default" profile if no row exists (handled in `getActiveProfileForDate` fallback)
- [ ] `UserSettingsDataSource` macro goal getters/setters remain untouched (legacy reads still compile); no data is deleted
- [ ] Typecheck passes

### US-003: MacroProfilesStateHolder + Settings screen — profile list
**Description:** As a user, I want to see all my macro goal profiles in Settings so I can manage them in one place.

**Acceptance Criteria:**
- [ ] `MacroProfilesStateHolder` in `ui/settings/` receives `MacroGoalRepository` + `CoroutineScope`; exposes `StateFlow<MacroProfilesState>` with `profiles: List<MacroGoalProfile>`, `weekdayMappings: Map<Int, MacroGoalProfile>`
- [ ] New Settings section "Macro Profiles" renders a `LazyColumn` of profile cards; each card shows profile name + P/C/F/kcal summary line
- [ ] FAB or "＋ Add Profile" button at bottom opens the add-profile dialog (US-004)
- [ ] Tapping a profile card opens edit/delete dialog (US-004)
- [ ] `MacroProfilesStateHolder` wired in `App.kt` via `remember {}`; passed into Settings composable
- [ ] Typecheck passes

### US-004: Add / edit / delete profile dialog
**Description:** As a user, I want to create, rename, and delete macro goal profiles so I can maintain exactly the set I need.

**Acceptance Criteria:**
- [ ] Dialog (ModalBottomSheet, same style as `CardioLogSheet`) has: name `OutlinedTextField`, protein / carbs / fat `OutlinedTextField` (KeyboardType.Number), calories read-only field auto-calculated via `MacroMath.caloriesFromMacros()` and displayed as a summary (same pattern as existing `MacroGoalsDialog`)
- [ ] Save validates: name non-blank, protein ≥ 0, carbs ≥ 0, fat ≥ 0; shows inline error otherwise
- [ ] Edit mode: pre-fills fields; shows "Delete" TextButton (destructive color)
- [ ] Delete is blocked with a SnackBar message if the profile is currently assigned to any weekday ("Unassign from all days first")
- [ ] Save/delete updates `StateFlow` immediately; no manual refresh
- [ ] Typecheck passes

### US-005: Weekday-to-profile assignment UI
**Description:** As a user, I want to assign a profile to each day of the week so the app knows which targets to use automatically.

**Acceptance Criteria:**
- [ ] Below the profile list, a "Weekly Schedule" section shows 7 rows: Mon, Tue, Wed, Thu, Fri, Sat, Sun
- [ ] Each row has the day name on the left and a dropdown (ExposedDropdownMenuBox) on the right listing all profile names
- [ ] Currently assigned profile shown as selected in the dropdown
- [ ] Selecting a different profile calls `stateHolder.setMapping(weekday, profileId)` and updates immediately
- [ ] Days with no explicit DB row show "Default" selected (matches fallback logic in `getActiveProfileForDate`)
- [ ] Typecheck passes

### US-006: Dashboard reads active profile
**Description:** As a user, I want the dashboard macro counters and summary card to automatically reflect the goal profile assigned to today's day of the week so I don't need to switch anything manually.

**Acceptance Criteria:**
- [ ] `MacroGoalRepository.getActiveProfileForDate(today)` used wherever the current macro goal is read: `MacroSummaryCard`, `MacroDailyCounterDetail`
- [ ] `MacroSummaryCard` shows a secondary subtitle with the active profile name (e.g. "Training Day") in `MiTextSecondary` color below the macro progress bars
- [ ] Both `MacroSummaryCard` and `MacroDailyCounterDetail` receive `MacroGoalRepository` (or goals as params) instead of reading directly from `UserSettingsDataSource`
- [ ] Changing the day (date navigator) re-evaluates the profile for that date's weekday
- [ ] Typecheck passes

## Functional Requirements

- FR-1: `macro_goal_profile` table — id, name, protein_g, carbs_g, fat_g, calories_kcal
- FR-2: `weekday_goal_mapping` table — weekday (0=Mon … 6=Sun) PK, profile_id FK
- FR-3: Migration 6.sqm creates both tables; seed logic inserts "Default" from `UserSettingsDataSource` values on first launch if profile table is empty
- FR-4: `MacroGoalRepository.getActiveProfileForDate(date)` — compute weekday via `dayOfWeekMonBased`, look up mapping, fall back to any profile named "Default" if no mapping row
- FR-5: Profile list in Settings section "Macro Profiles"; profiles are ordered by insertion (id ASC)
- FR-6: Add/edit dialog auto-calculates calories using `MacroMath.caloriesFromMacros(protein, carbs, fat)` — same as existing `MacroGoalsDialog`
- FR-7: Delete blocked if profile assigned to any weekday row in `weekday_goal_mapping`
- FR-8: Weekly Schedule — 7 `ExposedDropdownMenuBox` rows (Mon–Sun); each shows current assignment and allows re-assignment in one tap
- FR-9: `MacroSummaryCard` shows active profile name as subtitle
- FR-10: `MacroDailyCounterDetail` uses active profile's targets for progress ring / gram display
- FR-11: Unlimited profiles; no cap

## Non-Goals

- No time-of-day goal variation (only per weekday, not per meal or hour)
- No calories-only goal (all profiles require P/C/F — calories are derived)
- No profile sharing or export
- No automatic profile suggestion based on activity data
- No per-date override (only weekday-based; specific date exceptions are out of scope)
- Do NOT remove or break `UserSettingsDataSource` macro goal methods (legacy compat)
- No chart or historical view of which profile was active on past days

## Design Considerations

- "Macro Profiles" section appears in the existing Settings screen, after the current macro goal section (which can be removed or repurposed once US-006 is done)
- Profile card: `ElevatedCard`, title = profile name (titleMedium), subtitle = "P: Xg · C: Xg · F: Xg · Kcal kcal" (bodySmall, MiTextSecondary)
- Weekly Schedule section header: same style as other Settings section headers
- Dialog style: ModalBottomSheet matching `CardioLogSheet` (RoundedCornerShape topStart/topEnd 32.dp, `containerColor = surface`)
- Active profile name in `MacroSummaryCard`: bodySmall, MiTextSecondary, centered below macro bars
- FAB: MiOrange, Icons.Default.Add

## Technical Considerations

- Schema version bumps from 5→6: migration file is `6.sqm`
- `dayOfWeekMonBased()` already in `DateUtil.kt` (0=Mon…6=Sun) — reuse directly
- `MacroMath.caloriesFromMacros()` already exists in `util/MacroMath.kt` — reuse in dialog
- `MacroGoalRepositoryImpl` needs the DB + `UserSettingsDataSource` injected (for seed migration read)
- Wiring in `App.kt`: `MacroGoalRepositoryImpl(database, userSettingsDataSource)` via `remember {}`
- `MacroDailyCounterDetail` currently reads goals via direct `UserSettingsDataSource` calls in `LaunchedEffect` — replace with `MacroGoalRepository.getActiveProfileForDate(selectedDate)`
- `MacroSummaryCard` currently reads goals via direct `UserSettingsDataSource` calls — replace similarly

## Success Metrics

- User can create a new profile and assign it to a weekday in under 5 taps
- Dashboard goal targets update automatically when the date changes to a different weekday
- No regression on existing macro progress display for users who never add a second profile

## Open Questions

- Should `MacroSummaryCard` profile name be tappable (navigates to that profile's edit dialog)? Defer to post-MVP.

## Decisions

- **Retire old editors**: `MacroGoalsSettings.kt` and `MacroGoalsDialog` (in `MacroDailyCounterDetail.kt`) are removed and replaced entirely by the new profile system in US-003/US-004. The "Macro Goals" Settings section is replaced by "Macro Profiles". The dashboard dialog edit-goal button is removed or repurposed to open the Macro Profiles Settings section.
