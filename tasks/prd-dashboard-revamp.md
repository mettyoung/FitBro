# PRD: Dashboard UX Revamp — Banner + 2-Column Grid

## Introduction

Replace the current Balance tab (3-way toggle + single scrollable view) with a new home layout: a full-width calorie balance banner followed by a 2-column card grid. Tapping a card navigates to a detail screen (chart + history) for that specific lens. The 3-way toggle is removed entirely — lens selection now happens by tapping a card.

## Goals

- At a glance: calorie balance + cardio total visible without any interaction
- Clear entry points for Intake and Expenditure detail via tappable cards
- Detail screen reuses existing chart + history components (no duplication)
- Back navigation from detail returns to the new home grid

## New Layout

```
[ HEADER: title + date navigator + refresh ]
[ BALANCE BANNER CARD  (full width)        ]
  • Net kcal balance (signed, green/red)
  • "🏃 X min cardio"

[ INTAKE CARD    ] [ EXPENDITURE CARD ]
  total kcal in      total kcal out
  (tap → detail)     (tap → detail)

[ ... future cards go here ... ]
```

Detail screen (replaces grid when a card is tapped):
```
[ HEADER: title + date navigator + refresh + back button ]
[ SlidingWindowInsightCard (chart + summary for lens)    ]
[ History rows (for lens)                                ]
```

## User Stories

### US-001: Balance home — banner + 2-column grid
**Description:** As a user, I want to see my calorie balance and key metrics at a glance from the main dashboard without switching tabs.

**Acceptance Criteria:**
- [ ] `DashboardViewMode` toggle removed from `DashboardContent`; 3-way segmented control deleted
- [ ] New `DashboardHome` composable in `DashboardScreen.kt` renders:
  1. `CalorieBalanceBannerCard` — full-width `ElevatedCard`; shows net balance value (e.g. "+320 kcal", green if ≥ 0, red if < 0) derived from sum of `balance` across all `balances`; shows `"🏃 X min"` (`weeklyCardioMinutes`) below the balance value in bodyMedium + MiTextSecondary
  2. `Row` of two equal-width `ElevatedCard` items (`Modifier.weight(1f)` each): left = Intake, right = Expenditure
- [ ] Intake card shows: title "Intake" (labelSmall, MiOrange), total kcal in (sum of `intake` across `balances`, bodyLarge, onSurface)
- [ ] Expenditure card shows: title "Expenditure" (labelSmall, MiOrange), total kcal out (sum of `burn` across `balances`, bodyLarge, onSurface)
- [ ] `DashboardContent` renders `DashboardHome` when `selectedLens == null`; `DashboardDetail` when `selectedLens != null` (US-002 wires this — placeholder lambda for now)
- [ ] `CardioSummaryRow` between insight card and history removed (cardio now in banner)
- [ ] `selectedLens: DashboardViewMode?` state hoisted in `DashboardContent` (initially null)
- [ ] Typecheck passes: compileDebugKotlinAndroid + compileKotlinIosSimulatorArm64

### US-002: Detail drill-down — chart + history for selected lens
**Description:** As a user, I want to tap an Intake or Expenditure card and see the full chart and daily history for that lens so I can analyze trends.

**Acceptance Criteria:**
- [ ] New `DashboardDetail` composable in `DashboardScreen.kt`: takes `balances`, `viewMode: DashboardViewMode` (INTAKE or EXPENDITURE), `onBack: () -> Unit`, and `onBarClick: (DailyBalance) -> Unit`
- [ ] `DashboardDetail` renders: back IconButton (Icons.AutoMirrored.Filled.ArrowBack, `MiOrange` tint) in the header row; `SlidingWindowInsightCard(balances, viewMode)`; history rows (existing `CondensedLogItem` list, same style, reverse-chronological) — all scoped to the selected `viewMode`
- [ ] Tapping Intake card sets `selectedLens = DashboardViewMode.INTAKE`; tapping Expenditure sets `selectedLens = DashboardViewMode.EXPENDITURE`
- [ ] Back button / system back sets `selectedLens = null`, returning to `DashboardHome`
- [ ] `BreakdownDialog` still opens on bar tap (unchanged)
- [ ] BALANCE `DashboardViewMode` is no longer used in the UI (the banner card shows balance natively); no need to handle BALANCE in `DashboardDetail`
- [ ] Typecheck passes: compileDebugKotlinAndroid + compileKotlinIosSimulatorArm64

## Functional Requirements

- FR-1: Remove 3-way toggle composable and `viewMode` state from `DashboardContent`; delete `DashboardViewModeToggle` usage (keep the enum — still used by chart/history internals)
- FR-2: `CalorieBalanceBannerCard`: net balance = `balances.sumOf { it.balance }.roundToInt()`; cardio = `weeklyCardioMinutes`; color MiGreen (0xFF4CAF50) if balance ≥ 0 else red (0xFFF44336) — both already used in `CondensedLogItem`
- FR-3: Intake total = `balances.sumOf { it.intake }.roundToInt()` kcal; Expenditure total = `balances.sumOf { it.burn }.roundToInt()` kcal
- FR-4: `selectedLens` hoisted in `DashboardContent`; `DashboardHome` receives `onLensSelected: (DashboardViewMode) -> Unit`; `DashboardDetail` receives `viewMode` + `onBack`
- FR-5: Cards use `ElevatedCard` with `onClick` lambda (Material3 clickable card); consistent padding 16.dp inside
- FR-6: History rows in `DashboardDetail` reuse existing `CondensedLogItem(balance, viewMode, onClick)` — no new component needed
- FR-7: `CardioSummaryRow` removed (replaced by banner)
- FR-8: `AnimatedContent` or simple `if/else` to switch between `DashboardHome` and `DashboardDetail` within `DashboardContent`

## Non-Goals

- No BALANCE detail screen (balance is shown inline in the banner, not as a drillable lens)
- No new chart type for the home screen cards (numbers only, no mini-charts in cards for now)
- No reordering or adding/removing grid cards (layout is fixed for now)
- No changes to Macros, Cardio, or Settings tabs
- No animation between home and detail beyond what `AnimatedContent` provides by default

## Design Considerations

- `ElevatedCard` with `tonalElevation = 2.dp` for grid cards; `tonalElevation = 4.dp` for the banner
- Banner card: large balance number in `displayMedium`, signed ("+320 kcal" or "-150 kcal"); cardio row below in `bodyMedium`
- Grid cards: equal width via `Row + weight(1f)`; height auto (wrap content); `Modifier.padding(8.dp)` gap between cards
- Back button replaces calendar icon position in header, or sits in the leading position of the header row

## Technical Considerations

- `DashboardViewMode.BALANCE` enum value stays (referenced by chart `diverging` param and `CondensedLogItem` branching) but `DashboardHome` never passes it to `onLensSelected`
- `selectedLens: DashboardViewMode?` is in-memory only (not persisted); resets to null on tab switch
- Existing `DashboardContent` signature unchanged from callers' perspective — `weeklyCardioMinutes` stays as a param
- `SlidingWindowInsightCard` already accepts `viewMode` — pass `INTAKE` or `EXPENDITURE` from `DashboardDetail` directly

## Success Metrics

- Net balance, cardio total, intake, and expenditure all visible without any tap from the home screen
- Drill-down to Intake or Expenditure detail in 1 tap
- No regression on Macros, Cardio, or Settings tabs
