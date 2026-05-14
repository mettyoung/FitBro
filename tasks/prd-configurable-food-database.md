# PRD: Configurable Food Database

## Introduction

Replace the hardcoded OpenFoodFacts dependency with a configurable food data source abstraction. Users can select their preferred food database in Settings (persisted across sessions). Two databases supported: OpenFoodFacts (current, community-sourced) and USDA FoodData Central (lab-analyzed, high nutrient accuracy). Barcode scanning remains OpenFoodFacts-only; USDA text search only.

> **Important — API Note:** NCCDB (Nutrition Coordinating Center) has **no public REST API** — it is licensed software only. USDA FoodData Central (`api.nal.usda.gov`) is the practical equivalent: it contains SR Legacy data (same lab-analyzed quality as NCCDB) and is free with an API key. This PRD uses USDA FDC as the concrete "NCCDB-quality" implementation.

## Goals

- Decouple food search from OpenFoodFacts by introducing a generic `FoodDataSource` interface
- Add USDA FoodData Central as a second search backend with higher nutrient accuracy
- Persist the user's database choice via `UserSettingsDataSource`
- Surface the choice as a picker in the Settings screen
- Hide barcode scanning when USDA is selected (no barcode support in FDC API)

## User Stories

### US-001: Introduce generic FoodDataSource interface
**Description:** As a developer, I need a database-agnostic `FoodDataSource` interface so that the UI layer is not coupled to OpenFoodFacts.

**Acceptance Criteria:**
- [ ] New interface `FoodDataSource` in `data/food/` with:
  - `suspend fun search(query: String): FoodResult<List<FoodSearchResult>>`
  - `suspend fun searchByBarcode(barcode: String): FoodResult<FoodSearchResult>`
  - `val supportsBarcode: Boolean`
- [ ] `OpenFoodFactsError` renamed to `FoodError`; add `data object NotSupported : FoodError()`
- [ ] `FoodResult.Failure` error type changed from `OpenFoodFactsError` to `FoodError`
- [ ] `OpenFoodFactsDataSource` interface deleted; `OpenFoodFactsDataSourceImpl` renamed to `OpenFoodFactsFoodDataSource` and implements `FoodDataSource` with `supportsBarcode = true`
- [ ] All call sites updated (`App.kt`, `DashboardWithTabs`, `MacroDailyCounterDetail`, `FoodLogSheet`) to use `FoodDataSource` instead of `OpenFoodFactsDataSource`
- [ ] Build compiles with no errors

### US-002: Add food database preference to UserSettingsDataSource
**Description:** As a developer, I need to persist the user's food database choice so it survives app restarts.

**Acceptance Criteria:**
- [ ] `FoodDatabase` enum added to `data/food/`: `OPEN_FOOD_FACTS`, `USDA`
- [ ] `UserSettingsDataSource` interface gains `getFoodDatabase(): FoodDatabase` and `setFoodDatabase(db: FoodDatabase)`
- [ ] Default value when no preference stored: `OPEN_FOOD_FACTS`
- [ ] Platform implementations (`UserSettingsDataSource.android.kt` / iOS equivalent) store value as string key `"food_database"` in Multiplatform Settings
- [ ] Build compiles with no errors

### US-003: Implement USDA FoodData Central data source
**Description:** As a developer, I need a `FoodDataSource` implementation backed by USDA FoodData Central so that users can search a high-accuracy lab-analyzed food database.

**Acceptance Criteria:**
- [ ] New class `UsdaFoodDataSource` in `data/food/` implements `FoodDataSource`
- [ ] `supportsBarcode = false`
- [ ] `search(query)` hits `GET https://api.nal.usda.gov/fdc/v1/foods/search` with params:
  - `query` = search term
  - `pageSize` = 20
  - `dataType` = `"SR Legacy,Foundation"` (lab-analyzed only — excludes branded/user-submitted)
  - `api_key` = configurable constant (default `"DEMO_KEY"` for development)
- [ ] Response mapped to `FoodSearchResult`:
  - `name` = `description`
  - `brand` = `brandOwner` (nullable)
  - `caloriesPer100g` from nutrient with `nutrientName == "Energy"` and `unitName == "KCAL"`
  - `proteinPer100g` from nutrient with `nutrientName == "Protein"`
  - `carbPer100g` from nutrient with `nutrientName == "Carbohydrate, by difference"`
  - `fatPer100g` from nutrient with `nutrientName == "Total lipid (fat)"`
  - `servingSizeG` = `servingSize` field (nullable)
- [ ] `searchByBarcode()` returns `FoodResult.Failure(FoodError.NotSupported)` immediately
- [ ] Network/parse errors return `FoodResult.Failure(FoodError.NetworkError(...))`
- [ ] Empty results return `FoodResult.Failure(FoodError.EmptyResults)`
- [ ] Build compiles with no errors

### US-004: Wire active food datasource from settings in App.kt
**Description:** As a developer, I need App.kt to instantiate the correct `FoodDataSource` based on the persisted setting, and to re-instantiate when the setting changes.

**Acceptance Criteria:**
- [ ] `App.kt` reads `userSettingsDataSource.getFoodDatabase()` reactively (or on recomposition)
- [ ] `OpenFoodFactsFoodDataSource` instantiated when `OPEN_FOOD_FACTS` selected
- [ ] `UsdaFoodDataSource` instantiated when `USDA` selected
- [ ] Active `FoodDataSource` passed down to `DashboardWithTabs` → `MacroDailyCounterDetail` → `FoodLogSheet`
- [ ] Switching database in Settings causes next food search to use the new source (no restart required)
- [ ] Build compiles with no errors

### US-005: Hide barcode button when active database does not support it
**Description:** As a user, I want the barcode scan button hidden when using USDA so I am not confused by an unsupported action.

**Acceptance Criteria:**
- [ ] `FoodLogSheet` checks `foodDataSource.supportsBarcode` to conditionally render the barcode scan `IconButton`
- [ ] Barcode button absent when `USDA` selected
- [ ] Barcode button present and functional when `OPEN_FOOD_FACTS` selected (existing behavior unchanged)
- [ ] Build compiles with no errors
- [ ] Verify in browser using dev-browser skill

### US-006: Add food database selector to Settings screen
**Description:** As a user, I want to choose my food database in Settings so I can trade off coverage (OpenFoodFacts) vs accuracy (USDA).

**Acceptance Criteria:**
- [ ] `MacroGoalsSettings` (or a new section in the same Settings tab) shows a "Food Database" section with two selectable options:
  - **OpenFoodFacts** — "Community-sourced, includes barcodes" (subtitle)
  - **USDA FoodData Central** — "Lab-analyzed, high accuracy, no barcode" (subtitle)
- [ ] Current selection shown with a filled radio/checkmark indicator
- [ ] Tapping an option calls `userSettingsDataSource.setFoodDatabase(...)` immediately (no save button needed)
- [ ] Selection persists after app restart
- [ ] Build compiles with no errors
- [ ] Verify in browser using dev-browser skill

## Functional Requirements

- **FR-1:** `FoodDataSource` is the single interface for all food search operations; no UI code imports `OpenFoodFactsDataSourceImpl` directly.
- **FR-2:** `FoodDatabase` enum has exactly two values: `OPEN_FOOD_FACTS`, `USDA`.
- **FR-3:** `UserSettingsDataSource.getFoodDatabase()` defaults to `OPEN_FOOD_FACTS` when no value has been stored.
- **FR-4:** USDA search targets `dataType = SR Legacy,Foundation` only — excludes `Branded` and `Survey (FNDDS)` to ensure lab-analyzed results.
- **FR-5:** USDA API key is a constant in `UsdaFoodDataSource`; default value is `"DEMO_KEY"` (rate-limited but functional without registration).
- **FR-6:** Barcode scan button is hidden (not disabled) when `foodDataSource.supportsBarcode == false`.
- **FR-7:** `FoodError` sealed class replaces `OpenFoodFactsError` everywhere; `NotSupported` variant added for USDA barcode path.
- **FR-8:** USDA nutrient lookup uses exact `nutrientName` string matching (see US-003 AC); missing nutrients default to `0.0` (not null).

## Non-Goals

- No automatic fallback from USDA to OpenFoodFacts on empty results or failure
- No support for additional databases beyond these two
- No per-search database override (setting is global only)
- No caching of food search results
- No USDA API key management UI (key is hardcoded constant)
- No USDA barcode support — USDA FDC API does not expose barcode lookup

## Technical Considerations

- **USDA FDC API key:** Free registration at `https://fdc.nal.usda.gov/api-guide.html`. `DEMO_KEY` allows 1,000 req/hour per IP with no signup. Suitable for dev/test. Production should use a registered key.
- **USDA `dataType` filter:** Use `SR Legacy` and `Foundation` (comma-separated). These are the peer-reviewed, lab-analyzed datasets. `Branded` foods are industry-submitted and should be excluded since OpenFoodFacts covers branded better.
- **Nutrient name matching:** USDA FDC response contains a `foodNutrients` array with `nutrientName` and `value`. Names are verbose strings — match exactly as documented in US-003.
- **Reactive database switching in App.kt:** Use `remember(userSettingsDataSource.getFoodDatabase())` to re-create the data source when the setting changes. Since `UserSettingsDataSource` doesn't expose a `Flow`, reading in a recomposable lambda is sufficient.
- **`FoodResult` error generalization:** `FoodResult.Failure` currently has type `Failure(val error: OpenFoodFactsError)`. Change to `Failure(val error: FoodError)` and rename `OpenFoodFactsError` → `FoodError`. No behavioral change for OpenFoodFacts path.
- **Existing call sites:** `FoodLogSheet`, `MacroDailyCounterDetail`, `DashboardWithTabs`, `App.kt` all pass `OpenFoodFactsDataSource`. Update parameter types to `FoodDataSource`.

## Success Metrics

- User can switch databases in Settings in under 3 taps
- USDA search returns results for common whole foods (e.g., "chicken breast", "brown rice")
- Barcode button absent on food log sheet when USDA selected
- No regression in OpenFoodFacts search or barcode scan

## Open Questions

1. **USDA API key for production:** `DEMO_KEY` is rate-limited (1000 req/hour). Should we register a real key before shipping, or is DEMO_KEY acceptable for initial rollout?
2. **USDA serving size:** FDC `servingSize` field is often null for SR Legacy entries (lab data is per-100g only). Acceptable to show no serving size default for USDA foods, or should we hardcode 100g?
3. **USDA result quality for branded foods:** Filtering to `SR Legacy,Foundation` excludes branded products entirely. Is this acceptable, or should `Branded` be included as a third data type option?
