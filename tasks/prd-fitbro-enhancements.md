# PRD: FitBro Health Sync, Barcode Scanning & Icon Branding

## Introduction

Extend FitBro's nutrition tracking with three major improvements: (1) sync food diary data to HealthConnect/HealthKit through an abstraction layer, enabling balance dashboard to read nutrition data from health platforms; (2) add barcode scanning to food search for faster entry; (3) implement FitBro branding icon (buff guy silhouette) across app; (4) default food search results to actual serving sizes instead of 100g. These changes improve data interoperability, user experience, and app identity.

## Goals

- Establish bidirectional sync with HealthConnect/HealthKit via abstraction layer (future-proof for additional platforms)
- Balance dashboard reads nutrition data from HealthConnect indirectly (not only from HealthConnect, maintaining FitBro data ownership)
- Enable barcode scanning for fast food lookup with fallback to manual entry
- Default food search results to actual serving sizes as provided by datasource
- Establish FitBro visual identity with buff guy icon/mascot across UI
- User can adjust quantity/servings after auto-defaulting to serving size

## User Stories

### US-001: Create health platform abstraction layer
**Description:** As a developer, I need abstraction for health platform APIs so FitBro can sync to HealthConnect, HealthKit, and future platforms without code duplication.

**Acceptance Criteria:**
- [ ] Define `HealthPlatformProvider` interface with `writeMeal()`, `syncNutrition()` methods
- [ ] Implement HealthConnect provider (Android)
- [ ] Implement HealthKit provider (iOS)
- [ ] No platform-specific code in food diary UI layer
- [ ] Typecheck passes

### US-002: Write food diary entries to HealthConnect/HealthKit
**Description:** As a user, I want food entries from FitBro synced to my phone's health app so nutrition data centralizes across apps.

**Acceptance Criteria:**
- [ ] Food diary "add meal" saves to FitBro DB and writes to HealthConnect/HealthKit
- [ ] Writes include: date, time, food name, calories, macros (protein, carbs, fat)
- [ ] Sync succeeds silently; show error toast on failure
- [ ] Retry mechanism for failed syncs
- [ ] Typecheck passes

### US-003: Balance dashboard reads from HealthConnect indirectly
**Description:** As a user, I want balance dashboard to reflect both FitBro entries and HealthConnect data so I see unified nutrition totals.

**Acceptance Criteria:**
- [ ] Balance dashboard queries FitBro DB first (source of truth)
- [ ] FitBro aggregates HealthConnect/HealthKit entries for display (read-only integration)
- [ ] Totals combine FitBro entries + synced health platform entries
- [ ] Handle duplicate detection (same meal logged in both apps)
- [ ] Typecheck passes

### US-004: Create FitBro icon (buff guy silhouette)
**Description:** As a designer/user, I want FitBro visual identity established with recognizable icon so app feels branded.

**Acceptance Criteria:**
- [ ] Design buff guy silhouette icon (vector, exportable as SVG/PNG at multiple sizes)
- [ ] Provide icon in 1x, 2x, 3x resolutions for Android; 1x, 2x, 3x for iOS
- [ ] App launcher icon updated to buff guy
- [ ] Icon added to Compose design system as `FitroBroIcon` composable
- [ ] Design review approved

### US-005: Integrate FitBro icon into app UI
**Description:** As a user, I want to see FitBro branding throughout app so it feels cohesive and branded.

**Acceptance Criteria:**
- [ ] App header/toolbar shows FitBro icon + app name
- [ ] Food diary screen shows icon in header or prominent location
- [ ] Balance dashboard header displays icon
- [ ] Button accents use icon (e.g., "Add meal" button with icon)
- [ ] Icon color variants (light/dark mode) defined
- [ ] Typecheck passes
- [ ] Verify in browser using dev-browser skill

### US-006: Add barcode scanning to food search
**Description:** As a user, I want to scan food barcodes for instant lookup so adding meals is faster than typing.

**Acceptance Criteria:**
- [ ] Camera permission request on first scan attempt
- [ ] Barcode scanner UI integrated into food search screen
- [ ] Scan → query Open Food Facts API for barcode match
- [ ] Match found: populate food name, serving size, macros; proceed to quantity selection
- [ ] No match: show "Food not found" and fallback to manual search/entry
- [ ] Handle camera permission denial gracefully
- [ ] Typecheck passes
- [ ] Verify in browser using dev-browser skill

### US-007: Default food search results to serving size
**Description:** As a user, I want search results to show actual serving sizes (not 100g) so macros are accurate without manual adjustment.

**Acceptance Criteria:**
- [ ] Food search query returns `servingSize` field from datasource (if available)
- [ ] Default food quantity to serving size, not 100g
- [ ] Show serving size label (e.g., "1 serving = 150g")
- [ ] User can adjust quantity slider (e.g., "2 servings") and macros update accordingly
- [ ] Fallback to 100g for foods without serving size data
- [ ] Typecheck passes
- [ ] Verify in browser using dev-browser skill

### US-008: Serve size adjustment UX
**Description:** As a user, I want to easily adjust serving quantity after selecting a food so I can log accurate portions.

**Acceptance Criteria:**
- [ ] Quantity selector shows servings (e.g., "1.5 servings") instead of grams
- [ ] Slider or +/- buttons to adjust servings
- [ ] Macros recalculate in real-time as quantity changes
- [ ] Show both serving count and gram equivalent (e.g., "1.5 servings (225g)")
- [ ] Typecheck passes
- [ ] Verify in browser using dev-browser skill

## Functional Requirements

- FR-1: Define `HealthPlatformProvider` interface with platform-agnostic meal/nutrition write methods
- FR-2: Implement HealthConnect provider for Android with calorie + macro sync
- FR-3: Implement HealthKit provider for iOS with calorie + macro sync
- FR-4: Food diary "add meal" flow writes to FitBro DB and health platform simultaneously
- FR-5: Failed health platform writes trigger retry queue (sync later if offline)
- FR-6: Balance dashboard sources nutrition from FitBro DB + aggregates HealthConnect/HealthKit reads
- FR-7: Detect and handle duplicate meal entries (same meal in FitBro and health app on same date/time)
- FR-8: Barcode scanner UI accessible from food search screen
- FR-9: Barcode scan queries Open Food Facts API; fallback to manual search on no match
- FR-10: Camera permission request + denial handling
- FR-11: Food search datasource returns `servingSize` field; default UI quantity to serving size, not 100g
- FR-12: Quantity selector works in servings (multiplier of serving size) not raw grams
- FR-13: Macros recalculate in real-time as serving quantity changes
- FR-14: FitBro icon integrated as app launcher icon, toolbar icon, and design-system composable
- FR-15: Icon supports light/dark mode variants

## Non-Goals

- No manual HealthConnect/HealthKit entry from balance dashboard (read-only integration)
- No sync conflict resolution UI (auto-detect, don't show duplicate picker)
- No barcode API failover to multiple services (Open Food Facts only; manual fallback is acceptable)
- No custom serving size definitions (use datasource serving size as-is)
- No scheduled background syncs (sync on user action only)
- No historical data migration to HealthConnect

## Design Considerations

- **Icon design:** Buff guy silhouette should be simple, recognizable at small sizes (32px+), work in mono + color variants
- **Barcode UX:** Scanner should feel integrated, not modal/jarring; show preview frame with scan indicator
- **Serving size:** Maintain backward compatibility — existing logged meals still show 100g if that's what was recorded
- **Health platform data:** Treat as read-only aggregation layer; FitBro DB remains source of truth for user's FitBro entries

## Technical Considerations

- **HealthConnect/HealthKit:** Use official SDKs (androidx.health:health-connect for Android; HealthKit framework for iOS)
- **Barcode scanning:** Use ML Kit or CameraX + barcode detection library (KMP-compatible if available)
- **Open Food Facts API:** Rate-limit awareness; cache barcode lookups locally to reduce API calls
- **Abstraction layer:** Place `HealthPlatformProvider` in shared KMP code; platform implementations in Android/iOS source sets
- **Serving size:** Ensure datasource returns serving size metadata; gracefully fall back to 100g if missing
- **Sync reliability:** Queue failed writes; retry on next app launch or user action

## Success Metrics

- Food diary entries sync to HealthConnect/HealthKit within 2 seconds of logging
- Balance dashboard reflects HealthConnect data within 30 seconds (lazy refresh acceptable)
- Barcode scan → food found in <3 seconds (UI responsive even if Open Food Facts is slow)
- Users default to serving size, reducing manual adjustments by 70%+
- App feels branded; icon appears in ≥3 prominent UI locations

## Open Questions

- Should FitBro sync meal deletions to HealthConnect (remove entries that user deletes in FitBro)?
- Should balance dashboard show which entries came from HealthConnect vs. FitBro app?
- Open Food Facts API key: use public or request branded key?
- Should barcode scanner cache results locally or query API each time?
- Serving size multiplier: integer only (1, 2, 3 servings) or fractional (0.5, 1.5, 2.5)?
