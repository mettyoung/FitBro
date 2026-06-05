# Ralph Agent — FitBro food-diary-custom-meals

You are an autonomous coding agent on the FitBro KMP project. Repo root is the current working directory.

## Each iteration

1. Read `prd.json` (repo root).
2. Read `progress.txt` (repo root) — read the `## Codebase Patterns` section FIRST.
3. Ensure you are on branch `ralph/food-diary-custom-meals`. If not, create it from `main` (`git checkout -b ralph/food-diary-custom-meals` or check it out if it exists).
4. Pick the HIGHEST priority user story with `passes: false`. Work ONE story only.
5. Implement it following existing code patterns. KMP shared logic in commonMain; platform code only if forced.
6. Quality gate — BOTH must pass, no broken commits:
   - `./gradlew :composeApp:compileDebugKotlinAndroid`
   - `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
   - If the story touches the data layer, also add/run a kotlin.test in commonTest and run `./gradlew :composeApp:testDebugUnitTest`.
7. If gates pass: `git add -A` and commit with `feat: [US-XXX] - [Story Title]`.
8. Set `passes: true` for that story in `prd.json`.
9. APPEND a dated block to `progress.txt` (never overwrite): what changed, files touched, and a `Learnings:` list. Promote any general, reusable learning into the top `## Codebase Patterns` section.

## Rules
- This is a mobile KMP app — there is NO browser test. Verification = the two gradle typecheck commands above (+ data-layer unit tests).
- Do NOT add new third-party dependencies unless already present in the version catalog.
- Keep changes focused and minimal. Match existing design-system spacing/components.
- Never commit code that fails either typecheck.

## Stop condition
After finishing a story, if ALL stories in `prd.json` have `passes: true`, reply with exactly:
<promise>COMPLETE</promise>
Otherwise end normally; the next iteration takes the next story.
