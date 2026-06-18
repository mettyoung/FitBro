# Ralph Agent — FitBro dashboard-revamp

You are an autonomous coding agent on the FitBro KMP project. Repo root is the current working directory.

## Each iteration

1. Read `prd.json` (repo root).
2. Read `progress.txt` (repo root) — read the `## Codebase Patterns` section FIRST.
3. Ensure you are on branch `ralph/dashboard-revamp`. If not, create it from `main` (`git checkout -b ralph/dashboard-revamp`) or check it out if it exists.
4. Pick the HIGHEST priority user story with `passes: false`. Work ONE story only.
5. Implement it following existing code patterns. KMP shared logic in commonMain; platform code only if forced.
6. Quality gate — ALL must pass, no broken commits:
   - `./gradlew :composeApp:compileDebugKotlinAndroid`
   - `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
7. If gates pass: `git add -A` and commit with `feat: [US-XXX] - [Story Title]`.
8. Set `passes: true` for that story in `prd.json`.
9. APPEND a dated block to `progress.txt` (never overwrite): what changed, files touched, and a `Learnings:` list.

## Project specifics (dashboard-revamp)
- Full spec: `tasks/prd-dashboard-revamp.md`.
- **US-001**: Remove `DashboardViewModeToggle` usage from `DashboardContent`. Add `selectedLens: DashboardViewMode?` state (null = home). Add `DashboardHome` composable with `CalorieBalanceBannerCard` (full-width, net balance + cardio) and a `Row` of two `ElevatedCard` items (Intake, Expenditure totals). `DashboardContent` shows `DashboardHome` when `selectedLens==null`. Remove `CardioSummaryRow`.
- **US-002**: Add `DashboardDetail` composable with back button (ArrowBack, MiOrange), `SlidingWindowInsightCard(balances, viewMode)`, and `CondensedLogItem` history rows. Wire Intake/Expenditure card taps to set `selectedLens`. Wire back button to set `selectedLens = null`.

## Rules
- No new dependencies. No browser test. Run Gradle UNSANDBOXED.
- Never commit failing typechecks.

## Stop condition
If ALL stories `passes: true`, reply with exactly:
<promise>COMPLETE</promise>
Otherwise end normally.
