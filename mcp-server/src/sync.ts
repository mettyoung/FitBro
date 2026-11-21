import { z } from "zod";
import {
  applyChanges,
  changesSince,
  daysBetween,
  putExpenditure,
  putGoals,
  putLibrary,
  putPreferences,
  replaceBody,
  replaceWorkouts,
  type Change,
} from "./db";

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
const preferences = z.object({
  weekStart: z.enum(["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]),
  resistanceSessionsPerWeek: z.number().int().nonnegative(),
  cardioMinutesPerWeek: z.number().int().nonnegative(),
  hiddenWorkoutTypes: z.array(z.string()).max(200),
  excludedResistanceSources: z.array(z.string()).max(200),
});
// preferences is optional so app builds from before it existed keep syncing goals.
const settingsBody = z.object({
  goalsByWeekday: z.record(z.enum(["0", "1", "2", "3", "4", "5", "6"]), goals),
  preferences: preferences.optional(),
});

const isoDate = z.string().regex(/^\d{4}-\d{2}-\d{2}$/);
const MAX_REPLACE_DAYS = 400;

const bodyBody = z.object({
  start: isoDate,
  end: isoDate,
  days: z
    .array(
      z.object({
        date: isoDate,
        weightKg: z.number().positive().nullable(),
        bodyFatPct: z.number().min(0).max(100).nullable(),
        leanMassKg: z.number().positive().nullable(),
      }),
    )
    .max(MAX_REPLACE_DAYS),
});

const workoutDetail = z.object({
  title: z.string().nullable(),
  notes: z.string().nullable(),
  recordingMethod: z.string().nullable(),
  device: z.string().nullable(),
  activeKcal: z.number().nonnegative().nullable(),
  activeKcalSources: z.array(z.string()),
  heartRate: z
    .object({
      avgBpm: z.number(),
      minBpm: z.number(),
      maxBpm: z.number(),
      samples: z.number(),
      sources: z.array(z.string()),
    })
    .nullable(),
  heartRateAllowed: z.boolean(),
  segments: z
    .array(z.object({ name: z.string(), startMs: z.number().int(), endMs: z.number().int(), repetitions: z.number().int() }))
    .max(500),
  laps: z.number().int().nonnegative(),
  hasRoute: z.boolean(),
});

const workoutsBody = z.object({
  start: isoDate,
  end: isoDate,
  workouts: z
    .array(
      z.object({
        id: z.string().min(1),
        date: isoDate,
        startMs: z.number().int(),
        minutes: z.number().int().nonnegative(),
        activity: z.string(),
        type: z.string().min(1),
        category: z.enum(["CARDIO", "STRENGTH", "MIND_BODY", "SPORT", "OTHER"]),
        source: z.string(),
        detail: workoutDetail.nullable(),
      }),
    )
    .max(2000),
});

const food = {
  name: z.string(),
  brandName: z.string().nullable(),
  calories: z.number().nonnegative(),
  proteinG: z.number().nonnegative(),
  carbG: z.number().nonnegative(),
  fatG: z.number().nonnegative(),
  servingSizeG: z.number().nonnegative(),
};
const libraryBody = z.object({
  customFoods: z.array(z.object(food)).max(2000),
  customMeals: z
    .array(z.object({ name: z.string(), items: z.array(z.object({ ...food, servingUnit: z.string() })).max(100) }))
    .max(500),
});

const validRange = (start: string, end: string) => start <= end && daysBetween(start, end) <= MAX_REPLACE_DAYS;

const kcal = z.number().finite();
const expenditureDay = z.object({
  date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
  intake: kcal.nonnegative(),
  bmr: kcal.nonnegative(),
  tef: kcal.nonnegative(),
  neat: kcal.nonnegative(),
  eat: kcal.nonnegative(),
  burn: kcal.nonnegative(),
  balance: kcal,
  updatedAt: z.number().int().nonnegative(),
});
const expenditureBody = z.object({ days: z.array(expenditureDay).max(400) });

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
    if (parsed.data.preferences) await putPreferences(db, parsed.data.preferences, Date.now());
    return json({ ok: true });
  }

  if (path === "/sync/expenditure" && request.method === "PUT") {
    const parsed = expenditureBody.safeParse(await request.json().catch(() => null));
    if (!parsed.success) return json({ error: "invalid body", issues: parsed.error.issues }, 400);
    await putExpenditure(db, parsed.data.days);
    return json({ ok: true });
  }

  if (path === "/sync/body" && request.method === "PUT") {
    const parsed = bodyBody.safeParse(await request.json().catch(() => null));
    if (!parsed.success) return json({ error: "invalid body", issues: parsed.error.issues }, 400);
    const { start, end, days } = parsed.data;
    if (!validRange(start, end)) return json({ error: "invalid range" }, 400);
    await replaceBody(db, start, end, days, Date.now());
    return json({ ok: true });
  }

  if (path === "/sync/workouts" && request.method === "PUT") {
    const parsed = workoutsBody.safeParse(await request.json().catch(() => null));
    if (!parsed.success) return json({ error: "invalid body", issues: parsed.error.issues }, 400);
    const { start, end, workouts } = parsed.data;
    if (!validRange(start, end)) return json({ error: "invalid range" }, 400);
    await replaceWorkouts(db, start, end, workouts, Date.now());
    return json({ ok: true });
  }

  if (path === "/sync/library" && request.method === "PUT") {
    const parsed = libraryBody.safeParse(await request.json().catch(() => null));
    if (!parsed.success) return json({ error: "invalid body", issues: parsed.error.issues }, 400);
    await putLibrary(db, parsed.data, Date.now());
    return json({ ok: true });
  }

  return json({ error: "not found" }, 404);
}
