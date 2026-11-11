import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import {
  MEAL_TYPES,
  addDays,
  applyChanges,
  entriesInRange,
  getEntry,
  getGoals,
  nextSortOrder,
  totalsByDay,
  weekdayMonBased,
  type Entry,
} from "./db";

const date = z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Use YYYY-MM-DD");
const mealType = z.enum(MEAL_TYPES);
const grams = z.number().nonnegative();
const MAX_RANGE_DAYS = 92;

function result(value: unknown) {
  return { content: [{ type: "text" as const, text: JSON.stringify(value, null, 2) }] };
}

function error(message: string) {
  return { content: [{ type: "text" as const, text: message }], isError: true };
}

function rangeTooLong(start: string, end: string): boolean {
  return end < start || addDays(start, MAX_RANGE_DAYS) < end;
}

function round(n: number): number {
  return Math.round(n * 10) / 10;
}

function publicEntry(e: Entry) {
  return {
    uuid: e.uuid,
    date: e.date,
    meal_type: e.mealType,
    food_name: e.foodName,
    brand_name: e.brandName,
    calories: round(e.calories),
    protein_g: round(e.proteinG),
    carb_g: round(e.carbG),
    fat_g: round(e.fatG),
    amount_g: round(e.servingSizeG),
  };
}

export function createServer(db: D1Database, now: () => number = Date.now): McpServer {
  const server = new McpServer({ name: "fitbro", version: "1.0.0" });

  // FR-7: read tools
  server.registerTool(
    "get_daily_summary",
    {
      description:
        "Calorie and macro totals for one day of the user's FitBro food diary, with that day's goals and what remains. Dates are the user's local date.",
      inputSchema: { date },
      annotations: { readOnlyHint: true },
    },
    async ({ date }) => {
      const entries = await entriesInRange(db, date, date);
      const [totals] = totalsByDay(entries, date, date);
      const goals = (await getGoals(db))?.[String(weekdayMonBased(date))] ?? null;
      return result({
        date,
        totals: {
          calories: round(totals.calories),
          protein_g: round(totals.proteinG),
          carb_g: round(totals.carbG),
          fat_g: round(totals.fatG),
        },
        goals: goals && {
          calories: goals.calories,
          protein_g: goals.proteinG,
          carb_g: goals.carbG,
          fat_g: goals.fatG,
        },
        remaining: goals && {
          calories: round(goals.calories - totals.calories),
          protein_g: round(goals.proteinG - totals.proteinG),
          carb_g: round(goals.carbG - totals.carbG),
          fat_g: round(goals.fatG - totals.fatG),
        },
        entries: entries.map(publicEntry),
      });
    },
  );

  server.registerTool(
    "get_entries",
    {
      description: `Food diary entries between start_date and end_date inclusive (max ${MAX_RANGE_DAYS} days). end_date defaults to start_date.`,
      inputSchema: { start_date: date, end_date: date.optional() },
      annotations: { readOnlyHint: true },
    },
    async ({ start_date, end_date }) => {
      const end = end_date ?? start_date;
      if (rangeTooLong(start_date, end)) return error(`end_date must be within ${MAX_RANGE_DAYS} days after start_date`);
      return result((await entriesInRange(db, start_date, end)).map(publicEntry));
    },
  );

  server.registerTool(
    "get_trends",
    {
      description: `Per-day calorie and macro totals plus averages over logged days, between start_date and end_date inclusive (max ${MAX_RANGE_DAYS} days).`,
      inputSchema: { start_date: date, end_date: date },
      annotations: { readOnlyHint: true },
    },
    async ({ start_date, end_date }) => {
      if (rangeTooLong(start_date, end_date)) return error(`end_date must be within ${MAX_RANGE_DAYS} days after start_date`);
      const days = totalsByDay(await entriesInRange(db, start_date, end_date), start_date, end_date);
      const logged = days.filter((d) => d.entryCount > 0);
      const avg = (pick: (d: (typeof days)[number]) => number) =>
        logged.length ? round(logged.reduce((s, d) => s + pick(d), 0) / logged.length) : 0;
      return result({
        days: days.map((d) => ({
          date: d.date,
          calories: round(d.calories),
          protein_g: round(d.proteinG),
          carb_g: round(d.carbG),
          fat_g: round(d.fatG),
          entries: d.entryCount,
        })),
        logged_days: logged.length,
        average_per_logged_day: {
          calories: avg((d) => d.calories),
          protein_g: avg((d) => d.proteinG),
          carb_g: avg((d) => d.carbG),
          fat_g: avg((d) => d.fatG),
        },
      });
    },
  );

  server.registerTool(
    "get_goals",
    {
      description: "The user's daily calorie and macro goals by weekday (Monday … Sunday).",
      inputSchema: {},
      annotations: { readOnlyHint: true },
    },
    async () => {
      const goals = await getGoals(db);
      if (!goals) return result({ goals: null, note: "Goals have not been synced from the app yet." });
      const names = ["monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"];
      return result(
        Object.fromEntries(
          names.map((name, i) => {
            const g = goals[String(i)];
            return [name, g && { calories: g.calories, protein_g: g.proteinG, carb_g: g.carbG, fat_g: g.fatG }];
          }),
        ),
      );
    },
  );

  // FR-8: write tools
  server.registerTool(
    "log_food",
    {
      description:
        "Add a food to the user's diary. Provide totals for the amount eaten (not per 100 g). If the user doesn't give macros, estimate them and say so.",
      inputSchema: {
        date,
        meal_type: mealType,
        food_name: z.string().min(1).max(200),
        calories: grams,
        protein_g: grams,
        carb_g: grams,
        fat_g: grams,
        amount_g: z.number().positive().describe("Grams eaten. Estimate if the user gives a count (e.g. 2 large eggs ≈ 100 g)."),
        brand_name: z.string().max(200).optional(),
      },
    },
    async (args) => {
      const entry: Entry = {
        uuid: crypto.randomUUID(),
        date: args.date,
        mealType: args.meal_type,
        foodName: args.food_name,
        brandName: args.brand_name ?? null,
        calories: args.calories,
        proteinG: args.protein_g,
        carbG: args.carb_g,
        fatG: args.fat_g,
        servingSizeG: args.amount_g,
        servingUnit: "g",
        foodId: null,
        servingId: null,
        servingQuantity: null,
        sortOrder: await nextSortOrder(db, args.date, args.meal_type),
        updatedAt: now(),
      };
      await applyChanges(db, [{ ...entry, deleted: false }]);
      return result({ logged: publicEntry(entry), note: "Appears in the FitBro app on its next sync (when the app is opened)." });
    },
  );

  server.registerTool(
    "update_entry",
    {
      description: "Edit an existing diary entry by uuid (from get_entries). Only the fields provided are changed.",
      inputSchema: {
        uuid: z.string().min(1),
        date: date.optional(),
        meal_type: mealType.optional(),
        food_name: z.string().min(1).max(200).optional(),
        calories: grams.optional(),
        protein_g: grams.optional(),
        carb_g: grams.optional(),
        fat_g: grams.optional(),
        amount_g: z.number().positive().optional(),
      },
    },
    async (args) => {
      const existing = await getEntry(db, args.uuid);
      if (!existing) return error(`No entry with uuid ${args.uuid}`);
      const moved =
        (args.date && args.date !== existing.date) || (args.meal_type && args.meal_type !== existing.mealType);
      const macrosGiven =
        args.calories !== undefined || args.protein_g !== undefined || args.carb_g !== undefined || args.fat_g !== undefined;
      // Changing only the amount scales the existing macros proportionally.
      const scale =
        args.amount_g !== undefined && !macrosGiven && existing.servingSizeG > 0 ? args.amount_g / existing.servingSizeG : 1;
      const updated: Entry = {
        ...existing,
        date: args.date ?? existing.date,
        mealType: args.meal_type ?? existing.mealType,
        foodName: args.food_name ?? existing.foodName,
        calories: args.calories ?? existing.calories * scale,
        proteinG: args.protein_g ?? existing.proteinG * scale,
        carbG: args.carb_g ?? existing.carbG * scale,
        fatG: args.fat_g ?? existing.fatG * scale,
        servingSizeG: args.amount_g ?? existing.servingSizeG,
        updatedAt: Math.max(now(), existing.updatedAt + 1),
      };
      if (moved) updated.sortOrder = await nextSortOrder(db, updated.date, updated.mealType);
      // The FatSecret serving selection no longer describes the edited amount/macros.
      if (macrosGiven || args.amount_g !== undefined) {
        updated.servingId = null;
        updated.servingQuantity = null;
      }
      await applyChanges(db, [{ ...updated, deleted: false }]);
      return result({ updated: publicEntry(updated) });
    },
  );

  server.registerTool(
    "delete_entry",
    {
      description: "Delete a diary entry by uuid (from get_entries).",
      inputSchema: { uuid: z.string().min(1) },
      annotations: { destructiveHint: true },
    },
    async ({ uuid }) => {
      const existing = await getEntry(db, uuid);
      if (!existing) return error(`No entry with uuid ${uuid}`);
      await applyChanges(db, [{ uuid, updatedAt: Math.max(now(), existing.updatedAt + 1), deleted: true }]);
      return result({ deleted: publicEntry(existing) });
    },
  );

  return server;
}
