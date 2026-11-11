import { WebStandardStreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/webStandardStreamableHttp.js";
import { createServer } from "./mcp";
import { handleSync } from "./sync";

export interface Env {
  DB: D1Database;
  MCP_TOKEN: string;
  SYNC_TOKEN: string;
}

const encoder = new TextEncoder();

/** FR-3: constant-time bearer check; an unset/short secret never authorizes. */
async function authorized(request: Request, secret: string | undefined): Promise<boolean> {
  if (!secret || secret.length < 16) return false;
  const header = request.headers.get("Authorization") ?? "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : "";
  // Hash both sides so lengths match before the timing-safe compare.
  const [a, b] = await Promise.all([
    crypto.subtle.digest("SHA-256", encoder.encode(token)),
    crypto.subtle.digest("SHA-256", encoder.encode(secret)),
  ]);
  const x = new Uint8Array(a);
  const y = new Uint8Array(b);
  let diff = 0;
  for (let i = 0; i < x.length; i++) diff |= x[i] ^ y[i];
  return diff === 0;
}

const unauthorized = () =>
  new Response(JSON.stringify({ error: "unauthorized" }), {
    status: 401,
    headers: { "Content-Type": "application/json", "WWW-Authenticate": "Bearer" },
  });

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const path = new URL(request.url).pathname;

    if (path === "/mcp") {
      if (!(await authorized(request, env.MCP_TOKEN))) return unauthorized();
      // Stateless: a fresh server + transport per request.
      const transport = new WebStandardStreamableHTTPServerTransport({
        sessionIdGenerator: undefined,
        enableJsonResponse: true,
      });
      await createServer(env.DB).connect(transport);
      return transport.handleRequest(request);
    }

    if (path.startsWith("/sync/")) {
      if (!(await authorized(request, env.SYNC_TOKEN))) return unauthorized();
      return handleSync(request, env.DB, path);
    }

    return new Response("Not found", { status: 404 });
  },
} satisfies ExportedHandler<Env>;
