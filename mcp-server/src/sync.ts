import { z } from "zod";
import { applyChanges, changesSince, putGoals, type Change } from "./db";

const entryChange = z.object({
  uuid: z.string().min(1),
  updatedAt: z.number().int().nonnegative(),
  deleted: z.literal(false),
  date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
  mealType: z.string().min(1),
  foodName: z.string(),
  brandName: z.string().nullable(),
  calories: z.number(),
  proteinG: z.number(),
  carbG: z.number(),
  fatG: z.number(),
  servingSizeG: z.number(),
  servingUnit: z.string(),
  foodId: z.string().nullable(),
  servingId: z.string().nullable(),
  servingQuantity: z.number().nullable(),
  sortOrder: z.number().int(),
});

const tombstone = z.object({
  uuid: z.string().min(1),
  updatedAt: z.number().int().nonnegative(),
  deleted: z.literal(true),
});

const pushBody = z.object({ changes: z.array(z.discriminatedUnion("deleted", [entryChange, tombstone])).max(1000) });

const goals = z.object({
  calories: z.number().nonnegative(),
  proteinG: z.number().nonnegative(),
  carbG: z.number().nonnegative(),
  fatG: z.number().nonnegative(),
});
const settingsBody = z.object({ goalsByWeekday: z.record(z.enum(["0", "1", "2", "3", "4", "5", "6"]), goals) });

function json(body: unknown, status = 200): Response {
  return Response.json(body, { status });
}

export async function handleSync(request: Request, db: D1Database, path: string): Promise<Response> {
  if (path === "/sync/push" && request.method === "POST") {
    const parsed = pushBody.safeParse(await request.json().catch(() => null));
    if (!parsed.success) return json({ error: "invalid body", issues: parsed.error.issues }, 400);
    await applyChanges(db, parsed.data.changes as Change[]);
    return json({ ok: true });
  }

  if (path === "/sync/pull" && request.method === "GET") {
    const since = Number(new URL(request.url).searchParams.get("since") ?? "0");
    if (!Number.isInteger(since) || since < 0) return json({ error: "invalid since" }, 400);
    return json(await changesSince(db, since));
  }

  if (path === "/sync/settings" && request.method === "PUT") {
    const parsed = settingsBody.safeParse(await request.json().catch(() => null));
    if (!parsed.success) return json({ error: "invalid body", issues: parsed.error.issues }, 400);
    await putGoals(db, parsed.data.goalsByWeekday, Date.now());
    return json({ ok: true });
  }

  return json({ error: "not found" }, 404);
}
