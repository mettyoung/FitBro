import { beforeEach, describe, expect, it } from "vitest";
import worker from "../src/index";
import { PUBLIC_ORIGIN, type Env } from "../src/worker";
import { changesSince, putGoals } from "../src/db";
import { createTestDb } from "./d1";
import { createTestKv } from "./kv";

const MCP_TOKEN = "mcp-token-0123456789abcdef";
const SYNC_TOKEN = "sync-token-0123456789abcdef";
let env: Env;
let id = 0;

const ctx = { waitUntil() {}, passThroughOnException() {}, props: {} } as unknown as ExecutionContext;

async function rpc(method: string, params: unknown, token = MCP_TOKEN) {
  const res = await worker.fetch(
    new Request(`${PUBLIC_ORIGIN}/mcp`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
        Accept: "application/json, text/event-stream",
      },
      body: JSON.stringify({ jsonrpc: "2.0", id: ++id, method, params }),
    }),
    env,
    ctx,
  );
  return res;
}

async function callTool(name: string, args: Record<string, unknown>) {
  const res = await rpc("tools/call", { name, arguments: args });
  const body = (await res.json()) as { result: { content: { text: string }[]; isError?: boolean } };
  const text = body.result.content[0].text;
  return { isError: body.result.isError ?? false, text, data: body.result.isError ? null : JSON.parse(text) };
}

beforeEach(async () => {
  env = { DB: createTestDb(), OAUTH_KV: createTestKv(), MCP_TOKEN, SYNC_TOKEN } as Env;
  await putGoals(env.DB, { "1": { calories: 2000, proteinG: 150, carbG: 200, fatG: 60 } }, 0);
});

describe("auth (FR-3)", () => {
  it("rejects missing or wrong tokens", async () => {
    expect((await rpc("tools/list", {}, "wrong")).status).toBe(401);
    expect((await rpc("tools/list", {}, SYNC_TOKEN)).status).toBe(401);
    const sync = await worker.fetch(
      new Request(`${PUBLIC_ORIGIN}/sync/pull?since=0`, { headers: { Authorization: `Bearer ${MCP_TOKEN}` } }),
      env,
      ctx,
    );
    expect(sync.status).toBe(401);
  });

  it("rejects everything when the secret is unset", async () => {
    env = { ...env, MCP_TOKEN: "" };
    expect((await rpc("tools/list", {}, "")).status).toBe(401);
  });
});

describe("MCP tools (FR-7, FR-8, FR-9)", () => {
  it("lists all tools", async () => {
    const body = (await (await rpc("tools/list", {})).json()) as { result: { tools: { name: string }[] } };
    expect(body.result.tools.map((t) => t.name).sort()).toEqual(
      [
        "delete_entry",
        "get_daily_summary",
        "get_entries",
        "get_expenditure",
        "get_goals",
        "get_trends",
        "get_body_measurements",
        "get_insights",
        "get_workouts",
        "get_week_summary",
        "get_custom_foods",
        "log_food",
        "update_entry",
      ].sort(),
    );
  });

  it("log_food → summary → update → delete round trip", async () => {
    const logged = await callTool("log_food", {
      date: "2026-11-10",
      meal_type: "BREAKFAST",
      food_name: "Eggs",
      calories: 140,
      protein_g: 12,
      carb_g: 1,
      fat_g: 10,
      amount_g: 100,
    });
    const uuid = logged.data.logged.uuid as string;

    // 2026-11-10 is a Tuesday → weekday 1 goals.
    const summary = await callTool("get_daily_summary", { date: "2026-11-10" });
    expect(summary.data.totals).toEqual({ calories: 140, protein_g: 12, carb_g: 1, fat_g: 10 });
    expect(summary.data.remaining.protein_g).toBe(138);

    const updated = await callTool("update_entry", { uuid, amount_g: 50 });
    expect(updated.data.updated).toMatchObject({ calories: 70, protein_g: 6, amount_g: 50 });

    await callTool("delete_entry", { uuid });
    expect((await callTool("get_entries", { start_date: "2026-11-10" })).data).toEqual([]);

    // The phone sees the final state as a tombstone.
    const { changes } = await changesSince(env.DB, 0);
    expect(changes).toEqual([expect.objectContaining({ uuid, deleted: true })]);
  });

  it("validates inputs", async () => {
    const bad = await callTool("log_food", {
      date: "11/10/2026",
      meal_type: "BRUNCH",
      food_name: "x",
      calories: -1,
      protein_g: 0,
      carb_g: 0,
      fat_g: 0,
      amount_g: 10,
    });
    expect(bad.isError).toBe(true);
    expect((await callTool("get_trends", { start_date: "2026-01-01", end_date: "2026-12-31" })).isError).toBe(true);
    expect((await callTool("delete_entry", { uuid: "missing" })).isError).toBe(true);
  });

  it("get_trends averages over logged days only", async () => {
    for (const [date, calories] of [["2026-11-09", 2000], ["2026-11-11", 1000]] as const) {
      await callTool("log_food", {
        date, meal_type: "LUNCH", food_name: "Meal", calories, protein_g: 0, carb_g: 0, fat_g: 0, amount_g: 300,
      });
    }
    const trends = await callTool("get_trends", { start_date: "2026-11-09", end_date: "2026-11-11" });
    expect(trends.data.days).toHaveLength(3);
    expect(trends.data.logged_days).toBe(2);
    expect(trends.data.average_per_logged_day.calories).toBe(1500);
  });
});

describe("expenditure", () => {
  const day = (date: string, overrides: Record<string, number> = {}) => ({
    date,
    intake: 1800,
    bmr: 1600,
    tef: 180,
    neat: 400,
    eat: 300,
    burn: 2480,
    balance: -680,
    updatedAt: 1000,
    ...overrides,
  });

  async function putExpenditure(days: unknown[], token = SYNC_TOKEN) {
    return worker.fetch(
      new Request(`${PUBLIC_ORIGIN}/sync/expenditure`, {
        method: "PUT",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify({ days }),
      }),
      env,
      ctx,
    );
  }

  it("PUT /sync/expenditure requires the sync token and a valid body", async () => {
    expect((await putExpenditure([day("2026-11-10")], MCP_TOKEN)).status).toBe(401);
    expect((await putExpenditure([{ ...day("2026-11-10"), date: "11/10/2026" }])).status).toBe(400);
    expect((await putExpenditure([{ ...day("2026-11-10"), burn: -1 }])).status).toBe(400);
    expect((await putExpenditure([day("2026-11-10")])).status).toBe(200);
  });

  it("upserts per date with last-write-wins on updatedAt", async () => {
    await putExpenditure([day("2026-11-10", { burn: 2400, updatedAt: 2000 })]);
    await putExpenditure([day("2026-11-10", { burn: 2100, updatedAt: 1000 })]); // older → ignored
    let got = await callTool("get_expenditure", { start_date: "2026-11-10" });
    expect(got.data.days[0].burn).toBe(2400);

    await putExpenditure([day("2026-11-10", { burn: 2600, updatedAt: 3000 })]); // newer → applied
    got = await callTool("get_expenditure", { start_date: "2026-11-10" });
    expect(got.data.days).toHaveLength(1);
    expect(got.data.days[0].burn).toBe(2600);
  });

  it("get_expenditure returns days with data, averages and total balance", async () => {
    await putExpenditure([
      day("2026-11-09", { burn: 2400, balance: -600, intake: 1800 }),
      day("2026-11-11", { burn: 2600, balance: -400, intake: 2200 }),
    ]);
    const got = await callTool("get_expenditure", { start_date: "2026-11-09", end_date: "2026-11-11" });
    expect(got.data.days.map((d: { date: string }) => d.date)).toEqual(["2026-11-09", "2026-11-11"]);
    expect(got.data.days[0].breakdown).toEqual({ bmr: 1600, tef: 180, neat: 400, eat: 300 });
    expect(got.data.days_with_data).toBe(2);
    expect(got.data.average_per_day).toMatchObject({ burn: 2500, balance: -500, intake: 2000 });
    expect(got.data.total_balance).toBe(-1000);
    expect((await callTool("get_expenditure", { start_date: "2026-01-01", end_date: "2026-12-31" })).isError).toBe(true);
  });

  it("get_daily_summary includes that day's expenditure, or null", async () => {
    expect((await callTool("get_daily_summary", { date: "2026-11-10" })).data.expenditure).toBeNull();
    await putExpenditure([day("2026-11-10")]);
    const summary = await callTool("get_daily_summary", { date: "2026-11-10" });
    expect(summary.data.expenditure).toMatchObject({ burn: 2480, balance: -680, intake: 1800 });
  });
});
