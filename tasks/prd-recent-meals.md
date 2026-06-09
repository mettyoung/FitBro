# PRD: Recent Foods & Copy Meal to Another Day

## 1. Introduction/Overview

Logging the same foods repeatedly is tedious: today the only paths are FatSecret
search, barcode, custom foods, or custom meals. This feature adds two
low-friction shortcuts built on the user's own logging history:

1. **Recent foods** — a "Recent" tab in the Log Food sheet listing distinct
   foods the user has logged before, most-recent first, addable in one tap.
2. **Copy meal to another day** — duplicate a single meal slot (e.g. Monday's
   Lunch) onto another date's same slot, so repeated days (meal-prep, routines)
   don't require re-entry.

Both reuse the existing `FoodDiaryEntry` table and the established repository /
StateHolder / Compose patterns. Neither introduces a new persistent table.

## 2. Goals

- Let users re-log a previously logged food in **one tap**, without searching.
- Surface recents from history automatically — no manual saving step (that is
  what Custom Meals/Foods already cover).
- Let users copy an entire meal slot to a different day in **≤3 taps**.
- No new database table; derive everything from existing `FoodDiaryEntry` rows.
- No regression to existing search / barcode / custom-food / custom-meal flows.

## 3. User Stories

### US-001: Recent-foods query (data layer)
**Description:** As a developer, I need a query that returns the most recently
logged **distinct** foods so the Recent tab has data.

**Acceptance Criteria:**
- [ ] Add `recentFoods` query to `FoodDiary.sq` returning one row per distinct
      food, ordered most-recent first, with `LIMIT` (parameterized).
- [ ] Distinctness key = `food_id` when present, else
      `lower(trim(foodName)) || '|' || lower(trim(coalesce(brandName,'')))`.
      Same food logged 5× appears once.
- [ ] Each returned row carries the **last-logged** macros, `servingSizeG`,
      `servingUnit`, `foodName`, `brandName`, `food_id` for that food (the row
      with the greatest `id`).
- [ ] Add `FoodDiaryRepository.getRecentFoods(limit: Int): Flow<List<FoodDiaryEntry>>`
      + impl, main-safe (`asFlow().mapToList(Dispatchers.Default)`).
- [ ] Returned entries are detached templates: `id = 0`, `date`/`mealType`
      ignored by callers (they will be overwritten on add).
- [ ] Unit test (commonTest, `runSync` + fake repo): dedup keeps one row per
      food, ordering is most-recent-first, limit respected.
- [ ] Typecheck + `:composeApp:testDebugUnitTest` pass.

### US-002: Expose recents in FoodDiaryStateHolder
**Description:** As a developer, I need recents as observable state so the Log
Food sheet can render them.

**Acceptance Criteria:**
- [ ] `FoodDiaryStateHolder` exposes `recentFoods: StateFlow<List<FoodDiaryEntry>>`
      (e.g. `getRecentFoods(20)` via `stateIn(WhileSubscribed(5_000), emptyList())`).
- [ ] No change to existing `state` / `weeklyTotals` flows.
- [ ] Typecheck passes.

### US-003: "Recent" tab in Log Food sheet
**Description:** As a user, I want a Recent tab in the food-logging sheet so I can
see foods I've logged before instead of searching.

**Acceptance Criteria:**
- [ ] Log Food sheet shows two segments/tabs: **Search** (default) and **Recent**.
- [ ] Recent tab lists recent foods: name, brand (if any), and
      `"{servingSizeG}{unit} · {calories}kcal"` subtitle, most-recent first.
- [ ] Empty state when no history: "No recent foods yet — log something first."
- [ ] Switching tabs does not reset an in-progress search.
- [ ] Reuses existing row styling (`FoodResultRow` look) and sheet chrome.
- [ ] Verify on device (`installDebug`): Recent tab renders prior foods.
- [ ] Typecheck passes.

### US-004: One-tap add from Recent tab
**Description:** As a user, I want tapping a recent food to add it straight to the
meal slot I'm logging into, using the serving I last used.

**Acceptance Criteria:**
- [ ] Tapping a recent row creates a new `FoodDiaryEntry` for the current
      `mealType` + selected `date`, copying macros, `servingSizeG`,
      `servingUnit`, `foodName`, `brandName`, `food_id` from the recent template.
- [ ] New entry gets a fresh id and appended `sortOrder` (existing add path /
      `addEntry` contract); Health Connect sync fires as for any add.
- [ ] Sheet dismisses on add (matches current add-from-search behavior).
- [ ] Daily totals + balance refresh reflect the added entry.
- [ ] Verify on device: tap recent → entry appears in correct slot with correct
      macros; totals update.
- [ ] Typecheck passes.

### US-005: Copy-meal-slot repository operation (data layer)
**Description:** As a developer, I need a repository method that duplicates one
meal slot's entries onto another date so the UI can offer "Copy to…".

**Acceptance Criteria:**
- [ ] Add `FoodDiaryRepository.copyMealToDate(sourceDate, mealType, targetDate)`
      + impl.
- [ ] Reads source slot entries; inserts a copy of each into
      `targetDate` / same `mealType`, in a single SQLDelight transaction.
- [ ] Copied entries get new ids and `sortOrder` **appended after** any existing
      entries already in the target slot (does not clobber/reorder them).
- [ ] All fields copied verbatim except `id`, `date`, `sortOrder`.
- [ ] No-op (no throw) when source slot is empty.
- [ ] Unit test (fake repo / `runSync`): copying N entries appends N to target,
      source unchanged, target's pre-existing entries preserved and first.
- [ ] Typecheck + tests pass.

### US-006: Copy-to-day UI action on a meal slot
**Description:** As a user, I want a "Copy to another day" action on a meal slot
so I can replicate it without re-entering each food.

**Acceptance Criteria:**
- [ ] Each meal slot (Breakfast/Lunch/Dinner/Snacks) exposes a "Copy to day"
      action (overflow/icon in the slot header), enabled only when the slot has
      ≥1 entry.
- [ ] Tapping it opens the existing date picker (`DatePickerDialog`) to choose
      the target day.
- [ ] On confirm, calls `copyMealToDate(currentDate, mealType, targetDate)` via
      the StateHolder; shows a confirmation snackbar
      `"Copied {Slot} to {targetDate}"`.
- [ ] Copying to the **same** day appends a duplicate set (allowed; no special
      guard required for MVP).
- [ ] Daily totals for the target day reflect the copy when navigated to.
- [ ] Verify on device: copy Lunch to tomorrow → tomorrow's Lunch shows the same
      entries; today unchanged.
- [ ] Typecheck passes.

### US-007: Wire copy action through StateHolder
**Description:** As a developer, I need a StateHolder entry point for copy so the
UI stays unidirectional.

**Acceptance Criteria:**
- [ ] `FoodDiaryStateHolder.copyMeal(sourceDate, mealType, targetDate): Job`
      launches on its scope and calls the repository.
- [ ] After completion, balance/total refresh callback fires (mirror `addEntry`
      usage in `MacroDailyCounterDetail`).
- [ ] Typecheck passes.

## 4. Functional Requirements

- **FR-1:** System must derive a list of distinct recently-logged foods from
  `FoodDiaryEntry`, most-recent first, deduped by `food_id` (fallback
  name+brand), each carrying its last-logged macros/serving.
- **FR-2:** Log Food sheet must offer a "Recent" tab alongside "Search".
- **FR-3:** Tapping a recent food must add it to the active meal slot/date in one
  tap, reusing the existing add path (new id, appended sortOrder, Health sync).
- **FR-4:** Each meal slot must offer a "Copy to day" action, enabled only when
  non-empty.
- **FR-5:** Copy must duplicate every entry of the source slot into the chosen
  target date's **same** meal slot, appended after existing entries, in one
  transaction, leaving the source untouched.
- **FR-6:** Copy and recent-add must both trigger daily-total + balance refresh.
- **FR-7:** Recents limit defaults to 20 and must be a single query parameter.

## 5. Non-Goals (Out of Scope)

- **Whole-day copy** (all four slots at once) — per-slot only this iteration.
- **Recent whole-meals** (re-adding a past slot as a group) — recents are
  individual foods only; grouped reuse is already served by Custom Meals.
- **Frequency / "most-eaten" ranking** — strictly most-recent-first.
- **Editing serving size from the Recent tab before adding** — one-tap uses the
  last-used serving as-is. (Editing remains available afterward via the existing
  edit-entry sheet.)
- **Cross-slot copy** (Lunch → Dinner) — target slot equals source slot.
- **New persistent table or schema migration** — derived from existing rows.
- **Pinning/favoriting or removing items from recents.**

## 6. Design Considerations

- **Recent tab:** reuse `FoodResultRow` visual language; tab/segmented control at
  top of `FoodSearchContent` in `FoodLogSheet.kt`. Keep the "Create custom food"
  affordance on the Search tab only.
- **Copy action:** place in the meal-slot header in `MacroDailyCounterDetail.kt`,
  next to the existing add/custom-meal affordances; reuse `DatePickerDialog` and
  the existing snackbar host.
- **One-tap add** mirrors the `onAddEntry` callback already used by
  `FoodSearchSheet`, so downstream wiring (Health sync, refresh) is unchanged.

## 7. Technical Considerations

- **Data source:** `FoodDiaryEntry` table + `FoodDiary.sq`. New queries:
  `recentFoods` (distinct, ordered, limited) and reuse of `insertEntry` +
  `maxSortOrderForDateMeal` for copy.
- **Distinctness in SQL:** use a subquery selecting `MAX(id)` grouped by the
  identity key, then join/filter the full rows; order by `date DESC, id DESC`.
- **Repository:** extend `FoodDiaryRepository` (+ `FoodDiaryRepositoryImpl`) and
  the in-memory fakes in `commonTest` (`FakeFoodDiaryRepository`) for the new
  methods — the existing reorder test already models this contract.
- **Copy transaction:** use `transaction {}` / `transactionWithResult {}`; compute
  starting sortOrder from `maxSortOrderForDateMeal(targetDate, mealType)`.
- **Concurrency:** all DB ops main-safe (`withContext(Dispatchers.Default)` /
  `.asFlow()`), per existing repo conventions.
- **StateHolder:** `FoodDiaryStateHolder` owns the new `recentFoods` flow and
  `copyMeal` side effect; UI stays unidirectional.
- **Tests:** commonTest with `runSync` + fakes (no SQLDelight driver in
  commonTest, matching `FoodDiaryReorderTest`).

## 8. Success Metrics

- Re-logging a previously eaten food takes **1 tap** from the Log Food sheet
  (vs. type-search-select-add today).
- Replicating a meal slot to another day takes **≤3 taps** (action → date →
  confirm) with **0** manual food entries.
- No regression in Log Food search, barcode, custom-food, or custom-meal flows;
  all existing unit tests stay green.

## 9. Open Questions

- Should the Recent tab dedupe across brands when `food_id` is null but names
  match loosely (e.g. "Chicken breast" vs "chicken breast, raw")? MVP uses exact
  normalized name+brand.
- Should copying to a day/slot that already contains identical entries warn about
  duplicates, or stay silent (current spec: silent)?
- Is most-recent-first the right long-term default, or should a later iteration
  add frequency ranking as a toggle?
- Should "Copy to day" later gain a target-slot picker (cross-slot copy) once the
  same-slot version ships?
