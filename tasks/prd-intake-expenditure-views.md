# PRD: Intake & Expenditure Views on the Balance Screen

## Introduction

The dashboard's **Balance** tab currently shows one lens on a day: net caloric balance (intake − burn). Users who want to understand *why* the balance moved must open a per-day breakdown dialog. This feature adds two sibling lenses — **Intake** and **Expenditure** — selectable via a 3-way segmented toggle at the top of the Balance screen. Each lens reuses the existing chart + history-card layout but swaps the plotted value and the per-day breakdown shown in each history row.

This is a **presentation-only** change. No data-layer, repository, or persistence work: `DailyBalance` already carries every field needed (`intake`, `burn`, `balance`, `bmr`, `tef`, `neat`, `eat`, `proteinG`, `carbG`, `fatG`).

## Goals

- Let users switch the Balance screen between **Balance**, **Intake**, and **Expenditure** without leaving the tab.
- Intake view: surface where calories came from (carbs / protein / fat) per day, plus total kcal-in.
- Expenditure view: surface where calories went (BMR / TEF / Active energy) per day, plus total kcal-out.
- Reuse 100% of existing styling (cards, `RoundedCornerShape`, `MiOrange`, typography, dividers, chart shapes) so the three views feel like one screen.
- Zero changes to data sources, repositories, or the SQLDelight schema.

## User Stories

### US-001: 3-way segmented toggle + view-mode state
**Description:** As a user, I want a Balance | Intake | Expenditure toggle at the top of the Balance screen so I can switch lenses in one tap.

**Acceptance Criteria:**
- [ ] Add a `DashboardViewMode` enum: `BALANCE`, `INTAKE`, `EXPENDITURE`.
- [ ] Add a segmented-toggle composable rendered inside `DashboardContent`, below the date navigator and above the summary card.
- [ ] Toggle state is hoisted in `DashboardContent` via `remember { mutableStateOf(DashboardViewMode.BALANCE) }`; default = `BALANCE`.
- [ ] Selected segment uses `MiOrange` accent; unselected uses `MiTextSecondary` — matches existing pill/selection styling on the screen.
- [ ] With `BALANCE` selected, the screen renders identically to today (no visual regression).
- [ ] `compileDebugKotlinAndroid` + `compileKotlinIosSimulatorArm64` pass.
- [ ] On-device smoke: toggle switches the active segment highlight (see Technical Considerations — no dev-browser; this is KMP Android/iOS).

### US-002: Chart plots the active view's value
**Description:** As a user, I want the chart to reflect the lens I picked so the bars match the breakdown below.

**Acceptance Criteria:**
- [ ] `BALANCE` mode: chart unchanged — diverging bars around the zero line keyed on `balance` (+ orange / − tertiary).
- [ ] `INTAKE` mode: chart plots `intake` per day as upward-only bars (no negative half; always positive).
- [ ] `EXPENDITURE` mode: chart plots `burn` per day as upward-only bars.
- [ ] Chart is parameterized (e.g. a `valueSelector: (DailyBalance) -> Double` + a diverging/positive-only flag) rather than duplicated; day-initial labels and click-to-breakdown behavior preserved across all three modes.
- [ ] Bar height normalization for intake/expenditure uses the window max of the plotted value (not the balance max).
- [ ] Both compile gates pass.
- [ ] On-device smoke: bars visibly change when toggling modes.

### US-003: Summary insight card adapts per view
**Description:** As a user, I want the big number + footer on the summary card to describe the lens I'm viewing.

**Acceptance Criteria:**
- [ ] `BALANCE`: unchanged — "AVERAGE BALANCE", trend chip, "Total Week" / "Trend" footer.
- [ ] `INTAKE`: headline label "AVERAGE INTAKE", value = mean of `intake` over the window in kcal; footer total = sum of `intake`.
- [ ] `EXPENDITURE`: headline label "AVERAGE EXPENDITURE", value = mean of `burn`; footer total = sum of `burn`.
- [ ] Reuses existing `formatCalorieValue` and the card's typography/spacing; no new card style.
- [ ] Trend chip: keep balance-trend semantics for `BALANCE`; for intake/expenditure either hide the chip or show a neutral state (implementer's call — must not show a misleading green/red balance arrow).
- [ ] Both compile gates pass.

### US-004: Intake history rows — macro grams + total kcal-in
**Description:** As a user, in Intake view I want each day's history row to show carbs/protein/fat (grams) and the day's total calories in.

**Acceptance Criteria:**
- [ ] History list reuses the `CondensedLogItem` format/style, reverse-chronological (`balances.reversed()`), same Card + `HorizontalDivider` between rows.
- [ ] Left column: day abbreviation + day-of-month number (unchanged from Balance).
- [ ] Middle: three rows — Carbs / Protein / Fat — each with a colored dot (`ColorCarbs` / `ColorProtein` / `ColorFat`) and the value in **grams** (e.g. `120g carbs`), read from `carbG` / `proteinG` / `fatG`, rounded to whole grams.
- [ ] Right: total kcal-in for the day = `intake.roundToInt()` + " kcal", styled like the existing right-side value (not colored green/red — intake has no sign).
- [ ] Tapping a row still opens the existing `BreakdownDialog` (unchanged).
- [ ] Both compile gates pass.
- [ ] On-device smoke: Intake rows show three macro dots + grams + total kcal-in.

### US-005: Expenditure history rows — BMR/TEF/Active + total kcal-out
**Description:** As a user, in Expenditure view I want each day's history row to show BMR / TEF / Active energy and the day's total calories out.

**Acceptance Criteria:**
- [ ] Same reused `CondensedLogItem` layout, reverse-chronological, same Card/divider styling.
- [ ] Middle: three rows — BMR / TEF / Active — each with a colored dot and a kcal value: `bmr`, `tef`, and **Active = `neat + eat`**, each rounded to whole kcal.
- [ ] Dot colors: reuse existing palette consistently (e.g. BMR = `ColorProtein`, TEF = `ColorCarbs`, Active = `ColorFat`, matching the burn-breakdown coloring already used in `BreakdownDialog`).
- [ ] Right: total kcal-out for the day = `burn.roundToInt()` + " kcal", styled like the existing right-side value (no +/− sign, not green/red).
- [ ] BMR + TEF + Active sums to `burn` (sanity: `neat + eat + bmr + tef == burn`); if rounding drift appears, note it — do not fabricate a reconciliation field.
- [ ] Tapping a row opens the existing `BreakdownDialog` (unchanged).
- [ ] Both compile gates pass.
- [ ] On-device smoke: Expenditure rows show BMR/TEF/Active + total kcal-out.

## Functional Requirements

- FR-1: Introduce `DashboardViewMode { BALANCE, INTAKE, EXPENDITURE }`; default selection is `BALANCE`.
- FR-2: Render a 3-way segmented toggle in `DashboardContent`, between the date navigator and the summary card.
- FR-3: The toggle drives three coordinated regions: (a) the chart's plotted value, (b) the summary card's headline/footer, (c) the history rows' middle breakdown + right-side total.
- FR-4: Chart — `BALANCE` = diverging bars on `balance`; `INTAKE` = upward bars on `intake`; `EXPENDITURE` = upward bars on `burn`. Parameterize, do not fork.
- FR-5: Intake history row middle = Carbs/Protein/Fat in grams (`carbG`/`proteinG`/`fatG`) with `ColorCarbs`/`ColorProtein`/`ColorFat` dots; right = `intake` kcal.
- FR-6: Expenditure history row middle = BMR (`bmr`) / TEF (`tef`) / Active (`neat + eat`) in kcal; right = `burn` kcal.
- FR-7: Summary card headline = AVERAGE BALANCE / INTAKE / EXPENDITURE with the matching window mean; footer total = matching window sum.
- FR-8: All three views reuse existing components and design tokens — no new card, chart, or color styles introduced.

## Non-Goals (Out of Scope)

- No new bottom-nav tabs; the three views live behind the toggle on the existing Balance tab.
- No changes to `DailyBalance`, repositories, data sources, SQLDelight schema, or Health/intake plumbing.
- No changes to the per-day `BreakdownDialog` content (it already shows intake + burn breakdown).
- No per-view persistence of the selected toggle across app restarts (in-memory state is fine).
- No macro *goal* lines, targets, or percentages in the Intake view (that's the Macros tab's job).
- No date-range or navigator changes; the toggle does not affect the selected week.

## Design Considerations

- **Reuse:** `CalorieBalanceChart` (parameterize value selector + diverging flag), `CondensedLogItem` (parameterize middle + right content per view), `SlidingWindowInsightCard` (parameterize headline/footer). Keep one Card/list container.
- **Colors:** `MiOrange` (accent), `MiTextSecondary`, `ColorCarbs` `0xFFF72585`, `ColorProtein` `0xFF4361EE`, `ColorFat` `0xFF4CC9F0` — defined in `ui/Theme.kt`.
- **Toggle styling:** match the existing rounded pill / `RoundedCornerShape(16.dp)` background used by the date navigator; selected segment = `MiOrange`.
- **Sign convention:** Intake and Expenditure totals are always non-negative — render plain (no `+`/`−`, no green/red). Only Balance keeps the signed, color-coded value.

## Technical Considerations

- Files in scope: `ui/dashboard/DashboardScreen.kt` (toggle + per-view wiring of card/history), `ui/dashboard/CalorieBalanceChart.kt` (parameterize). A new small file for the toggle composable and `DashboardViewMode` enum is acceptable. `BreakdownDialog.kt` is reference-only (color/label parity) — not modified.
- The chart currently splits into positive (top) / negative (bottom) halves around a center zero line. Intake/expenditure are positive-only — render as a single upward bar (reuse the top-half path, skip the bottom half) to avoid a second chart implementation.
- **Verification:** This is KMP Compose Multiplatform (Android + iOS) — there is no browser. Per project testing gap, typecheck ≠ working. Build & install to the device and visually confirm each view; gradle runs **unsandboxed** with `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home`. No new table/column → migration gate not required here.
- StateHolder (`DashboardStateHolder`) needs no changes — the toggle is pure UI state over the already-loaded `balances`.

## Success Metrics

- A user can switch Balance → Intake → Expenditure in a single tap each, with chart + history updating together.
- Intake rows correctly show `carbG`/`proteinG`/`fatG` grams and `intake` kcal; Expenditure rows show `bmr`/`tef`/(`neat+eat`) and `burn` kcal.
- No visual regression to the existing Balance view when `BALANCE` is selected.

## Open Questions

- Trend chip in Intake/Expenditure: hide entirely, or repurpose as an intake/expenditure trend? (Default assumption: hide to avoid misleading balance semantics.)
- Should the segmented toggle remember the last-selected view within a session as the user navigates away and back to the Balance tab? (Default: resets to `BALANCE`; cheap to make sticky later.)
