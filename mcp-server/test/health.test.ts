import { beforeEach, describe, expect, it } from "vitest";
import worker from "../src/index";
import { PUBLIC_ORIGIN, type Env } from "../src/worker";
import { addDays, applyChanges, putExpenditure } from "../src/db";
import { estimateExpenditure, trendSeries, weekStartFor } from "../src/analytics";
import { createTestDb } from "./d1";
import { createTestKv } from "./kv";

const MCP_TOKEN = "mcp-token-0123456789abcdef";
const SYNC_TOKEN = "sync-token-0123456789abcdef";
const ctx = { waitUntil() {}, passThroughOnException() {}, props: {} } as unknown as ExecutionContext;
let env: Env;
let id = 0;

async function put(path: string, body: unknown) {
  return worker.fetch(
    new Request(`${PUBLIC_ORIGIN}${path}`, {
      method: "PUT",
      headers: { Authorization: `Bearer ${SYNC_TOKEN}`, "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }),
    env,
    ctx,
  );
}

async function callTool(name: string, args: Record<string, unknown>) {
  const res = await worker.fetch(
    new Request(`${PUBLIC_ORIGIN}/mcp`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${MCP_TOKEN}`,
        "Content-Type": "application/json",
        Accept: "application/json, text/event-stream",
      },
      body: JSON.stringify({ jsonrpc: "2.0", id: ++id, method: "tools/call", params: { name, arguments: args } }),
    }),
    env,
    ctx,
  );
  const body = (await res.json()) as { result: { content: { text: string }[]; isError?: boolean } };
  if (body.result.isError) throw new Error(body.result.content[0].text);
  return JSON.parse(body.result.content[0].text);
}

const workout = (over: Record<string, unknown> = {}) => ({
  id: "w1",
  date: "2026-11-17",
  startMs: Date.parse("2026-11-17T07:21:00Z"),
  minutes: 38,
  activity: "Group 3: Pull",
  type: "STRENGTH_TRAINING",
  category: "STRENGTH",
  source: "Hevy",
  detail: null,
  ...over,
});

const detail = {
  title: "Group 3: Pull",
  notes: null,
  recordingMethod: "Tracked in app",
  device: null,
  activeKcal: 210.4,
  activeKcalSources: ["Hevy"],
  heartRate: { avgBpm: 121, minBpm: 80, maxBpm: 160, samples: 300, sources: ["Mi Fitness"] },
  heartRateAllowed: true,
  segments: [{ name: "Pull-up", startMs: 0, endMs: 120000, repetitions: 10 }],
  laps: 0,
  hasRoute: false,
};

const prefs = {
  weekStart: "MONDAY",
  resistanceSessionsPerWeek: 5,
  cardioMinutesPerWeek: 150,
  hiddenWorkoutTypes: ["STRENGTH_TRAINING", "YOGA"],
  excludedResistanceSources: ["Mi Fitness"],
};

beforeEach(() => {
  env = { DB: createTestDb(), OAUTH_KV: createTestKv(), MCP_TOKEN, SYNC_TOKEN } as Env;
});

describe("sync endpoints", () => {
  it("rejects the MCP token and bad bodies", async () => {
    const res = await worker.fetch(
      new Request(`${PUBLIC_ORIGIN}/sync/body`, { method: "PUT", headers: { Authorization: `Bearer ${MCP_TOKEN}` }, body: "{}" }),
      env,
      ctx,
    );
    expect(res.status).toBe(401);
    expect((await put("/sync/body", { start: "2026-11-01", end: "2026-10-01", days: [] })).status).toBe(400);
    expect((await put("/sync/workouts", { start: "2026-11-01", end: "2026-11-02", workouts: [{ id: "" }] })).status).toBe(400);
  });

  it("body push replaces its range", async () => {
    await put("/sync/body", {
      start: "2026-11-01",
      end: "2026-11-10",
      days: [
        { date: "2026-11-02", weightKg: 66, bodyFatPct: 20, leanMassKg: null },
        { date: "2026-11-05", weightKg: 65.5, bodyFatPct: null, leanMassKg: 50 },
      ],
    });
    // Reading on the 2nd deleted on the phone.
    await put("/sync/body", {
      start: "2026-11-01",
      end: "2026-11-10",
      days: [{ date: "2026-11-05", weightKg: 65.5, bodyFatPct: null, leanMassKg: 50 }],
    });
    const rows = await callTool("get_body_measurements", { start_date: "2026-11-01", end_date: "2026-11-10" });
    expect(rows).toHaveLength(1);
    expect(rows[0]).toMatchObject({ date: "2026-11-05", weight_kg: 65.5, lean_mass_kg: 50, body_fat_kg: null });
  });

  it("workout push keeps stored detail and drops deleted sessions", async () => {
    await put("/sync/workouts", { start: "2026-11-16", end: "2026-11-22", workouts: [workout({ detail }), workout({ id: "w2" })] });
    await put("/sync/workouts", { start: "2026-11-16", end: "2026-11-22", workouts: [workout()] });
    const { workouts } = await callTool("get_workouts", { start_date: "2026-11-16", end_date: "2026-11-22" });
    expect(workouts.map((w: { id: string }) => w.id)).toEqual(["w1"]);
    expect(workouts[0].detail).toMatchObject({
      active_kcal: 210.4,
      heart_rate: { avg_bpm: 121, sources: ["Mi Fitness"] },
      exercises: [{ name: "Pull-up", repetitions: 10, minutes: 2 }],
    });
    expect(workouts[0].source_app).toBe("Hevy");
  });
});

describe("analytics match the app", () => {
  it("EMA seeds with the first reading and carries forward", () => {
    const series = trendSeries(new Map([["a", 60], ["c", 70]]), ["a", "b", "c"]);
    expect(series).toEqual([60, 60, 61]);
  });

  it("week start honours the setting", () => {
    expect(weekStartFor("2026-11-18", "MONDAY")).toBe("2026-11-16");
    expect(weekStartFor("2026-11-18", "SUNDAY")).toBe("2026-11-15");
    expect(weekStartFor("2026-11-15", "MONDAY")).toBe("2026-11-09");
  });

  it("expenditure estimate needs enough data", () => {
    const window = Array.from({ length: 28 }, (_, i) => addDays("2026-10-21", i));
    const intake = new Map(window.map((d) => [d, 2000]));
    const weights = new Map([[window[0], 65], [window[27], 66]]);
    const series = trendSeries(weights, window);
    const est = estimateExpenditure(intake, weights, new Map(window.map((d, i) => [d, series[i]])), window);
    // trend moves 0.1 kg (α 0.1 of a 1 kg jump) over 27 days → 2000 − 0.1 × 7700 / 27
    expect(est?.kcalPerDay).toBeCloseTo(2000 - (0.1 * 7700) / 27, 5);
    expect(estimateExpenditure(new Map(), weights, new Map(), window)).toBeNull();
  });
});

describe("MCP tools", () => {
  it("lists the health tools", async () => {
    const res = await worker.fetch(
      new Request(`${PUBLIC_ORIGIN}/mcp`, {
        method: "POST",
        headers: { Authorization: `Bearer ${MCP_TOKEN}`, "Content-Type": "application/json", Accept: "application/json, text/event-stream" },
        body: JSON.stringify({ jsonrpc: "2.0", id: ++id, method: "tools/list", params: {} }),
      }),
      env,
      ctx,
    );
    const names = ((await res.json()) as { result: { tools: { name: string }[] } }).result.tools.map((t) => t.name);
    expect(names).toEqual(expect.arrayContaining(["get_body_measurements", "get_insights", "get_workouts", "get_week_summary", "get_custom_foods"]));
  });

  it("get_week_summary mirrors the Home tab", async () => {
    const goalsByWeekday = Object.fromEntries(
      ["0", "1", "2", "3", "4", "5", "6"].map((k) => [k, { calories: 2000, proteinG: 150, carbG: 200, fatG: 60 }]),
    );
    expect((await put("/sync/settings", { goalsByWeekday, preferences: prefs })).status).toBe(200);
    const day = (date: string, intake: number, burn: number) => ({
      date, intake, bmr: 1500, tef: 0, neat: 0, eat: 0, burn, balance: intake - burn, updatedAt: 1,
    });
    await putExpenditure(env.DB, [day("2026-11-16", 1950, 2850), day("2026-11-17", 1960, 2860), day("2026-11-18", 500, 2000)]);
    await put("/sync/workouts", {
      start: "2026-11-16",
      end: "2026-11-22",
      workouts: [
        workout({ id: "a" }),
        workout({ id: "b", source: "Mi Fitness" }),
        workout({ id: "c", type: "RUNNING", category: "CARDIO", minutes: 40, source: "Mi Fitness" }),
      ],
    });
    const s = await callTool("get_week_summary", { today: "2026-11-18" });
    expect(s.week).toMatchObject({ start: "2026-11-16", end: "2026-11-22", completed_days: 2 });
    // Today (18th) is partial and excluded.
    expect(s.balance).toEqual({ week_to_date_kcal: -1800, average_per_day: -900, logged_days: 2 });
    expect(s.average_intake).toBe(1955);
    expect(s.average_calorie_goal).toBe(2000);
    expect(s.resistance).toMatchObject({ sessions: 1, target: 5 });
    expect(s.cardio).toEqual({ minutes: 40, sessions: 1, target_minutes: 150 });
    const goals = await callTool("get_goals", {});
    expect(goals.weekly_targets).toMatchObject({ resistance_sessions_per_week: 5, week_starts_on: "MONDAY" });
  });

  it("get_insights computes trends from synced readings and diary intake", async () => {
    const days = Array.from({ length: 28 }, (_, i) => addDays("2026-10-21", i));
    await put("/sync/body", {
      start: "2026-09-01",
      end: "2026-11-17",
      days: [
        { date: days[0], weightKg: 65, bodyFatPct: 20, leanMassKg: null },
        { date: days[27], weightKg: 66, bodyFatPct: 20, leanMassKg: null },
      ],
    });
    await applyChanges(
      env.DB,
      days.map((d, i) => ({
        uuid: `e${i}`, date: d, mealType: "LUNCH", foodName: "Rice", brandName: null, calories: 2000, proteinG: 0, carbG: 0, fatG: 0,
        servingSizeG: 100, servingUnit: "g", foodId: null, servingId: null, servingQuantity: null, sortOrder: 0, updatedAt: 1, deleted: false,
      })),
    );
    const r = await callTool("get_insights", { today: "2026-11-18" });
    expect(r.window).toEqual({ start: "2026-10-21", end: "2026-11-17" });
    expect(r.trends.weight).toMatchObject({ trend_kg: 65.1, change_kg: 0.1, readings_in_window: 2 });
    expect(r.trends.lean_mass.trend_kg).toBeNull();
    expect(r.estimated_expenditure).toMatchObject({ kcal_per_day: Math.round((2000 - (0.1 * 7700) / 27) * 10) / 10, logged_days: 28, span_days: 27 });
  });

  it("get_custom_foods returns the synced library", async () => {
    expect((await callTool("get_custom_foods", {})).custom_foods).toEqual([]);
    await put("/sync/library", {
      customFoods: [{ name: "Protein shake", brandName: null, calories: 120, proteinG: 24, carbG: 3, fatG: 1, servingSizeG: 30 }],
      customMeals: [
        {
          name: "Breakfast usual",
          items: [{ name: "Oats", brandName: null, calories: 300, proteinG: 10, carbG: 50, fatG: 5, servingSizeG: 80, servingUnit: "g" }],
        },
      ],
    });
    const lib = await callTool("get_custom_foods", {});
    expect(lib.custom_foods[0]).toMatchObject({ name: "Protein shake", protein_g: 24 });
    expect(lib.custom_meals[0]).toMatchObject({ name: "Breakfast usual", total_calories: 300 });
  });
});
