# PRD: Cardio Workout Type Filter

## Introduction

The Cardio tab reads every exercise session from Health Connect (Android) / HealthKit (iOS), so strength, yoga and other non-cardio workouts show up and inflate cardio minutes. This feature introduces a shared, platform-neutral `WorkoutType` list, maps each platform's native workout types onto it, and lets the user hide workout types. A hidden type stays hidden for every future workout of that type.

Mapping table: [`workout-type-mapping.md`](workout-type-mapping.md) (source of truth for FR-3/FR-4).

## Goals

- Cardio tab and cardio minutes count only workout types the user wants.
- One shared type list, so hiding a type behaves identically on Android and iOS.
- Sensible defaults on first launch (strength and mind-body hidden); no setup required.
- Extensible: new platform types or new shared types are a mapping-table change, never a schema/settings migration.

## User Stories

### US-001: Shared workout type
**Description:** As a developer, I want every exercise session to carry a platform-neutral `WorkoutType` so filtering and labels live in common code.

**Acceptance Criteria:**
- [ ] `WorkoutType` enum + `WorkoutCategory` enum in `data/model/`
- [ ] `CardioSession` gains `type: WorkoutType`
- [ ] Android and iOS map native types per `workout-type-mapping.md`; unknown codes → `OTHER`
- [ ] Platform label tables (`exerciseTypeLabel`, `workoutLabel`) removed; labels come from `WorkoutType.label`
- [ ] Typecheck passes: compileDebugKotlinAndroid + compileKotlinIosSimulatorArm64

### US-002: Hide a workout type from the Cardio tab
**Description:** As a user, I want to long-press a workout and hide its type so this and all future workouts of that type disappear.

**Acceptance Criteria:**
- [ ] Long-press on a session row opens a menu with "Hide all <type label>"
- [ ] Choosing it hides every session of that type immediately; total minutes and session count update
- [ ] Snackbar "<type label> hidden" with **Undo**
- [ ] Choice persists across app restarts and applies to sessions synced later

### US-003: Manage hidden types in Settings
**Description:** As a user, I want a Settings screen to review and change which workout types count as cardio.

**Acceptance Criteria:**
- [ ] Settings → Cardio → "Workout types" opens a bottom sheet listing every `WorkoutType` grouped by `WorkoutCategory`, each with a switch (on = shown)
- [ ] Toggling updates the Cardio tab without a manual refresh
- [ ] "Reset to defaults" action restores FR-8 defaults

### US-004: Hidden-count hint
**Description:** As a user, I want to know when workouts are hidden so an empty or short list isn't confusing.

**Acceptance Criteria:**
- [ ] When ≥1 session in range is hidden, Cardio tab shows "N workouts hidden · Manage" below the summary card
- [ ] "Manage" opens the same workout-types sheet as US-003, in place (no tab switch)
- [ ] Empty-state copy distinguishes "no workouts" from "all workouts hidden"

## Functional Requirements

- FR-1: `enum class WorkoutCategory { CARDIO, STRENGTH, MIND_BODY, SPORT, OTHER }`
- FR-2: `enum class WorkoutType(val label: String, val category: WorkoutCategory)` — entries exactly as in `workout-type-mapping.md`, including `OTHER`
- FR-3: Android: `fun Int.toWorkoutType(): WorkoutType` in `androidMain` maps `ExerciseSessionRecord.EXERCISE_TYPE_*`; any unmapped value → `OTHER`
- FR-4: iOS: `fun HKWorkoutActivityType.toWorkoutType(): WorkoutType` in `iosMain`; deprecated values (Dance, DanceInspiredTraining, MixedMetabolicCardioTraining) mapped; any unmapped value → `OTHER`
- FR-5: `CardioSession.activity` = record title if non-blank, else `type.label` (unchanged display behaviour, label source moves to common)
- FR-6: `HealthDataSource.readExerciseSessions` keeps returning **all** sessions; filtering happens in common code (FR-9), so platforms stay dumb mappers
- FR-7: `UserSettingsDataSource` gains `getHiddenWorkoutTypes(): Set<WorkoutType>` / `setHiddenWorkoutTypes(Set<WorkoutType>)`. Stored as comma-joined enum **names** under key `hidden_workout_types`; names that no longer parse are ignored (forward/backward compatible)
- FR-8: Key absent → defaults = all types in `STRENGTH` + `MIND_BODY`. Once written (even as empty), the stored value wins
- FR-9: Pure function `fun List<CardioSession>.visible(hidden: Set<WorkoutType>): List<CardioSession>` in common; `CardioStateHolder` applies it before computing `totalMinutes` and the session list
- FR-10: `CardioState` gains `hiddenCount: Int` (sessions in range filtered out) and `hiddenTypes: Set<WorkoutType>`
- FR-11: `CardioStateHolder` exposes `hideType(type)`, `showType(type)`, `resetHiddenTypes()`; each persists via FR-7 and re-filters the last fetched sessions **without** re-reading Health Connect
- FR-12: Trends "cardio minutes" (`DashboardWithTabs` → `cardioState.totalMinutes`) reflects the filter automatically — no extra wiring
- FR-13: Settings screen (US-003) reads/writes through `CardioStateHolder` (single owner of hidden-types state), not the settings data source directly
- FR-14: Calorie burn/expenditure is unaffected (still active calories from Health Connect / HealthKit)

## Non-Goals

- No filtering by source app or by workout title (e.g. hiding only "Padel" inside `OTHER`). Possible later via a raw-code/title key; not built now
- No syncing hidden types to the MCP server / other devices
- No per-session hide (hiding one workout but not its type)
- No calorie-per-session display
- No changes to the legacy `CardioSession` SQL table

## Technical Considerations

- **Why filter in common, not in the HC query:** Health Connect `ReadRecordsRequest` can't filter by exercise type; HealthKit could via predicate but would diverge from Android. Reading all then filtering keeps one code path and lets toggles apply instantly (FR-11).
- **Mapping is lossy by design:** ~58 HC and ~85 HK types collapse to ~53 shared types. Rare leisure types and anything new land in `OTHER`. Mi Fitness likely writes niche sports as `OTHER_WORKOUT` with the name only in the title — hiding `OTHER` hides them all. Verify against real Mi Fitness data after rollout.
- **Platform-only types:** `JUMP_ROPE`, `PICKLEBALL`, `LACROSSE`, `MULTISPORT`, `WATER_SPORTS`, `TRACK_AND_FIELD` only come from HealthKit; `WEIGHTLIFTING` only from Health Connect. Settings still lists them (harmless; keeps one list).
- **Enum names are the persisted contract:** renaming a `WorkoutType` entry silently un-hides it. Rename = add new + map old name in FR-7 parsing, or don't rename.
- **Long-press on `SessionRow`:** `Modifier.combinedClickable(onLongClick = …)` + `DropdownMenu`; snackbar via the existing `Scaffold` in `CardioScreen` (or add `SnackbarHost`).
- **Manage from Cardio tab (US-004):** reuses `WorkoutTypesSheet` (`ui/settings/WorkoutTypesSettings.kt`) in place rather than switching tabs — no cross-tab navigation state needed.

## Testing

- `commonTest`: FR-9 `visible()` (hidden types removed, others untouched, empty set = identity); FR-7/FR-8 parse/serialize (round-trip, unknown names ignored, absent → defaults, empty stored → empty)
- `androidUnitTest`: FR-3 mapping — every `EXERCISE_TYPE_*` constant maps to the row in the mapping table; unknown int → `OTHER`
- iOS mapping (FR-4): compile-checked only; no iOS test target today
- `CardioStateHolder` logic stays thin (no coroutine test lib in the project)

## Open Questions

1. Sports category default: **shown** (spec as written) or hidden?
2. Walking: keep in `CARDIO` (spec as written) or move to `OTHER`?

## Success Metrics

- After install, Cardio tab shows no strength/yoga sessions without any user action
- Hiding a type is ≤2 taps and survives restart
