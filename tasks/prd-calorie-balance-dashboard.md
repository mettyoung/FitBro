# PRD: Calorie Balance Dashboard

## Introduction

The Calorie Balance Dashboard provides users with a daily snapshot of their energy balance: total calorie intake minus total calorie expenditure. Data comes from two sources: Cronometer (food logging and basal metabolic rate/thermic effect data) and Mi Band (activity data via Health Connect/HealthKit). The dashboard allows users to track their nutrition and activity over a 7-day period with interactive visualizations and the ability to drill down into individual calorie components.

## Goals

- Display daily calorie balance (intake - burn) for the last 7 days
- Integrate Cronometer API for food intake and metabolic data (BMR, TEF)
- Integrate Health Connect (Android) / HealthKit (iOS) for Mi Band activity data (NEAT, EAT)
- Provide offline fallback using cached sync data
- Allow manual data refresh with detailed error reporting
- Enable interactive chart drill-down to view component breakdowns (intake, NEAT, EAT, BMR, TEF)
- Support calendar-based date selection for 7-day windows

## User Stories

### US-001: Set up Cronometer OAuth authentication
**Description:** As a developer, I need to establish OAuth flow with Cronometer API so users can securely authenticate and grant access to their food logging and metabolic data.

**Acceptance Criteria:**
- [ ] OAuth 2.0 flow implemented with Cronometer endpoints
- [ ] Credentials securely stored in platform keychain (Android Keystore / iOS Keychain)
- [ ] Token refresh logic handles expired tokens gracefully
- [ ] Typecheck passes
- [ ] Token persists across app restarts

### US-002: Fetch calorie intake and metabolic data from Cronometer
**Description:** As a developer, I need to fetch daily calorie intake (food), BMR, and TEF from Cronometer API so these values populate the dashboard calculations.

**Acceptance Criteria:**
- [ ] Ktor HTTP client configured for Cronometer API
- [ ] Parse response JSON into domain models: `DailyIntake { date, totalCalories }` and `Metabolism { date, bmr, tef }`
- [ ] Handle API errors and rate limits gracefully
- [ ] Fetch covers 7-day date range
- [ ] Typecheck passes
- [ ] Network calls are non-blocking (coroutine-based)

### US-003: Bridge Health Connect (Android) and HealthKit (iOS) for Mi Band data
**Description:** As a developer, I need to read NEAT and EAT (activity energy expenditure) from platform health APIs so I can include Mi Band data in calorie burn calculations.

**Acceptance Criteria:**
- [ ] Android: Health Connect integration using expect/actual pattern
- [ ] iOS: HealthKit integration reading HKQuantityTypeIdentifierActiveEnergyBurned
- [ ] Parse and map activity data to `ActivityBurn { date, neat, eat }`
- [ ] Request appropriate permissions (Android: HEALTH_CONNECT_READ; iOS: Health read)
- [ ] Typecheck passes
- [ ] Handle permission denial gracefully

### US-004: Implement local caching for sync data
**Description:** As a developer, I need to cache synced data locally so the dashboard works offline and provides fallback when API calls fail.

**Acceptance Criteria:**
- [ ] Store latest successful sync for each data source (Cronometer, Health data) with timestamp
- [ ] Cache keyed by date range (7-day window)
- [ ] Clear cache when user signs out
- [ ] Timestamp visible in UI to indicate freshness
- [ ] Typecheck passes

### US-005: Create CalorieMath repository for balance calculation
**Description:** As a developer, I need a centralized repository that computes daily calorie balance from all sources so the calculation logic is testable and reusable.

**Acceptance Criteria:**
- [ ] Function: `computeDailyBalance(intake: DailyIntake, metabolism: Metabolism, activity: ActivityBurn) -> DailyBalance`
- [ ] `DailyBalance { date, intake, burn, balance }` where `burn = NEAT + EAT + BMR + TEF`
- [ ] Handle missing data gracefully (e.g., if Mi Band data unavailable, show intake - (BMR + TEF))
- [ ] Sealed class error type for invalid inputs
- [ ] Unit tests covering edge cases (zero values, negative balance, missing components)
- [ ] Typecheck passes

### US-006: Build dashboard state holder with refresh logic
**Description:** As a developer, I need a shared state holder that manages the dashboard's data pipeline so loading, errors, and cached fallback are coordinated.

**Acceptance Criteria:**
- [ ] StateFlow-based state holder: `DashboardState { uiState, lastSyncTime, errorMessage, selectedDateRange }`
- [ ] `uiState` is sealed: `Loading | Success(data) | Error(message)`
- [ ] Manual refresh function triggers Cronometer + Health API calls in parallel
- [ ] On API failure: fall back to cached data, set error message visible in UI
- [ ] Track last successful sync timestamp from each source
- [ ] Sync logic is main-safe (uses Dispatchers.Default for computation)
- [ ] Typecheck passes

### US-007: Display 7-day calorie balance chart
**Description:** As a user, I want to see my daily calorie balance over the last 7 days in a clear chart so I can spot trends.

**Acceptance Criteria:**
- [ ] Bar or line chart showing balance for each day (positive = surplus, negative = deficit)
- [ ] X-axis: date labels (Mon, Tue, etc.)
- [ ] Y-axis: calorie values with zero line marked
- [ ] Positive bars colored green, negative red
- [ ] Responsive layout on phone and tablet
- [ ] Typecheck passes
- [ ] Verify in browser using dev-browser skill

### US-008: Add date range picker for 7-day window navigation
**Description:** As a user, I want to select different date ranges (previous week, next week) so I can review balance data for any 7-day period.

**Acceptance Criteria:**
- [ ] Calendar icon button opens date picker modal
- [ ] Picker allows selecting start date; automatically shows 7-day window
- [ ] Prev/Next week buttons for quick navigation
- [ ] Selected range displayed in header (e.g., "May 1 - May 7")
- [ ] Chart updates immediately on date selection
- [ ] Cannot select future dates
- [ ] Typecheck passes
- [ ] Verify in browser using dev-browser skill

### US-009: Implement chart drill-down for component breakdown
**Description:** As a user, I want to click on a day in the chart and see the breakdown of intake, NEAT, EAT, BMR, and TEF so I understand what drives the balance.

**Acceptance Criteria:**
- [ ] Tapping/clicking a bar opens bottom sheet or modal with breakdown
- [ ] Shows: Intake (from Cronometer), NEAT, EAT (from Health), BMR, TEF
- [ ] Each component labeled with calorie value and percentage of total burn
- [ ] Visual bar chart within modal showing relative sizes
- [ ] Close button or swipe to dismiss
- [ ] Typecheck passes
- [ ] Verify in browser using dev-browser skill

### US-010: Display sync status and last refresh time
**Description:** As a user, I want to see when data was last synced and know if I'm viewing fresh or cached data so I can trust the numbers.

**Acceptance Criteria:**
- [ ] "Last synced: 2 hours ago" or timestamp displayed at bottom of dashboard
- [ ] "Offline" badge shown if using cached data (no recent successful sync)
- [ ] "Syncing..." indicator during active API calls
- [ ] Typecheck passes
- [ ] Verify in browser using dev-browser skill

### US-011: Add manual refresh with error reporting
**Description:** As a user, I want to manually trigger a data refresh and see detailed error messages if it fails so I can diagnose connection issues.

**Acceptance Criteria:**
- [ ] Refresh button (circular arrow icon) in header
- [ ] Clicking shows loading spinner during sync
- [ ] On success: "Data updated" toast, last-synced timestamp updates
- [ ] On failure: modal/dialog showing error details (e.g., "Cronometer: Auth expired", "Health Connect: Permission denied")
- [ ] Error shows which data source failed (Cronometer? Health?)
- [ ] Suggests action (e.g., "Re-login to Cronometer" or "Grant Health Connect permission")
- [ ] Typecheck passes
- [ ] Verify in browser using dev-browser skill

### US-012: Handle missing or incomplete data gracefully
**Description:** As a developer, I need the dashboard to handle cases where one data source is unavailable so the app doesn't crash and users see partial data where possible.

**Acceptance Criteria:**
- [ ] If Cronometer unavailable: show Health data only (NEAT + EAT + BMR estimate)
- [ ] If Health data unavailable: show Cronometer data only (intake, BMR, TEF; no activity)
- [ ] Show warning banners for missing data (e.g., "Activity data unavailable")
- [ ] Display best-effort balance with caveat message
- [ ] Typecheck passes
- [ ] Unit tests cover all missing-data scenarios

## Functional Requirements

- **FR-1:** Cronometer OAuth: User can authenticate once; token persists and auto-refreshes
- **FR-2:** Cronometer API: Fetch daily food intake, BMR, TEF for a date range
- **FR-3:** Health Connect (Android): Read NEAT and EAT from device activity; request HEALTH_CONNECT_READ permission
- **FR-4:** HealthKit (iOS): Read HKQuantityTypeIdentifierActiveEnergyBurned; request HealthKit authorization
- **FR-5:** CalorieMath: `balance = intake - (NEAT + EAT + BMR + TEF)` for each day
- **FR-6:** Local caching: Store latest synced data per source; include timestamp
- **FR-7:** Offline mode: Dashboard displays cached data if sync fails; clear indication shown to user
- **FR-8:** Dashboard chart: 7-day bar/line chart; positive=green, negative=red
- **FR-9:** Date picker: Select any 7-day range; prev/next week navigation; cannot pick future dates
- **FR-10:** Drill-down: Click/tap bar to see component breakdown (Intake, NEAT, EAT, BMR, TEF)
- **FR-11:** Sync indicator: Show "Last synced: X minutes ago"; badge for offline state
- **FR-12:** Manual refresh: Button to trigger sync; show spinner during fetch; display errors with actionable messages
- **FR-13:** Error recovery: If one source fails, display partial data; suggest fix (e.g., "Re-authenticate Cronometer")

## Non-Goals

- **No automated syncing in background.** Sync only on user action (app open or manual refresh).
- **No notifications or alerts** based on calorie balance (e.g., no "deficit warning").
- **No meal-level drill-down.** Dashboard shows daily aggregate only; individual meals visible in Cronometer app.
- **No multi-user or account switching.** Single user per device.
- **No data export or sharing.**
- **No goal-setting or targets.** This is observation only; targets can be added later.

## Design Considerations

- **Chart library:** Use Compose Charts (or similar KMP-compatible library) for shared chart across Android/iOS
- **Color scheme:** Green for surplus (positive balance), red for deficit (negative), neutral gray for zero
- **Typography:** Large date labels, smaller component values in drill-down modal
- **Spacing:** Generous padding for mobile; responsive behavior for tablet (e.g., larger chart, side-by-side layouts)
- **Accessibility:** Tap targets ≥44pt; color-blind friendly palette; alt text for chart data
- **Reusable components:** Modal/bottom sheet for drill-down; date picker; sync status badge

## Technical Considerations

- **KMP source sets:** 
  - `commonMain`: CalorieMath logic, state holder, repository interfaces, Ktor client setup
  - `androidMain`: Health Connect bridge, keystore for credentials
  - `iosMain`: HealthKit bridge, Keychain for credentials
- **State management:** StateFlow in common source; ViewModel/shared presenter pattern
- **Coroutines:** Parallel fetch of Cronometer + Health data; use `coroutineScope { async { ... } }`
- **Error handling:** Sealed class `DataError { CronomerterAuth, CronomerterNetwork, HealthPermission, HealthUnavailable, ... }`
- **Performance:** Chart re-renders only on data change; date picker picker memoized
- **Testing:** Unit tests for CalorieMath; mock Ktor client for API tests; platform-agnostic state holder tests
- **API rate limits:** Cronometer likely has per-user rate limits; cache aggressively; show sync time to avoid hammering API

## Success Metrics

- User can view 7-day calorie balance in under 2 seconds (cached) or 5 seconds (fresh sync)
- Drill-down modal opens within 300ms
- Error messages are actionable (user can fix the issue without support)
- Dashboard works offline with ≥90% of data available (cached)
- No crashes on missing data (Health unavailable, Cronometer 403, etc.)

## Open Questions

1. What is the priority if Cronometer and Health have different timestamps for the same day? (e.g., Cronometer data updated at 11pm, Health updated at 10am)
2. Should the BMR estimate from Health be used if Cronometer BMR is unavailable, or only use Cronometer?
3. Do we need to handle timezone offsets (e.g., user traveling, syncing data from different timezones)?
4. Should we pre-fetch 14 days of data for smoother navigation, or fetch on-demand per 7-day range?
5. Is there a specific Cronometer API documentation link or do we need to reverse-engineer from their web app?
