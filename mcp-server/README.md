# FitBro MCP server

Cloudflare Worker + D1 that lets Grok read and write the FitBro food diary and read everything else the app shows (expenditure, body measurements and trends, workouts, weekly targets, custom foods/meals), and that the app syncs with. See `tasks/prd-grok-mcp-integration.md`.

## Deploy (free tier)

```shell
npm install
npx wrangler login
npx wrangler d1 create fitbro                 # paste the database_id into wrangler.toml
npx wrangler d1 migrations apply fitbro --remote
npx wrangler kv namespace create OAUTH_KV     # paste the id into wrangler.toml
npx wrangler secret put MCP_TOKEN             # for Grok; use a long random value (openssl rand -hex 32)
npx wrangler secret put SYNC_TOKEN            # for the app; a different long random value
npx wrangler deploy
```

If you deploy under a different workers.dev subdomain, update `PUBLIC_ORIGIN` in `src/worker.ts` (it is the OAuth issuer and resource URL).

## Connect

- **App:** Settings → Cloud Sync → URL `https://fitbro-mcp.<you>.workers.dev`, token = `SYNC_TOKEN`.
- **Grok app / grok.com:** Connectors → New Connector → Custom → URL `https://fitbro-mcp.<you>.workers.dev/mcp`. A FitBro sign-in page opens; paste `MCP_TOKEN` and press Allow. (OAuth 2.1 with PKCE; Grok registers itself via dynamic client registration or a client ID metadata document.)
- **xAI API / scripts:** remote MCP server URL `…/mcp` with header `Authorization: Bearer <MCP_TOKEN>`.

## Endpoints

| Path | Auth | Purpose |
|---|---|---|
| `POST /mcp` | OAuth access token or `MCP_TOKEN` | MCP (Streamable HTTP, stateless). Diary: `get_daily_summary`, `get_entries`, `get_trends`, `log_food`, `update_entry`, `delete_entry`. Read-only mirror: `get_expenditure`, `get_goals` (+ weekly targets), `get_week_summary` (Home tab), `get_insights` (body trends + estimated expenditure, same maths as the app in `src/analytics.ts`), `get_body_measurements`, `get_workouts`, `get_custom_foods` |
| `POST /sync/push` | `SYNC_TOKEN` | App → cloud changes (last-write-wins on `updatedAt`) |
| `GET /sync/pull?since=N` | `SYNC_TOKEN` | Cloud → app changes after cursor `N`, incl. deletions |
| `PUT /sync/settings` | `SYNC_TOKEN` | Macro goals by weekday + preferences (week start, weekly targets, hidden workout types, excluded resistance apps) |
| `PUT /sync/expenditure` | `SYNC_TOKEN` | Daily intake/burn/balance computed by the app |
| `PUT /sync/body` | `SYNC_TOKEN` | Scale readings; replaces the pushed date range (app sends last 120 days) |
| `PUT /sync/workouts` | `SYNC_TOKEN` | Exercise sessions (+ detail); replaces the range, keeps stored detail when a session is sent without it (app sends last 90 days) |
| `PUT /sync/library` | `SYNC_TOKEN` | Custom foods and meals (whole snapshot) |
| `/.well-known/oauth-*`, `/register`, `/authorize`, `/token` | — | OAuth discovery, client registration, owner sign-in (5 wrong tokens per IP → 15 min lockout), token exchange |

## Develop

```shell
cp .dev.vars.example .dev.vars
npx wrangler d1 migrations apply fitbro --local
npm run dev        # http://localhost:8787
npm test           # vitest against node:sqlite
npm run typecheck
```
