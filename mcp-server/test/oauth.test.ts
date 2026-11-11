import { createHash, randomBytes } from "node:crypto";
import { beforeEach, describe, expect, it } from "vitest";
import worker from "../src/index";
import { PUBLIC_ORIGIN, type Env } from "../src/worker";
import { createTestDb } from "./d1";
import { createTestKv } from "./kv";

const MCP_TOKEN = "mcp-token-0123456789abcdef";
const SYNC_TOKEN = "sync-token-0123456789abcdef";
const REDIRECT = "https://grok.com/oauth/callback";
const ctx = { waitUntil() {}, passThroughOnException() {}, props: {} } as unknown as ExecutionContext;
let env: Env;

const call = (path: string, init?: RequestInit) => worker.fetch(new Request(`${PUBLIC_ORIGIN}${path}`, init), env, ctx);
const b64url = (buf: Buffer) => buf.toString("base64url");

beforeEach(() => {
  env = { DB: createTestDb(), OAUTH_KV: createTestKv(), MCP_TOKEN, SYNC_TOKEN } as Env;
});

async function register(): Promise<string> {
  const res = await call("/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ client_name: "Grok", redirect_uris: [REDIRECT], token_endpoint_auth_method: "none" }),
  });
  expect(res.status).toBe(201);
  return ((await res.json()) as { client_id: string }).client_id;
}

/** Opens the sign-in page; returns what the browser would hold to submit it. */
async function openAuthorize(clientId: string) {
  const verifier = b64url(randomBytes(32));
  const challenge = b64url(createHash("sha256").update(verifier).digest());
  const query = new URLSearchParams({
    response_type: "code",
    client_id: clientId,
    redirect_uri: REDIRECT,
    code_challenge: challenge,
    code_challenge_method: "S256",
    state: "state-123",
    scope: "diary",
    resource: `${PUBLIC_ORIGIN}/mcp`,
  });
  const res = await call(`/authorize?${query}`);
  const body = await res.text();
  expect(res.status, body).toBe(200);
  expect(res.headers.get("Content-Type")).toContain("text/html");
  const handle = /name="handle" value="([^"]+)"/.exec(body)?.[1];
  expect(handle).toBeTruthy();
  const cookie = res.headers
    .getSetCookie()
    .map((c) => c.split(";")[0])
    .join("; ");
  return { verifier, handle: handle!, cookie, body };
}

function submit(cookie: string, fields: Record<string, string>, ip = "203.0.113.7") {
  return call("/authorize", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded", Cookie: cookie, "CF-Connecting-IP": ip },
    body: new URLSearchParams(fields),
    redirect: "manual",
  });
}

async function exchange(clientId: string, code: string, verifier: string) {
  const res = await call("/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "authorization_code",
      code,
      redirect_uri: REDIRECT,
      client_id: clientId,
      code_verifier: verifier,
      resource: `${PUBLIC_ORIGIN}/mcp`,
    }),
  });
  return { status: res.status, body: (await res.json()) as { access_token?: string; refresh_token?: string } };
}

function listTools(token: string) {
  return call("/mcp", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
      Accept: "application/json, text/event-stream",
    },
    body: JSON.stringify({ jsonrpc: "2.0", id: 1, method: "tools/list", params: {} }),
  });
}

describe("discovery (FR-19)", () => {
  it("advertises the resource and authorization server", async () => {
    const challenge = await call("/mcp", { method: "POST", body: "{}" });
    expect(challenge.status).toBe(401);
    expect(challenge.headers.get("WWW-Authenticate")).toContain("resource_metadata");

    const resource = (await (await call("/.well-known/oauth-protected-resource/mcp")).json()) as Record<string, unknown>;
    expect(resource.resource).toBe(`${PUBLIC_ORIGIN}/mcp`);

    const as = (await (await call("/.well-known/oauth-authorization-server")).json()) as Record<string, unknown>;
    expect(as.authorization_endpoint).toBe(`${PUBLIC_ORIGIN}/authorize`);
    expect(as.token_endpoint).toBe(`${PUBLIC_ORIGIN}/token`);
    expect(as.registration_endpoint).toBe(`${PUBLIC_ORIGIN}/register`);
    expect(as.code_challenge_methods_supported).toContain("S256");
  });
});

describe("sign-in flow (FR-21)", () => {
  it("issues a token that works on /mcp after the owner enters MCP_TOKEN", async () => {
    const clientId = await register();
    const { verifier, handle, cookie, body } = await openAuthorize(clientId);
    expect(body).toContain("Grok");
    expect(body).toContain("grok.com");

    const wrong = await submit(cookie, { handle, decision: "approve", secret: "nope" });
    expect(wrong.status).toBe(401);
    expect(await wrong.text()).toContain("not correct");

    // The same handle still works after a wrong attempt.
    const ok = await submit(cookie, { handle, decision: "approve", secret: MCP_TOKEN });
    expect(ok.status).toBe(302);
    const location = new URL(ok.headers.get("Location")!);
    expect(`${location.origin}${location.pathname}`).toBe(REDIRECT);
    expect(location.searchParams.get("state")).toBe("state-123");
    const code = location.searchParams.get("code")!;

    const token = await exchange(clientId, code, verifier);
    expect(token.status).toBe(200);
    expect(token.body.access_token).toBeTruthy();

    const tools = await listTools(token.body.access_token!);
    expect(tools.status).toBe(200);
    expect(((await tools.json()) as { result: { tools: unknown[] } }).result.tools).toHaveLength(7);

    // Handles are single use.
    expect((await submit(cookie, { handle, decision: "approve", secret: MCP_TOKEN })).status).toBe(400);
  });

  it("rejects a code exchanged with the wrong PKCE verifier", async () => {
    const clientId = await register();
    const { handle, cookie } = await openAuthorize(clientId);
    const ok = await submit(cookie, { handle, decision: "approve", secret: MCP_TOKEN });
    const code = new URL(ok.headers.get("Location")!).searchParams.get("code")!;
    expect((await exchange(clientId, code, b64url(randomBytes(32)))).status).toBe(400);
  });

  it("deny redirects back with access_denied", async () => {
    const clientId = await register();
    const { handle, cookie } = await openAuthorize(clientId);
    const denied = await submit(cookie, { handle, decision: "deny" });
    expect(denied.status).toBe(302);
    expect(new URL(denied.headers.get("Location")!).searchParams.get("error")).toBe("access_denied");
  });

  it("locks out an IP after 5 wrong tokens", async () => {
    const clientId = await register();
    const { handle, cookie } = await openAuthorize(clientId);
    for (let i = 0; i < 5; i++) {
      expect((await submit(cookie, { handle, decision: "approve", secret: `wrong-${i}` })).status).toBe(401);
    }
    expect((await submit(cookie, { handle, decision: "approve", secret: MCP_TOKEN })).status).toBe(429);
    // Another IP is unaffected.
    expect((await submit(cookie, { handle, decision: "approve", secret: MCP_TOKEN }, "198.51.100.9")).status).toBe(302);
  });

  it("refuses the form without the browser cookie from the sign-in page", async () => {
    const clientId = await register();
    const { handle } = await openAuthorize(clientId);
    expect((await submit("", { handle, decision: "approve", secret: MCP_TOKEN })).status).toBe(400);
  });
});

describe("static tokens and sync (FR-20)", () => {
  it("still accepts MCP_TOKEN directly on /mcp and keeps /sync on SYNC_TOKEN", async () => {
    expect((await listTools(MCP_TOKEN)).status).toBe(200);
    expect((await listTools(SYNC_TOKEN)).status).toBe(401);
    const pull = await call("/sync/pull?since=0", { headers: { Authorization: `Bearer ${SYNC_TOKEN}` } });
    expect(pull.status).toBe(200);
  });

  it("tolerates a stale Mcp-Session-Id header (FR-22)", async () => {
    const res = await call("/mcp", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${MCP_TOKEN}`,
        "Content-Type": "application/json",
        Accept: "application/json, text/event-stream",
        "Mcp-Session-Id": "stale-session",
      },
      body: JSON.stringify({ jsonrpc: "2.0", id: 1, method: "tools/list", params: {} }),
    });
    expect(res.status).toBe(200);
  });
});
