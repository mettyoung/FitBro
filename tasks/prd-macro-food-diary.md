# PRD: Macro Page Redesign + Food Diary with OpenFoodFacts

## Introduction

Redesign the Macros tab (`MacroDailyCounterDetail`) into two zones: a top 1/3 macro summary header with circle charts and a calorie progress bar, and a bottom 2/3 food diary grouped by meal. Food search is powered by the OpenFoodFacts API (no auth required). The food diary is persisted to a local SQLDelight database (source of truth) with a supplemental write to HealthConnect after each diary save.

> **Note on HealthConnect as DB:** HealthConnect is Android-only, write-optimized, and not a reliable query/read source — it must not serve as the app's source of truth. Local SQLDelight DB → sync to HealthConnect.

## Goals

- Redesign macro summary section with per-macro circle charts and calorie progress bar
- Enable food search via OpenFoodFacts public API
- Persist food diary entries by meal, by day using SQLDelight (KMP-compatible)
- Support edit/delete of diary entries
- Sync nutrition totals to HealthConnect after diary writes (Android supplemental)
- View past days' food diary and weekly macro trends

## User Stories

### US-001: SQLDelight schema for food diary
**Description:** As a developer, I need a local relational store for food diary entries so data persists and is queryable across days.

**Acceptance Criteria:**
- [ ] Add SQLDelight dependency to `composeApp/build.gradle.kts` for KMP (`app.cash.sqldelight`)
- [ ] Create schema: `FoodDiaryEntry` (id, date, mealType, foodName, brandName?, calories, proteinG, carbG, fatG, servingSize, servingUnit)
- [ ] `MealType` values: `BREAKFAST`, `LUNCH`, `DINNER`, `SNACKS`
- [ ] Generate typesafe queries: insert, getByDate, getByDateAndMeal, update, delete
- [ ] Platform drivers wired: `AndroidSqliteDriver` (androidMain), `NativeSqliteDriver` (iosMain)
- [ ] Typecheck passes

### US-002: OpenFoodFacts data source
**Description:** As a developer, I need a data source that queries OpenFoodFacts so users can search for real foods.

**Acceptance Criteria:**
- [ ] Create `OpenFoodFactsDataSource` interface in `commonMain`
- [ ] Implement with Ktor client: `GET https://world.openfoodfacts.org/cgi/search.pl?search_terms={query}&action=process&json=1&page_size=20`
- [ ] Include `User-Agent` header: `FitBro/1.0 (Android; emmettyoung92@gmail.com)`
- [ ] Response mapped to `FoodSearchResult(name, brand, caloriesPer100g, proteinPer100g, carbPer100g, fatPer100g, servingSizeG?)`
- [ ] Sealed error type: `OpenFoodFactsError` (NetworkError, ParseError, EmptyResults)
- [ ] Returns `Result<List<FoodSearchResult>, OpenFoodFactsError>`
- [ ] Typecheck passes

### US-003: Food diary repository
**Description:** As a developer, I need a repository that orchestrates diary reads/writes and computes daily macro totals so the UI has a single source of truth.

**Acceptance Criteria:**
- [ ] Create `FoodDiaryRepository` interface in `commonMain`
- [ ] Methods: `getEntriesForDate(date): Flow<List<FoodDiaryEntry>>`, `addEntry(entry)`, `updateEntry(entry)`, `deleteEntry(id)`, `getDailyTotals(date): Flow<DailyMacroTotals>`
- [ ] `DailyMacroTotals` = sum of calories/protein/carbs/fat across all entries for that date
- [ ] Implement with SQLDelight queries
- [ ] Typecheck passes

### US-004: HealthConnect sync on diary write
**Description:** As a user, I want my food diary entries automatically reflected in HealthConnect so other health apps can see my nutrition.

**Acceptance Criteria:**
- [ ] After successful `addEntry` / `updateEntry` / `deleteEntry`, trigger HealthConnect `NutritionRecord` write via existing `HealthDataSource` expect/actual
- [ ] Add `writeNutritionRecord(entry: FoodDiaryEntry)` to `HealthDataSource` (androidMain: HealthConnect `NutritionRecord`, iosMain: no-op)
- [ ] Sync failures are logged but do NOT block diary save (fire-and-forget)
- [ ] Typecheck passes

### US-005: Macro summary header redesign
**Description:** As a user, I want to see my macro progress at a glance in the top of the Macros tab so I know where I stand for the day.

**Acceptance Criteria:**
- [ ] Top ~1/3 of `MacroDailyCounterDetail` replaced with new `MacroSummaryHeader` composable
- [ ] Single row of three `MacroCircleChart` composables: Carbs (blue), Protein (red), Fat (yellow)
- [ ] Each circle shows: macro name, grams consumed / grams goal, arc fill proportional to progress (capped at 100%)
- [ ] Below the row: total calorie count (e.g. "1,450 / 2,000 kcal") + `LinearProgressIndicator` 
- [ ] Progress bar color: green <85%, amber 85–100%, red >100%
- [ ] Data sourced from `DailyMacroTotals` (diary) + `UserSettingsDataSource` (goals)
- [ ] Typecheck passes
- [ ] Verify in browser using dev-browser skill

### US-006: Food diary list grouped by meal
**Description:** As a user, I want to see my logged foods organized by meal so I can track what I ate and when.

**Acceptance Criteria:**
- [ ] Bottom ~2/3 of `MacroDailyCounterDetail` shows `FoodDiarySection` per meal (Breakfast, Lunch, Dinner, Snacks)
- [ ] Each section header shows meal name + section calorie total
- [ ] Each section has an "Add food" button/icon
- [ ] Empty sections still show with "Add food" (not hidden)
- [ ] Each food entry row shows: food name, serving size, calories, macros (small text)
- [ ] Long-press or swipe on entry reveals Delete option
- [ ] Tap on entry opens edit bottom sheet (US-007)
- [ ] Typecheck passes
- [ ] Verify in browser using dev-browser skill

### US-007: Food search screen
**Description:** As a user, I want to search for foods by name and add them to a meal so I can log what I eat.

**Acceptance Criteria:**
- [ ] "Add food" opens `FoodSearchSheet` (bottom sheet or full screen)
- [ ] Search bar auto-focused on open
- [ ] Results list shows: food name, brand (if available), calories per serving
- [ ] Tapping result opens `FoodEntrySheet`: serving size input (with unit selector g/oz/serving), macro preview updates live as serving size changes
- [ ] "Add" button saves entry to selected meal + date via `FoodDiaryRepository.addEntry`
- [ ] Loading state shown during API call
- [ ] Empty results state shown with message "No results for '{query}'"
- [ ] Error state shown on network failure with retry button
- [ ] Typecheck passes
- [ ] Verify in browser using dev-browser skill

### US-008: Edit existing diary entry
**Description:** As a user, I want to edit the serving size of a logged food so I can correct mistakes.

**Acceptance Criteria:**
- [ ] Tapping diary entry opens `FoodEntrySheet` pre-filled with current values
- [ ] User can update serving size / unit
- [ ] Macros preview updates live
- [ ] "Save" calls `FoodDiaryRepository.updateEntry`; sheet dismisses and list refreshes
- [ ] Typecheck passes
- [ ] Verify in browser using dev-browser skill

### US-009: Delete diary entry
**Description:** As a user, I want to remove a logged food so I can fix errors.

**Acceptance Criteria:**
- [ ] Delete action (swipe or long-press) shows confirmation snackbar with Undo (3s window)
- [ ] After 3s or confirmed: `FoodDiaryRepository.deleteEntry` called
- [ ] List updates immediately (optimistic)
- [ ] Typecheck passes
- [ ] Verify in browser using dev-browser skill

### US-010: View past days' food diary
**Description:** As a user, I want to browse previous days' food logs so I can review my eating history.

**Acceptance Criteria:**
- [ ] Date selector (reuse existing `DatePickerDialog` or a date chip row) at top of Macros tab
- [ ] Changing date loads diary entries and macro totals for that date
- [ ] Default date is today
- [ ] Typecheck passes
- [ ] Verify in browser using dev-browser skill

### US-011: Weekly macro trends
**Description:** As a user, I want to see a 7-day macro trend so I can understand my weekly eating patterns.

**Acceptance Criteria:**
- [ ] Collapsible "Weekly Trends" card below food diary
- [ ] Shows bar or line chart for each macro (carbs/protein/fat/calories) across last 7 days
- [ ] Data sourced from `FoodDiaryRepository` aggregated by date
- [ ] Days with no data show as zero
- [ ] Typecheck passes
- [ ] Verify in browser using dev-browser skill

## Functional Requirements

- FR-1: SQLDelight DB stores `FoodDiaryEntry` with date, mealType, macronutrients, serving details
- FR-2: OpenFoodFacts API queried via Ktor; response mapped to `FoodSearchResult`; User-Agent header required
- FR-3: `MacroSummaryHeader` displays circle charts for carbs/protein/fat + calorie progress bar sourced from daily diary totals vs user goals
- FR-4: Food diary section renders entries grouped by BREAKFAST / LUNCH / DINNER / SNACKS for selected date
- FR-5: Serving size input recalculates macros proportionally (macros per 100g × serving_g / 100). Units: g, oz (1 oz = 28.3495g), "1 serving" (gram equivalent from API `serving_size`; default 100g if absent)
- FR-6: After any diary write, `HealthDataSource.writeNutritionRecord` called as fire-and-forget (failures non-blocking)
- FR-7: Optimistic delete with 3s undo snackbar
- FR-8: Date picker drives diary and summary views together; default = today
- FR-9: Weekly trends aggregate diary DB for last 7 days

## Non-Goals

- No barcode scanning (future feature)
- No custom food creation (manual entry without API search)
- No meal planning or scheduling
- No cloud sync beyond HealthConnect
- No calorie goal recommendations (user sets goals manually in Settings tab)
- No iOS HealthKit integration (iosMain `writeNutritionRecord` is no-op)
- No pagination of OpenFoodFacts results (20 results per search)
- No offline caching of food search results

## Design Considerations

- `MacroCircleChart`: use Canvas API (Compose) to draw arc. No third-party chart lib unless already in project.
- Weekly trends chart: reuse or extend `CalorieBalanceChart` pattern if compatible; otherwise Canvas-based bar chart
- Circle chart color tokens: carbs = `MaterialTheme.colorScheme.primary`, protein = `MaterialTheme.colorScheme.error`, fat = `MaterialTheme.colorScheme.tertiary`
- Bottom 2/3 food diary: `LazyColumn` inside a `NestedScrollConnection`-aware layout so macro header stays sticky
- `FoodSearchSheet` / `FoodEntrySheet`: use `ModalBottomSheet`

## Technical Considerations

- **DB**: SQLDelight `2.x` (`app.cash.sqldelight:android-driver` + `native-driver`). No Room — Android-only.
- **API**: OpenFoodFacts `v2` search endpoint. No API key. Must send User-Agent per their fair-use policy.
- **Macro calculation**: `macroG = (macroPer100g * servingG) / 100.0`. Round to 1 decimal.
- **State holder**: follow existing `DashboardStateHolder` pattern — constructor-inject repo + scope, expose `StateFlow`, no ViewModel.
- **HealthConnect write**: extend existing `HealthDataSource` expect interface. Android impl writes `NutritionRecord`; iOS impl is no-op stub.
- **Macro source priority**: diary DB totals used on both Balance and Macros tabs. Fall back to HC-read `DailyIntake` only when diary total is empty for that date.
- **Shared date state**: `DashboardStateHolder` owns `selectedDate: StateFlow<LocalDate>`. `FoodDiaryStateHolder` accepts it as a constructor param (or observes shared flow). Single source drives both tabs.
- **Serving unit conversion**: oz → g multiply by 28.3495. "1 serving" → use `servingSizeG` from `FoodSearchResult`; if null default to 100g.

## Success Metrics

- User can search and log a food in under 30 seconds
- Macro summary header reflects diary changes immediately (no manual refresh)
- HealthConnect write does not add perceptible latency to diary save
- Zero crash on empty diary days

## Resolved Decisions

- **Diary vs HC-read priority:** `DailyMacroTotals` from diary DB is source of truth on both Balance and Macros tabs. Fall back to `DailyIntake` from HC-read only when diary total for that date is empty (no logged entries).
- **Shared date state:** Balance tab and Macros tab share a single selected date. Date changes on either tab propagate to both. `DashboardStateHolder` owns selected date; `FoodDiaryStateHolder` observes it.
- **Serving units:** g, oz, and "1 serving". "1 serving" uses `serving_size` from OpenFoodFacts response (in grams) as the gram equivalent for macro calculation. If API returns no serving size, default to 100g and label as "100g serving".
