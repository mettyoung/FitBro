# FitBro MCP server

Cloudflare Worker + D1 that lets Grok read and write the FitBro food diary, and that the app syncs with. See `tasks/prd-grok-mcp-integration.md`.

## Deploy (free tier)

```shell
npm install
npx wrangler login
npx wrangler d1 create fitbro                 # paste the database_id into wrangler.toml
npx wrangler d1 migrations apply fitbro --remote
npx wrangler secret put MCP_TOKEN             # for Grok; use a long random value (openssl rand -hex 32)
npx wrangler secret put SYNC_TOKEN            # for the app; a different long random value
npx wrangler deploy
```

## Connect

- **App:** Settings → Cloud Sync → URL `https://fitbro-mcp.<you>.workers.dev`, token = `SYNC_TOKEN`.
- **Grok (xAI API):** remote MCP tool, server URL `https://fitbro-mcp.<you>.workers.dev/mcp`, authorization `Bearer <MCP_TOKEN>`.

## Endpoints

| Path | Auth | Purpose |
|---|---|---|
| `POST /mcp` | `MCP_TOKEN` | MCP (Streamable HTTP, stateless): `get_daily_summary`, `get_entries`, `get_trends`, `get_goals`, `log_food`, `update_entry`, `delete_entry` |
| `POST /sync/push` | `SYNC_TOKEN` | App → cloud changes (last-write-wins on `updatedAt`) |
| `GET /sync/pull?since=N` | `SYNC_TOKEN` | Cloud → app changes after cursor `N`, incl. deletions |
| `PUT /sync/settings` | `SYNC_TOKEN` | Macro goals by weekday |

## Develop

```shell
cp .dev.vars.example .dev.vars
npx wrangler d1 migrations apply fitbro --local
npm run dev        # http://localhost:8787
npm test           # vitest against node:sqlite
npm run typecheck
```
