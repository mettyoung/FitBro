# Ralph Agent — FitBro intake-expenditure-views

You are an autonomous coding agent on the FitBro KMP project. Repo root is the current working directory.

## Each iteration

1. Read `prd.json` (repo root).
2. Read `progress.txt` (repo root) — read the `## Codebase Patterns` section FIRST.
3. Ensure you are on branch `ralph/intake-expenditure-views`. If not, create it from `main` (`git checkout -b ralph/intake-expenditure-views` or check it out if it exists).
4. Pick the HIGHEST priority user story with `passes: false`. Work ONE story only.
5. Implement it following existing code patterns. KMP shared logic in commonMain; platform code only if forced.
6. Quality gate — ALL must pass, no broken commits:
   - `./gradlew :composeApp:compileDebugKotlinAndroid`
   - `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
7. If gates pass: `git add -A` and commit with `feat: [US-XXX] - [Story Title]`.
8. Set `passes: true` for that story in `prd.json`.
9. APPEND a dated block to `progress.txt` (never overwrite): what changed, files touched, and a `Learnings:` list. Promote any general, reusable learning into the top `## Codebase Patterns` section.

## Project specifics (intake-expenditure-views)
- Full spec: `tasks/prd-intake-expenditure-views.md`. Feature = add Intake + Expenditure lenses to the Balance screen via a 3-way segmented toggle.
- **Presentation-only.** NO data-layer, repository, StateHolder, or SQLDelight changes. `DailyBalance` already carries `intake`, `burn`, `balance`, `bmr`, `tef`, `neat`, `eat`, `proteinG`, `carbG`, `fatG`.
- Files in scope: `ui/dashboard/DashboardScreen.kt` (toggle + per-view wiring of card/history), `ui/dashboard/CalorieBalanceChart.kt` (parameterize value selector + diverging flag). A new small file for the toggle composable + `DashboardViewMode` enum is fine. `BreakdownDialog.kt` is reference-only (color/label parity) — do NOT modify it.
- Reuse existing components & tokens: `SlidingWindowInsightCard`, `CondensedLogItem`, `CalorieBalanceChart`, `formatCalorieValue`; colors `MiOrange`, `MiTextSecondary`, `ColorCarbs` (0xFFF72585), `ColorProtein` (0xFF4361EE), `ColorFat` (0xFF4CC9F0) in `ui/Theme.kt`.
- Key decisions: Intake macro breakdown in GRAMS; Active energy = `neat + eat`; intake/expenditure totals are unsigned (no +/-, not green/red); toggle defaults to BALANCE and is in-memory only.
- Parameterize (`DashboardViewMode`) — do NOT fork the chart or the history row into copies.

## Rules
- This is a mobile KMP app — there is NO browser test. Verification = the two typecheck commands above. Ignore any "dev-browser" wording in acceptance criteria.
- Do NOT add new third-party dependencies.
- Keep changes focused and minimal. Match existing design-system spacing/components. With BALANCE selected, the screen must look identical to today (no regression).
- Never commit code that fails any typecheck.

## Stop condition
After finishing a story, if ALL stories in `prd.json` have `passes: true`, reply with exactly:
<promise>COMPLETE</promise>
Otherwise end normally; the next iteration takes the next story.
