# PRD: Grok MCP Integration (Cloud Sync + MCP Server)

## Introduction

Let Grok read and write the FitBro food diary ("how was my protein this week?", "log 2 eggs for breakfast"). A free-tier Cloudflare Worker + D1 database hosts (a) an MCP server Grok connects to and (b) a sync API the app uses to mirror diary entries both ways. The phone remains the primary UI; D1 is the shared copy Grok operates on.

## Goals

- ~$0 infrastructure (Cloudflare Workers + D1 free tiers)
- Grok can query daily totals, entries, trends, and goals
- Grok can log, edit, and delete diary entries; changes reach the phone on next sync
- App changes reach D1 automatically after every local mutation
- Health Connect stays consistent (no duplicate records on edit)

## User Stories

### US-001: Worker scaffold + D1 schema
**Description:** As a developer, I need a deployable Worker with a D1 database holding diary entries, tombstones, and goals.

**Acceptance Criteria:**
- [x] `mcp-server/` package with `wrangler.toml` (D1 binding `DB`), `tsconfig.json`, typecheck script
- [x] `migrations/0001_init.sql` creates `entries` and `settings` tables (FR-1, FR-2)
- [x] Bearer auth: `/mcp` requires `MCP_TOKEN`, `/sync/*` requires `SYNC_TOKEN`, constant-time compare, 401 otherwise (FR-3)
- [x] `npm run typecheck` passes

### US-002: Sync API
**Description:** As the app, I need push/pull endpoints so local and cloud diaries converge.

**Acceptance Criteria:**
- [x] `POST /sync/push` applies changes with last-write-wins (FR-4)
- [x] `GET /sync/pull?since=N` returns changes after cursor incl. tombstones (FR-5)
- [x] `PUT /sync/settings` stores goals (FR-6)
- [x] Unit tests cover LWW and tombstones

### US-003: MCP tools
**Description:** As a Grok user, I want tools to read and edit my diary.

**Acceptance Criteria:**
- [x] Read tools: `get_daily_summary`, `get_entries`, `get_trends`, `get_goals` (FR-7)
- [x] Write tools: `log_food`, `update_entry`, `delete_entry` (FR-8)
- [x] Inputs validated with zod (FR-9)
- [x] MCP Inspector can list and call every tool

### US-004: Local sync schema
**Description:** As a developer, I need global ids, change timestamps, and an outbox on-device.

**Acceptance Criteria:**
- [ ] Migration `8.sqm` adds `uuid`, `updated_at`, backfills uuids, creates `SyncOutbox` (FR-10)
- [ ] Every local mutation stamps `updated_at` and enqueues an outbox row in the same transaction (FR-11)
- [ ] `applyRemoteChanges` applies LWW + tombstones without enqueuing (FR-12)
- [ ] Typecheck passes: compileDebugKotlinAndroid + compileKotlinIosSimulatorArm64

### US-005: SyncRepository + wiring
**Description:** As a user, I want the app to sync automatically.

**Acceptance Criteria:**
- [ ] `SyncApi` (Ktor) + `SyncRepository` push outbox → pull → apply → save cursor, serialized by a mutex (FR-13)
- [ ] First sync (no cursor) pushes all local rows (FR-14)
- [ ] Sync triggered after local mutations and on every app resume, which includes start (FR-15)
- [ ] Remotely-created/edited entries are written to Health Connect (FR-16)

### US-006: Cloud sync settings
**Description:** As a user, I want to configure the Worker URL and token in Settings.

**Acceptance Criteria:**
- [ ] Settings section "Cloud Sync": URL, token, "Sync now", last-synced status (FR-17)
- [ ] Sync is a no-op when URL or token is blank

### US-007: Health Connect dedupe
**Description:** As a user, I don't want edits to create duplicate Health Connect records.

**Acceptance Criteria:**
- [ ] `writeNutritionRecord` sets `clientRecordId = uuid`, `clientRecordVersion = updatedAt` (FR-18)

### US-008: Grok app connector (OAuth)
**Description:** As a user, I want to add FitBro as a custom connector in the Grok app / grok.com, which signs in with OAuth.

**Acceptance Criteria:**
- [x] OAuth discovery, dynamic client registration / CIMD, PKCE, token + refresh (FR-19)
- [x] Static `MCP_TOKEN` still works on `/mcp` (FR-20)
- [x] Sign-in page accepts `MCP_TOKEN`, rate-limited (FR-21)
- [x] Stale `Mcp-Session-Id` tolerated (FR-22)
- [x] Deployed with `OAUTH_KV` (FR-23); docs updated (FR-24)
- [ ] Grok app connects and can call `get_daily_summary` / `log_food`

## Functional Requirements

- FR-1: D1 `entries` table — `uuid` PK, all `FoodDiaryEntry` fields (snake_case), `updated_at`, `deleted`, `server_seq`; indexes on `date`, `server_seq`
- FR-2: D1 `settings` table — `key` PK, `value`, `updated_at`; stores calorie/protein/carb/fat goals
- FR-3: Static bearer tokens via `wrangler secret`: `MCP_TOKEN` (Grok), `SYNC_TOKEN` (app); constant-time compare
- FR-4: Push upserts a change only when incoming `updated_at` ≥ stored; every applied write gets the next `server_seq`
- FR-5: Pull returns rows with `server_seq > since` ordered ascending, plus the new cursor
- FR-6: Settings PUT replaces goal values
- FR-7: Read tools — daily summary (totals, goals, remaining), entries by date range, per-day trends with averages, goals
- FR-8: Write tools — log/update/delete set `updated_at = now`, delete writes a tombstone; all bump `server_seq`. Amounts are grams eaten (`amount_g`, stored as `servingSizeG`); changing only `amount_g` scales macros proportionally
- FR-9: Validation — `date` is `YYYY-MM-DD`, `meal_type` ∈ BREAKFAST/LUNCH/DINNER/SNACKS, macros ≥ 0
- FR-10: Local migration 8: `uuid TEXT` (unique, backfilled), `updated_at INTEGER NOT NULL DEFAULT 0`, `SyncOutbox(uuid PK, deleted, queued_at)`
- FR-11: add/update/delete/reorder/move/copy enqueue outbox rows transactionally
- FR-12: Remote apply uses LWW on `updated_at`; tombstones hard-delete locally; never enqueues (no echo)
- FR-13: Sync = push outbox → clear acked → pull since cursor → apply → persist cursor; one run at a time
- FR-14: Unset cursor ⇒ full push of all local entries
- FR-15: Triggers — after local mutations, and on `Lifecycle.Event.ON_RESUME` in `App.kt` (covers app start and returning to foreground on Android and iOS)
- FR-16: Pulled non-deleted entries are written to Health Connect / HealthKit
- FR-17: Sync URL/token stored in app-private settings (SharedPreferences / NSUserDefaults — not encrypted); cursor + last-synced persisted; changing the URL resets the cursor
- FR-18: Health Connect records keyed by `clientRecordId = uuid` (version = write time) so edits upsert. HealthKit (iOS) dedupe is out of scope

- FR-19: `@cloudflare/workers-oauth-provider` in front of `/mcp`: RFC 8414 + RFC 9728 metadata, DCR at `/register`, CIMD enabled, `/token` (S256 PKCE only), scope `diary`, resource `${PUBLIC_ORIGIN}/mcp`
- FR-20: `resolveExternalToken` accepts the static `MCP_TOKEN` with the same resource audience; `SYNC_TOKEN` is never accepted on `/mcp`
- FR-21: `/authorize` shows client name, verified domain (CIMD) and redirect host; owner proves identity by entering `MCP_TOKEN`; consent handle is single-use and browser-bound; 5 failures per IP → 15-minute lockout (KV); deny returns `access_denied`
- FR-22: `/mcp` drops any incoming `Mcp-Session-Id` (stateless server, nothing to resume)
- FR-23: KV namespace `OAUTH_KV` stores grants/tokens (hashed by the library)
- FR-24: README "Connect" section, CLAUDE.md MCP section

## Non-Goals

- No background/periodic sync (WorkManager / BGTask) — Grok entries appear when the app opens
- No FatSecret search tool for Grok (Grok estimates macros)
- No syncing of cardio, custom foods, custom meals, or macro profiles
- No multi-user accounts (OAuth sign-in is single-owner, gated by `MCP_TOKEN`)

## Technical Considerations

- Kotlin 2.3 → `kotlin.uuid.Uuid.random()` available in commonMain
- Ktor client + kotlinx-json already dependencies
- Schema version bumps 7→8: migration file is `8.sqm`
- Connect Grok via xAI API remote MCP tool: server URL `https://<worker>.workers.dev/mcp`, authorization `Bearer <MCP_TOKEN>`

## Open Questions

- Grok connector OAuth expectations are undocumented by xAI; first live connection will confirm redirect URI / scope handling.
