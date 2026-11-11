const encoder = new TextEncoder();

/** FR-3: constant-time comparison; an unset/short secret never matches. */
export async function tokenMatches(candidate: string, secret: string | undefined): Promise<boolean> {
  if (!secret || secret.length < 16) return false;
  // Hash both sides so lengths match before the constant-time compare.
  const [a, b] = await Promise.all([
    crypto.subtle.digest("SHA-256", encoder.encode(candidate)),
    crypto.subtle.digest("SHA-256", encoder.encode(secret)),
  ]);
  const x = new Uint8Array(a);
  const y = new Uint8Array(b);
  let diff = 0;
  for (let i = 0; i < x.length; i++) diff |= x[i] ^ y[i];
  return diff === 0;
}

export function bearerToken(request: Request): string {
  const header = request.headers.get("Authorization") ?? "";
  return header.startsWith("Bearer ") ? header.slice(7) : "";
}
