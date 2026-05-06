# PRD: Health Connect Full Integration

## Introduction

Replace the fictional Cronometer OAuth integration with a real Health Connect (Android) + HealthKit (iOS) pipeline. Cronometer does not have a public API — this feature wires all calorie data (intake, BMR, NEAT) through the platform health data layer, which Cronometer, Mi Fit, Samsung Health, and other apps already write to.

The dashboard currently renders hardcoded sample data with no-op callbacks. After this feature, it will display real data from the user's Health Connect / HealthKit store, end-to-end.

---

## Goals

- Replace `CronometerDataSource` with Health Connect / HealthKit reads for calorie intake and BMR
- Wire `App.kt` to instantiate real data sources and `DashboardStateHolder`
- Show a permission gate before the dashboard on Android (Health Connect requires explicit grants)
- TEF auto-estimated as 10% of daily intake (no Health Connect record type exists for it)
- No external API keys, no OAuth — everything local to the device

---

## Technical Context (read before implementing)

**Existing code to reuse:**
- `HealthDataSource` interface: `composeApp/src/commonMain/kotlin/com/mettyoung/fitbro/data/health/HealthDataSource.kt`
- Android HC impl: `composeApp/src/androidMain/kotlin/com/mettyoung/fitbro/data/health/HealthDataSource.android.kt`
- iOS HK impl: `composeApp/src/iosMain/kotlin/com/mettyoung/fitbro/data/health/HealthDataSource.ios.kt`
- `DashboardStateHolder`: `composeApp/src/commonMain/kotlin/com/mettyoung/fitbro/ui/dashboard/DashboardStateHolder.kt`
- `App.kt`: `composeApp/src/commonMain/kotlin/com/mettyoung/fitbro/App.kt` — currently renders hardcoded sample data
- `MainActivity.kt`: `composeApp/src/androidMain/kotlin/com/mettyoung/fitbro/MainActivity.kt`
- `CacheSource` enum in `CacheDataSource.kt` — keep existing key names, no rename needed

**What to remove/ignore:**
- `CronometerDataSourceImpl.kt` — dead code after this feature; leave in place but do not use
- `CronometerOAuthRepository` — no longer called; leave in place

**Health Connect record types needed (Android):**
- `NutritionRecord` → aggregate `ENERGY_TOTAL` metric by day → `DailyIntake.totalCalories`
- `BasalMetabolicRateRecord` → average `BASAL_METABOLIC_RATE` samples per day → `Metabolism.bmr`
- `ActiveCaloriesBurnedRecord` → already implemented → `ActivityBurn.neat`
- New permissions required: `HealthPermission.getReadPermission(NutritionRecord::class)`, `HealthPermission.getReadPermission(BasalMetabolicRateRecord::class)`

**HealthKit record types needed (iOS):**
- `HKQuantityTypeIdentifierDietaryEnergyConsumed` → sum samples per day (in kilocalories) → `DailyIntake.totalCalories`
- `HKQuantityTypeIdentifierBasalEnergyBurned` → sum samples per day → `Metabolism.bmr`
- `HKQuantityTypeIdentifierActiveEnergyBurned` → already implemented → `ActivityBurn.neat`

**TEF computation:**
- In `DashboardStateHolder.computeBalances()`, after fetching metabolism (which has `tef=0.0` from Health Connect), override: `metabolism.copy(tef = intake.totalCalories * 0.1)`

**Typecheck commands:**
- Android: `./gradlew :composeApp:compileDebugKotlinAndroid`
- iOS: `./gradlew :composeApp:compileKotlinIosSimulatorArm64`

---

## User Stories

### US-001: Extend HealthDataSource interface with readDailyIntake and readBasalMetabolicRate

**Description:** As a developer, I need the shared HealthDataSource interface to declare intake and BMR queries so platform implementations can provide them.

**Acceptance Criteria:**
- [ ] Add `suspend fun readDailyIntake(startDate: String, endDate: String): HealthResult<List<DailyIntake>>` to `HealthDataSource` interface
- [ ] Add `suspend fun readBasalMetabolicRate(startDate: String, endDate: String): HealthResult<List<Metabolism>>` to `HealthDataSource` interface — returned `Metabolism` objects must have `tef = 0.0` (TEF is computed, not fetched)
- [ ] Stub implementations return `HealthResult.Failure(HealthDataError.NotAvailable)` so Android/iOS compile before their actuals are done
- [ ] Both Android and iOS `actual` implementations updated to include the new methods (may be stubs that return NotAvailable for now)
- [ ] Typecheck passes (Android + iOS)

---

### US-002: Implement NutritionRecord + BMR reading in Android Health Connect

**Description:** As a developer, I need the Android HealthDataSource to read calorie intake from `NutritionRecord` and BMR from `BasalMetabolicRateRecord`.

**Acceptance Criteria:**
- [ ] `readDailyIntake`: aggregates `NutritionRecord.ENERGY_TOTAL` using `aggregateGroupByPeriod(Period.ofDays(1))` for the given date range; returns `DailyIntake(date, totalCalories)` per day
- [ ] `readBasalMetabolicRate`: reads `BasalMetabolicRateRecord` samples for date range; averages `basal_metabolic_rate.inKilocaloriesPerDay` per calendar day; returns `Metabolism(date, bmr, tef=0.0)` per day
- [ ] Days with no data are omitted from the result (not zero-filled)
- [ ] `AndroidManifest.xml` updated with `android.permission.health.READ_NUTRITION` and `android.permission.health.READ_BASAL_METABOLIC_RATE` in both the uses-permission block and the health-connect queries block
- [ ] Permission check: if `READ_NUTRITION` or `READ_BASAL_METABOLIC_RATE` not granted, return `HealthResult.Failure(HealthDataError.PermissionDenied)`
- [ ] Typecheck passes

---

### US-003: Implement dietary energy + BMR reading in iOS HealthKit

**Description:** As a developer, I need the iOS HealthDataSource to read calorie intake from `HKQuantityTypeIdentifierDietaryEnergyConsumed` and BMR from `HKQuantityTypeIdentifierBasalEnergyBurned`.

**Acceptance Criteria:**
- [ ] `readDailyIntake`: uses `HKSampleQuery` for `HKQuantityTypeIdentifierDietaryEnergyConsumed`; sums samples per calendar day (in kilocalories using `HKUnit.kilocalorieUnit()`); returns `DailyIntake(date, totalCalories)` per day
- [ ] `readBasalMetabolicRate`: uses `HKSampleQuery` for `HKQuantityTypeIdentifierBasalEnergyBurned`; averages samples per calendar day (in kcal/day); returns `Metabolism(date, bmr, tef=0.0)` per day
- [ ] `requestAuthorizationToShareTypes` updated to include the two new quantity types in its `readTypes` set
- [ ] Days with no data are omitted (not zero-filled)
- [ ] `Info.plist` `NSHealthShareUsageDescription` already exists — no change needed
- [ ] Typecheck passes

---

### US-004: Refactor DashboardStateHolder to use HealthDataSource for all data

**Description:** As a developer, I need DashboardStateHolder to use HealthDataSource for intake and BMR instead of CronometerDataSource, and to compute TEF from intake.

**Acceptance Criteria:**
- [ ] Remove `cronometerDataSource: CronometerDataSource` constructor parameter from `DashboardStateHolder`
- [ ] Replace `cronometerDataSource.fetchDailyIntake(...)` call with `healthDataSource.readDailyIntake(...)`
- [ ] Replace `cronometerDataSource.fetchMetabolism(...)` call with `healthDataSource.readBasalMetabolicRate(...)`
- [ ] In `computeBalances()`, after pairing intake and metabolism, compute TEF: pass `metabolism.copy(tef = intake.totalCalories * 0.1)` to `calorieMathRepository.computeDailyBalance()`
- [ ] `CacheSource.CRONOMETER_INTAKE` and `CacheSource.CRONOMETER_METABOLISM` keys still used for caching — no rename needed
- [ ] `buildWarnings()` call updated: remove `cronometerIntakeFailed`/`cronometerMetabolismFailed` flags; rename to `healthIntakeFailed`/`healthMetabolismFailed` with matching warning messages ("Health intake data unavailable", "BMR data unavailable")
- [ ] `DashboardWarnings.kt` `buildWarnings()` signature updated accordingly and `DashboardMissingDataTest.kt` updated to match
- [ ] Typecheck passes
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)

---

### US-005: Wire App.kt with real DashboardStateHolder

**Description:** As a developer, I need App.kt to instantiate real data sources and DashboardStateHolder so the dashboard renders live Health Connect data instead of hardcoded samples.

**Acceptance Criteria:**
- [ ] `App()` composable creates `createHealthDataSource()` (platform factory)
- [ ] `App()` creates `createCacheDataSource()` (platform factory)
- [ ] `App()` creates `CalorieMathRepositoryImpl()`
- [ ] `App()` creates `DashboardStateHolder` with the above dependencies and a `rememberCoroutineScope()`
- [ ] `App()` calls `DashboardScreen(stateHolder = ...)` instead of `DashboardContent` with hardcoded state
- [ ] Initial date range = last 7 days ending today (use `todayString()` and `minusDays(6)` from `DateUtil.kt`)
- [ ] `stateHolder.refresh()` is called on first composition via `LaunchedEffect(Unit)`
- [ ] Hardcoded `sampleBalances` and `demoState` removed from `App.kt`
- [ ] Typecheck passes

---

### US-006: Add Health Connect permission gate on Android

**Description:** As a user, I need the app to request Health Connect permissions before showing the dashboard so data can actually be fetched.

**Acceptance Criteria:**
- [ ] Android `MainActivity.kt` checks if all required Health Connect permissions are granted on start: `READ_ACTIVE_CALORIES_BURNED`, `READ_NUTRITION`, `READ_BASAL_METABOLIC_RATE`
- [ ] If permissions not all granted: show a simple permission screen (Column with explanation text + "Grant Permissions" button)
- [ ] "Grant Permissions" button launches `HealthPermission.getHealthPermissionsActivityContract()` with the required permission set
- [ ] On activity result: if all granted, navigate to dashboard; if denied, show "Permissions required" message with retry button
- [ ] On iOS: no permission gate screen needed — HealthKit permission is requested inline by `HealthDataSource.ios.kt` on first query
- [ ] Typecheck passes

---

## Functional Requirements

- FR-1: All calorie data (intake, BMR, NEAT) sourced from device health store — no external API
- FR-2: TEF = 10% of daily calorie intake, computed in `DashboardStateHolder.computeBalances()`
- FR-3: Android permissions gated before dashboard renders
- FR-4: Missing Health Connect data (no records for a day) → day excluded from chart, not zero-filled
- FR-5: Cache fallback still works for all three data types (intake, BMR, activity)
- FR-6: Error states show appropriate warnings via `buildWarnings()` for each missing source

---

## Non-Goals

- Do NOT implement EAT (Exercise Activity Thermogenesis) — `eat = 0.0` stays
- Do NOT delete CronometerDataSourceImpl or OAuthRepository — leave as dead code
- Do NOT add a settings screen for user profile (height/weight/age)
- Do NOT support step-count-based NEAT estimation — use Health Connect records only
- Do NOT add onboarding beyond the permission gate

---

## Technical Considerations

- `NutritionRecord` aggregation on Android: use `AggregateGroupByPeriodRequest` with `NutritionRecord.ENERGY_TOTAL` metric and `Period.ofDays(1)` — same pattern as existing `ActiveCaloriesBurnedRecord` aggregation
- `BasalMetabolicRateRecord` on Android: no aggregate metric exists — must use `readRecords()` then manually average per calendar day using `record.basalMetabolicRate.inKilocaloriesPerDay`
- K/N category method import pattern (documented in progress.txt): explicit imports needed for `HKUnit.kilocalorieUnit()` and similar ObjC category methods
- `createHealthDataSource()` is an `expect` function in commonMain — adding methods to the interface requires updating both platform `actual` implementations simultaneously
- Health Connect client is not available on iOS — all HC code stays in `androidMain`

---

## Success Metrics

- Dashboard renders real calorie intake from health apps (e.g. Mi Fit food log, Samsung Health) without any API keys or server
- BMR values match what health apps display
- Permissions are requested gracefully on first launch
- Typecheck and unit tests pass on both platforms
