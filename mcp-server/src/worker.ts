import { OAuthProvider, type OAuthHelpers } from "@cloudflare/workers-oauth-provider";
import { WebStandardStreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/webStandardStreamableHttp.js";
import { bearerToken, tokenMatches } from "./auth";
import { handleAuthorize } from "./authorize";
import { createServer } from "./mcp";
import { handleSync } from "./sync";

export interface Env {
  DB: D1Database;
  OAUTH_KV: KVNamespace;
  OAUTH_PROVIDER: OAuthHelpers;
  MCP_TOKEN: string;
  SYNC_TOKEN: string;
}

export const PUBLIC_ORIGIN = "https://fitbro-mcp.emmettyoung92.workers.dev";

const unauthorized = () =>
  new Response(JSON.stringify({ error: "unauthorized" }), {
    status: 401,
    headers: { "Content-Type": "application/json", "WWW-Authenticate": "Bearer" },
  });

/** Reached only with a valid OAuth access token or the static MCP_TOKEN (checked by the provider). */
const mcpHandler = {
  async fetch(request: Request, env: Env): Promise<Response> {
    // Stateless: a fresh server + transport per request. Grok may resend a session id from an
    // earlier response; there is no session to resume, so drop it (FR-22).
    const headers = new Headers(request.headers);
    headers.delete("mcp-session-id");
    const transport = new WebStandardStreamableHTTPServerTransport({
      sessionIdGenerator: undefined,
      enableJsonResponse: true,
    });
    await createServer(env.DB).connect(transport);
    return transport.handleRequest(new Request(request, { headers }));
  },
};

/** Everything outside /mcp: the sign-in page and the app's sync API. */
const appHandler = {
  async fetch(request: Request, env: Env): Promise<Response> {
    const path = new URL(request.url).pathname;
    if (path === "/authorize") return handleAuthorize(request, env);
    if (path.startsWith("/sync/")) {
      if (!(await tokenMatches(bearerToken(request), env.SYNC_TOKEN))) return unauthorized();
      return handleSync(request, env.DB, path);
    }
    return new Response("Not found", { status: 404 });
  },
};

/** FR-19/FR-20: OAuth for the Grok app, plus the static MCP_TOKEN for API clients. */
export function createWorker(origin: string) {
  const resource = `${origin}/mcp`;
  return new OAuthProvider<Env>({
    apiRoute: "/mcp",
    apiHandler: mcpHandler,
    defaultHandler: appHandler,
    authorizeEndpoint: "/authorize",
    tokenEndpoint: "/token",
    clientRegistrationEndpoint: "/register",
    clientIdMetadataDocumentEnabled: true,
    scopesSupported: ["diary"],
    resourceMetadata: { resource, authorization_servers: [origin] },
    resolveExternalToken: async ({ token, env }) =>
      (await tokenMatches(token, env.MCP_TOKEN)) ? { props: { userId: "owner" }, audience: resource } : null,
  });
}

