# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FitBro is a Kotlin Multiplatform (KMP) calorie-tracking app targeting Android and iOS via Compose Multiplatform.

- **Shared code**: `composeApp/src/commonMain/` — UI, data layer, business logic
- **Android-specific**: `composeApp/src/androidMain/` — MainActivity, Health Connect, permissions, barcode scanning
- **iOS-specific**: `composeApp/src/iosMain/` — Swift interop, platform APIs

## Maintenance Rules

- This CLAUDE.md is a living document. After any major architectural change, refactor, or new convention, update the relevant sections immediately.
- When I say “update CLAUDE.md”, revise only the changed parts and keep the file concise.

## Architecture Patterns

### Data Layer
- **Repositories** (`data/repository/`): Abstract data operations with explicit error types (sealed `CalorieResult`).
- **Data Sources** (`data/{health,cache,food,db}/`): Concrete implementations (Health Connect/HealthKit, local cache, FatSecret, SQLDelight). Cross-platform ones use top-level `expect/actual createXxx()` factories (e.g. `createHealthDataSource()`, `createCacheDataSource()`, `createSqlDriver()`).
- **Models** (`data/model/`): Domain objects (DailyBalance, FoodDiaryEntry, Metabolism).
- **Database**: SQLDelight (SQLite). Queries live in `src/commonMain/sqldelight/`. Generated code in `data/db/`.

### UI Layer
- **StateHolder pattern** (`ui/dashboard/DashboardStateHolder.kt`): Compose ViewModel equivalent. Manages state, side effects, coordination.
- **Unidirectional data flow**: StateHolder emits `StateFlow<UiState>`, UI responds to events.
- **Wiring**: `App.kt` instantiates data sources, repositories, and StateHolders via `remember { ... }` and passes them down. Platform factories called here.

### Entry Points
- **Android**: `MainActivity.kt` → `PermissionGateApp.kt` → `App()` (shared Composable).
  - Manages permission gates (Health Connect, barcode scanner).
- **iOS**: Framework generated from `iosMain/`; Xcode entry point in `iosApp/`.

## Build & Test

### Build Android
```shell
./gradlew :composeApp:assembleDebug          # Build APK
./gradlew :composeApp:installDebug           # Build + install to emulator
```

### Build iOS
```shell
# Open in Xcode and build from IDE, or use:
./gradlew :composeApp:iosSimulatorArm64Binaries  # Build framework for simulator
```

### Run Tests
```shell
./gradlew :composeApp:commonTestClasses      # Compile common tests
./gradlew :composeApp:test                   # Run all tests (commonTest + platform-specific)
./gradlew :composeApp:testDebugUnitTest      # Run Android unit tests only
```

### Lint / Format
```shell
# Kotlin code style (configured in gradle.properties: kotlin.code.style=official)
# No auto-formatter configured; follow Kotlin conventions
```

## Dependencies & Key Libraries

- **Compose Multiplatform**: UI framework
- **Ktor**: HTTP client (platform-specific backends: OkHttp for Android, Darwin for iOS)
- **SQLDelight**: Type-safe SQL with coroutine bindings
- **Coroutines**: Async/concurrency (default dispatcher: `Dispatchers.Default` for background work)
- **Health Connect API** (Android): `androidx.health.connect`; iOS uses **HealthKit** (`platform.HealthKit`, real `HKHealthStore` integration in `iosMain`)
- **Barcode Scanning** (Android): GMS Code Scanner
- **Multiplatform Settings**: Encrypted key-value storage (android: `androidx.security.crypto`)
- **FatSecret Platform API**: Food database integration. OAuth 1.0 signing (HMAC-SHA1) via `util/HmacSha1` (expect/actual). Endpoint `https://platform.fatsecret.com/rest/server.api`.

## Code Organization

### Key Classes & Responsibilities

| Path | Purpose |
|------|---------|
| `ui/dashboard/DashboardStateHolder` | Main dashboard state + calorie math coordination |
| `ui/dashboard/FoodDiaryStateHolder` | Food diary add/edit/list logic |
| `data/repository/FoodDiaryRepositoryImpl` | Food diary CRUD (SQLDelight) |
| `data/repository/CalorieMathRepositoryImpl` | Calorie/macro/TEF calculations (interface `CalorieMathRepository`, sealed `CalorieResult`) |
| `data/health/HealthDataSource` | expect interface + `createHealthDataSource()` factory; Android actual = Health Connect, iOS actual = HealthKit |
| `data/food/FatSecretFoodDataSource` | FatSecret REST calls, OAuth 1.0 HMAC-SHA1 signing, search caching |
| `data/cache/UserSettingsDataSource` | Encrypted user prefs (target weight, activity level, etc.) |
| `data/db/FitBroDatabase` | SQLDelight database (generated from `.sq` files) |

### Source Folders

- `common*`: Shared across Android + iOS
- `android*`: Android-specific (uses Android SDK, Health Connect, security.crypto)
- `ios*`: iOS-specific (Swift interop via framework)

## Development Workflow

### Adding a Feature

1. **Model**: Add domain model to `data/model/`.
2. **Data Source / Repository**: Implement in appropriate `data/` subdirectory.
3. **State Holder**: Add state class + update logic in a new or existing StateHolder.
4. **UI**: Build Composable using state from StateHolder.
5. **Platform Specifics**: Place Android/iOS code in `android*/` or `ios*/` if needed.

### Database Changes

1. Edit `.sq` files in `src/commonMain/sqldelight/com/mettyoung/fitbro/data/db/`.
2. Add schema migrations as `.sqm` files under `.../db/migrations/` (e.g. `1.sqm`).
3. SQLDelight auto-generates `FitBroDatabase` (configured in `composeApp/build.gradle.kts`) and query types.
4. Update repository to use new queries.

### Handling Errors

- Use sealed result types (e.g., `CalorieResult`, `FoodDiaryResult`).
- Avoid exceptions for expected errors (invalid input, calculation failure).
- Catch exceptions at boundaries (API calls, Android APIs).

## Key Constraints & Patterns

- **Main-safe data ops**: Repositories use `withContext(Dispatchers.Default)` or SQLDelight's `.asFlow()` for background work.
- **No unchecked exceptions**: Sealed result types preferred.
- **Coroutine scopes**: Pass `CoroutineScope` to StateHolders; they own side effects.
- **Compose previews**: Android `@Preview` in `MainActivity.kt`; use for UI iteration.
- **Platform factories**: Instantiate platform-specific objects in `App.kt` via `remember { createXxx() }`.

## Testing

- Common tests in `commonTest/`.
- Use `kotlin.test` (JUnit-compatible, multiplatform).
- Test repositories & business logic (CalorieMathRepository, FoodDiaryRepository).
- StateHolder tests are minimal; focus on data layer correctness.

### Example Test Pattern
```kotlin
@Test
fun myCalculation() {
    val result = repo.calculate(input)
    val success = assertIs<Result.Success>(result)
    assertEquals(expected, success.value)
}
```

## Recent Changes & Context

- **Food Diary**: Macro tracking, serving-size defaults, FatSecret integration (replaced earlier OpenFoodFacts).
- **Health**: Primary data source for intake, BMR, activity. Android = Health Connect (write capability for workouts); iOS = HealthKit.
- **Cleanup**: Removed unused Cronometer OAuth integration. Note: OAuth 1.0 / HMAC-SHA1 infra now powers FatSecret (`util/HmacSha1`), not Cronometer.
- **UI Redesign**: Recent Gemini-based redesign; Compose patterns align with Material 3.

## Gotchas & Non-Obvious Details

1. **SQLDelight transactions**: Use `transactionWithResult {}` to return inserted IDs.
2. **Health permissions**: Android = Health Connect (permission gate in `PermissionGateApp.kt`); iOS = HealthKit (authorization in `HealthDataSource.ios.kt`). Both integrated.
3. **Coroutine cancellation**: StateHolder scope cancellation cleans up flows & jobs.
4. **Date format**: `todayString()` & `DateRange` use "YYYY-MM-DD" strings for consistency.
5. **TEF calculation**: Falls back to flat 10% if macros missing (see `CalorieMathRepository`).
6. **Serving sizes**: Default from FatSecret; user can override per entry.