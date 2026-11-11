import { AuthorizationError, CimdFetchError, type ConsentDescription } from "@cloudflare/workers-oauth-provider";
import { tokenMatches } from "./auth";
import type { Env } from "./worker";

const MAX_FAILURES = 5;
const LOCKOUT_SECONDS = 15 * 60;

const escape = (value: string) => value.replace(/[&<>"']/g, (c) => `&#${c.charCodeAt(0)};`);

function page(details: ConsentDescription, handle: string): string {
  const name = escape(details.clientName);
  const origin = details.clientDomain
    ? `Published by <strong>${escape(details.clientDomain)}</strong>.`
    : "This app registered itself; its name is not verified.";
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>FitBro sign-in</title>
<style>
  body{font-family:system-ui,sans-serif;max-width:28rem;margin:3rem auto;padding:0 1rem;line-height:1.5;color:#1a1a1a;background:#fafafa}
  input[type=password]{width:100%;padding:.6rem;font-size:1rem;box-sizing:border-box;border:1px solid #bbb;border-radius:8px}
  button{padding:.6rem 1.2rem;font-size:1rem;border-radius:8px;border:0;margin-right:.5rem;cursor:pointer}
  .allow{background:#ff6a00;color:#fff} .deny{background:#e5e5e5}
  .muted{color:#555;font-size:.9rem}
</style></head>
<body>
<h1>Allow ${name} to use your FitBro diary?</h1>
<p class="muted">${origin} Access goes to <strong>${escape(details.redirectHost)}</strong>.
It will be able to read your food diary and goals, and add, edit or delete entries.</p>
${details.redirectIsLoopback ? "<p><strong>This sends access to an app on your computer.</strong> Continue only if you just started signing in from it.</p>" : ""}
<form method="post">
  <input type="hidden" name="handle" value="${escape(handle)}">
  <label for="secret">FitBro access token (MCP_TOKEN)</label>
  <p><input id="secret" type="password" name="secret" autocomplete="current-password" autofocus></p>
  <p><button class="allow" name="decision" value="approve">Allow</button><button class="deny" name="decision" value="deny">Deny</button></p>
</form>
</body></html>`;
}

function retryPage(handle: string): string {
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>FitBro sign-in</title></head>
<body style="font-family:system-ui,sans-serif;max-width:28rem;margin:3rem auto;padding:0 1rem">
<p style="color:#b00020">That token is not correct.</p>
<form method="post">
  <input type="hidden" name="handle" value="${escape(handle)}">
  <label for="secret">FitBro access token (MCP_TOKEN)</label>
  <p><input id="secret" type="password" name="secret" autofocus style="width:100%;padding:.6rem"></p>
  <p><button name="decision" value="approve">Allow</button> <button name="decision" value="deny">Deny</button></p>
</form>
</body></html>`;
}

function html(body: string, headers: Headers, status = 200): Response {
  headers.set("Content-Type", "text/html; charset=utf-8");
  headers.set("X-Frame-Options", "DENY");
  if (!headers.has("Content-Security-Policy")) headers.set("Content-Security-Policy", "frame-ancestors 'none'");
  headers.set("Cache-Control", "no-store");
  return new Response(body, { status, headers });
}

function plain(message: string, status: number): Response {
  return new Response(message, { status, headers: { "Content-Type": "text/plain; charset=utf-8" } });
}

function failureKey(request: Request): string {
  return `authfail:${request.headers.get("CF-Connecting-IP") ?? "unknown"}`;
}

async function failures(env: Env, request: Request): Promise<number> {
  return Number((await env.OAUTH_KV.get(failureKey(request))) ?? "0");
}

/** FR-21: single-user sign-in — the owner proves they hold MCP_TOKEN, then the grant is issued. */
export async function handleAuthorize(request: Request, env: Env): Promise<Response> {
  const oauth = env.OAUTH_PROVIDER;
  try {
    if (request.method === "GET") {
      const authRequest = await oauth.parseAuthRequest(request);
      const details = await oauth.describeConsent(authRequest);
      const consent = await oauth.beginConsent(authRequest);
      return html(page(details, consent.handle), consent.headers);
    }

    if (request.method !== "POST") return plain("Method not allowed", 405);

    const form = await request.formData();
    const handle = String(form.get("handle") ?? "");

    if (form.get("decision") !== "approve") {
      const denied = await oauth.denyConsent(request, handle);
      return new Response(null, { status: 302, headers: denied.headers });
    }

    if ((await failures(env, request)) >= MAX_FAILURES) {
      return plain("Too many attempts. Try again in 15 minutes.", 429);
    }

    if (!(await tokenMatches(String(form.get("secret") ?? ""), env.MCP_TOKEN))) {
      const count = (await failures(env, request)) + 1;
      await env.OAUTH_KV.put(failureKey(request), String(count), { expirationTtl: LOCKOUT_SECONDS });
      // The handle is only consumed by approve/deny, so the same form can be retried.
      return html(retryPage(handle), new Headers(), 401);
    }

    const approved = await oauth.approveConsent(request, handle);
    const { redirectTo } = await oauth.completeAuthorization({
      request: approved.request,
      userId: "owner",
      metadata: {},
      scope: approved.request.scope,
      props: { userId: "owner" },
    });
    approved.headers.set("Location", redirectTo);
    return new Response(null, { status: 302, headers: approved.headers });
  } catch (error) {
    if (error instanceof AuthorizationError && error.redirectTo) {
      return Response.redirect(error.redirectTo, 302);
    }
    if (error instanceof AuthorizationError) return plain(error.description ?? "Invalid authorization request.", 400);
    if (error instanceof CimdFetchError) return plain("This app could not be verified.", 400);
    throw error;
  }
}
