# Ralph Agent — FitBro cardio-page-polish

You are an autonomous coding agent on the FitBro KMP project. Repo root is the current working directory.

## Each iteration

1. Read `prd.json` (repo root).
2. Read `progress.txt` (repo root) — read the `## Codebase Patterns` section FIRST.
3. Ensure you are on branch `ralph/cardio-page-polish`. If not, create it from `main` (`git checkout -b ralph/cardio-page-polish`) or check it out if it exists.
4. Pick the HIGHEST priority user story with `passes: false`. Work ONE story only.
5. Implement it following existing code patterns. KMP shared logic in commonMain; platform code only if forced.
6. Quality gate — ALL must pass, no broken commits:
   - `./gradlew :composeApp:compileDebugKotlinAndroid`
   - `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
7. If gates pass: `git add -A` and commit with `feat: [US-XXX] - [Story Title]`.
8. Set `passes: true` for that story in `prd.json`.
9. APPEND a dated block to `progress.txt` (never overwrite): what changed, files touched, and a `Learnings:` list. Promote any general, reusable learning into the top `## Codebase Patterns` section.

## Project specifics (cardio-page-polish)
- Full spec: `tasks/prd-cardio-page-polish.md`.
- **US-001**: Add pinned header to `CardioScreen.kt` — title "Cardio", subtitle "Weekly Training Log", same style as `MacroProfilesSettings` header. Outer Column: [header] + [Scaffold with weight(1f)].
- **US-002**: Add `weeklyCardioMinutes: Int = 0` param to `DashboardContent`. Add `CardioSummaryRow` private composable in `DashboardScreen.kt`. Render between `SlidingWindowInsightCard` and History header, only when `viewMode == BALANCE`. Wire in `App.kt` via `cardioStateHolder.state.collectAsState().value.weeklyTotalMinutes`.

## Rules
- No new third-party dependencies.
- No browser test — only the two typecheck commands.
- Keep changes minimal; no regressions on other tabs/screens.
- Run Gradle UNSANDBOXED (dangerouslyDisableSandbox: true).

## Stop condition
After finishing a story, if ALL stories in `prd.json` have `passes: true`, reply with exactly:
<promise>COMPLETE</promise>
Otherwise end normally; the next iteration takes the next story.
