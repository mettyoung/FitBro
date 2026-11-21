import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import {
  MEAL_TYPES,
  addDays,
  applyChanges,
  entriesInRange,
  expenditureInRange,
  getEntry,
  getGoals,
  nextSortOrder,
  totalsByDay,
  weekdayMonBased,
  bodyInRange,
  getLibrary,
  getPreferences,
  workoutsInRange,
  type Entry,
  type Expenditure,
  type Preferences,
  type Workout,
} from "./db";
import {
  TREND_WARMUP_DAYS,
  TREND_WINDOW_DAYS,
  averagingDays,
  bodyFatKg,
  cardioSessions,
  dateRange,
  estimateExpenditure,
  metricPoints,
  resistanceSessions,
  summarizeTrend,
  trendSeries,
  weekBalance,
  weekStartFor,
  type BodyMetric,
} from "./analytics";

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

function publicExpenditure(e: Expenditure) {
  return {
    date: e.date,
    intake: round(e.intake),
    burn: round(e.burn),
    balance: round(e.balance),
    breakdown: { bmr: round(e.bmr), tef: round(e.tef), neat: round(e.neat), eat: round(e.eat) },
    synced_at: new Date(e.updatedAt).toISOString(),
  };
}

const EXPENDITURE_NOTE =
  "Computed by the FitBro app from Health Connect: burn = bmr (basal) + tef (digesting food) + neat (everyday movement) + eat (exercise); " +
  "balance = intake - burn (negative = deficit). intake is the app's figure for that day and can differ from diary totals when the day's " +
  "source is Health Connect. Values are as of synced_at, so today's burn grows as the day goes on; days the app never computed are absent.";


function round2(n: number | null): number | null {
  return n == null ? null : Math.round(n * 100) / 100;
}

const BODY_METRICS: BodyMetric[] = ["weight", "lean_mass", "body_fat"];

const TODAY_NOTE = "today = the user's current local date (YYYY-MM-DD); it's needed because the server doesn't know their time zone.";

/** Before the app has synced preferences: the app's defaults (Monday weeks, strength + mind-body hidden from cardio). */
function effectivePreferences(p: Preferences | null) {
  return {
    synced: p != null,
    weekStart: p?.weekStart ?? "MONDAY",
    resistanceSessionsPerWeek: p?.resistanceSessionsPerWeek ?? null,
    cardioMinutesPerWeek: p?.cardioMinutesPerWeek ?? null,
    excludedResistanceSources: p?.excludedResistanceSources ?? [],
    isCardio: (w: Workout) =>
      p ? cardioSessions([w], p.hiddenWorkoutTypes).length > 0 : w.category !== "STRENGTH" && w.category !== "MIND_BODY",
  };
}

function publicWorkout(w: Workout, withDetail: boolean) {
  const d = w.detail;
  return {
    id: w.id,
    date: w.date,
    start: new Date(w.startMs).toISOString(),
    minutes: w.minutes,
    activity: w.activity,
    type: w.type,
    category: w.category,
    source_app: w.source,
    ...(withDetail
      ? {
          detail: d && {
            title: d.title,
            notes: d.notes,
            recording_method: d.recordingMethod,
            device: d.device,
            active_kcal: d.activeKcal == null ? null : round(d.activeKcal),
            active_kcal_sources: d.activeKcalSources,
            heart_rate: d.heartRate && {
              avg_bpm: d.heartRate.avgBpm,
              min_bpm: d.heartRate.minBpm,
              max_bpm: d.heartRate.maxBpm,
              samples: d.heartRate.samples,
              sources: d.heartRate.sources,
            },
            heart_rate_permission: d.heartRateAllowed,
            exercises: d.segments.map((s) => ({
              name: s.name,
              repetitions: s.repetitions || null,
              minutes: round((s.endMs - s.startMs) / 60000),
            })),
            laps: d.laps,
            has_route: d.hasRoute,
          },
        }
      : {}),
  };
}

const WORKOUT_NOTE =
  "Sessions are read by the app from Health Connect / HealthKit (written there by Mi Fitness, Hevy, etc.). Health Connect has no " +
  "sets/weights, so strength detail is limited to what the source app wrote (often just duration and calories). Only the last " +
  "~90 days are synced; detail (heart rate, active kcal, exercises) may be null for sessions the app hasn't read yet.";

export function createServer(db: D1Database, now: () => number = Date.now): McpServer {
  const server = new McpServer({ name: "fitbro", version: "1.0.0" });

  // FR-7: read tools
  server.registerTool(
    "get_daily_summary",
    {
      description:
        "Calorie and macro totals for one day of the user's FitBro food diary, with that day's goals, what remains, and calories burned " +
        "(expenditure; null if the app hasn't synced it). Dates are the user's local date.",
      inputSchema: { date },
      annotations: { readOnlyHint: true },
    },
    async ({ date }) => {
      const entries = await entriesInRange(db, date, date);
      const [totals] = totalsByDay(entries, date, date);
      const goals = (await getGoals(db))?.[String(weekdayMonBased(date))] ?? null;
      const [expenditure] = await expenditureInRange(db, date, date);
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
        expenditure: expenditure ? publicExpenditure(expenditure) : null,
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
    "get_expenditure",
    {
      description:
        `Calories burned per day and net energy balance between start_date and end_date inclusive (max ${MAX_RANGE_DAYS} days; ` +
        `end_date defaults to start_date), plus averages and the total balance over days that have data. ${EXPENDITURE_NOTE}`,
      inputSchema: { start_date: date, end_date: date.optional() },
      annotations: { readOnlyHint: true },
    },
    async ({ start_date, end_date }) => {
      const end = end_date ?? start_date;
      if (rangeTooLong(start_date, end)) return error(`end_date must be within ${MAX_RANGE_DAYS} days after start_date`);
      const days = await expenditureInRange(db, start_date, end);
      const sum = (pick: (d: Expenditure) => number) => days.reduce((s, d) => s + pick(d), 0);
      const avg = (pick: (d: Expenditure) => number) => (days.length ? round(sum(pick) / days.length) : 0);
      return result({
        days: days.map(publicExpenditure),
        days_with_data: days.length,
        average_per_day: {
          intake: avg((d) => d.intake),
          burn: avg((d) => d.burn),
          balance: avg((d) => d.balance),
          bmr: avg((d) => d.bmr),
          tef: avg((d) => d.tef),
          neat: avg((d) => d.neat),
          eat: avg((d) => d.eat),
        },
        total_balance: round(sum((d) => d.balance)),
      });
    },
  );

  server.registerTool(
    "get_goals",
    {
      description:
        "The user's daily calorie and macro goals by weekday (Monday … Sunday), plus weekly targets (resistance sessions, cardio minutes), " +
        "week start day, workout types hidden from cardio and apps excluded from resistance.",
      inputSchema: {},
      annotations: { readOnlyHint: true },
    },
    async () => {
      const goals = await getGoals(db);
      const prefs = await getPreferences(db);
      const weekly = prefs && {
        resistance_sessions_per_week: prefs.resistanceSessionsPerWeek,
        cardio_minutes_per_week: prefs.cardioMinutesPerWeek,
        week_starts_on: prefs.weekStart,
        hidden_workout_types: prefs.hiddenWorkoutTypes,
        resistance_excluded_apps: prefs.excludedResistanceSources,
      };
      if (!goals) return result({ goals: null, weekly_targets: weekly, note: "Goals have not been synced from the app yet." });
      const names = ["monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"];
      return result({
        ...Object.fromEntries(
          names.map((name, i) => {
            const g = goals[String(i)];
            return [name, g && { calories: g.calories, protein_g: g.proteinG, carb_g: g.carbG, fat_g: g.fatG }];
          }),
        ),
        weekly_targets: weekly,
      });
    },
  );


  // ---- Health data mirrored from the app ----
  server.registerTool(
    "get_body_measurements",
    {
      description:
        `Scale readings between start_date and end_date inclusive (max ${MAX_RANGE_DAYS} days): weight (kg), body fat (% and kg = weight × %), ` +
        "lean body mass (kg, only when the scale wrote it — never derived), each with the smoothed weight trend the app shows. " +
        "From Health Connect / HealthKit via the app; last ~120 days synced.",
      inputSchema: { start_date: date, end_date: date },
      annotations: { readOnlyHint: true },
    },
    async ({ start_date, end_date }) => {
      if (rangeTooLong(start_date, end_date)) return error(`end_date must be within ${MAX_RANGE_DAYS} days after start_date`);
      const readDays = dateRange(addDays(start_date, -TREND_WARMUP_DAYS), end_date);
      const all = await bodyInRange(db, readDays[0], end_date);
      const series = trendSeries(metricPoints("weight", all), readDays);
      const weightTrend = new Map(readDays.map((d, i) => [d, series[i]]));
      return result(
        all
          .filter((m) => m.date >= start_date)
          .map((m) => ({
            date: m.date,
            weight_kg: round2(m.weightKg),
            weight_trend_kg: round2(weightTrend.get(m.date) ?? null),
            body_fat_pct: round2(m.bodyFatPct),
            body_fat_kg: round2(bodyFatKg(m)),
            lean_mass_kg: round2(m.leanMassKg),
          })),
      );
    },
  );

  server.registerTool(
    "get_insights",
    {
      description:
        "The FitBro Insights tab: smoothed trends (EMA α 0.1) for weight, lean body mass and body fat kg over the 28 days ending end_date, " +
        "with change over the window and the average, plus estimated expenditure (TDEE) = average intake − trend weight change × 7700 kcal/kg ÷ " +
        "days between first and last weigh-in (needs ≥14 logged days and weigh-ins ≥14 days apart). end_date defaults to the day before " +
        "today (the app uses the last completed day). " + TODAY_NOTE,
      inputSchema: { today: date, end_date: date.optional() },
      annotations: { readOnlyHint: true },
    },
    async ({ today, end_date }) => {
      const end = end_date ?? addDays(today, -1);
      const window = dateRange(addDays(end, -(TREND_WINDOW_DAYS - 1)), end);
      const readDays = dateRange(addDays(window[0], -TREND_WARMUP_DAYS), end);
      const measurements = await bodyInRange(db, readDays[0], end);
      const trends = Object.fromEntries(
        BODY_METRICS.map((metric) => {
          const t = summarizeTrend(metric, measurements, readDays, window);
          return [
            metric,
            {
              trend_kg: round2(t.value),
              change_kg: round2(t.change),
              average_kg: round2(t.average),
              readings_in_window: t.readings.length,
            },
          ];
        }),
      );
      const intake = new Map(totalsByDay(await entriesInRange(db, window[0], end), window[0], end).map((d) => [d.date, d.calories]));
      const weightPoints = metricPoints("weight", measurements);
      const series = trendSeries(weightPoints, readDays);
      const estimate = estimateExpenditure(intake, weightPoints, new Map(readDays.map((d, i) => [d, series[i]])), window);
      const hcLogged = (await expenditureInRange(db, window[0], end)).filter((d) => d.intake > 0);
      const hcAvg = hcLogged.length ? round(hcLogged.reduce((s, d) => s + d.burn, 0) / hcLogged.length) : null;
      return result({
        window: { start: window[0], end },
        trends,
        estimated_expenditure: estimate
          ? {
              kcal_per_day: round(estimate.kcalPerDay),
              average_intake: round(estimate.avgIntake),
              logged_days: estimate.loggedDays,
              trend_weight_change_kg: round2(estimate.trendChangeKg),
              span_days: estimate.spanDays,
            }
          : null,
        estimated_expenditure_note: estimate ? null : "Needs ≥14 logged days and two weigh-ins ≥14 days apart in the window.",
        health_connect_average_burn: hcAvg,
      });
    },
  );

  server.registerTool(
    "get_workouts",
    {
      description:
        `Exercise sessions between start_date and end_date inclusive (max ${MAX_RANGE_DAYS} days), optionally one category ` +
        `(CARDIO, STRENGTH, MIND_BODY, SPORT, OTHER), with heart rate, active calories and exercises when available. ${WORKOUT_NOTE}`,
      inputSchema: {
        start_date: date,
        end_date: date,
        category: z.enum(["CARDIO", "STRENGTH", "MIND_BODY", "SPORT", "OTHER"]).optional(),
      },
      annotations: { readOnlyHint: true },
    },
    async ({ start_date, end_date, category }) => {
      if (rangeTooLong(start_date, end_date)) return error(`end_date must be within ${MAX_RANGE_DAYS} days after start_date`);
      const workouts = (await workoutsInRange(db, start_date, end_date)).filter((w) => !category || w.category === category);
      return result({
        workouts: workouts.map((w) => publicWorkout(w, true)),
        count: workouts.length,
        total_minutes: workouts.reduce((s, w) => s + w.minutes, 0),
      });
    },
  );

  server.registerTool(
    "get_week_summary",
    {
      description:
        "The FitBro Home tab for the calendar week containing date (default today; week start from the user's setting): week-to-date " +
        "calorie balance and average intake / burn over completed logged days (today excluded, like the app), average calorie goal, " +
        "resistance sessions vs weekly target (strength sessions from counted apps) and cardio minutes vs weekly target (excluding the " +
        "workout types the user hides). " + TODAY_NOTE,
      inputSchema: { today: date, date: date.optional() },
      annotations: { readOnlyHint: true },
    },
    async ({ today, date: anyDay }) => {
      const prefs = effectivePreferences(await getPreferences(db));
      const start = weekStartFor(anyDay ?? today, prefs.weekStart);
      const days = dateRange(start, addDays(start, 6));
      const completed = averagingDays(days, today);
      const balance = weekBalance(await expenditureInRange(db, days[0], days[6]), completed);
      const goals = await getGoals(db);
      const goalDays = completed.length ? completed : days;
      const goalValues = goalDays.map((d) => goals?.[String(weekdayMonBased(d))]?.calories).filter((v): v is number => v != null);
      const workouts = await workoutsInRange(db, days[0], days[6]);
      const resistance = resistanceSessions(workouts, prefs.excludedResistanceSources);
      const cardio = workouts.filter(prefs.isCardio);
      return result({
        week: { start: days[0], end: days[6], week_starts_on: prefs.weekStart, completed_days: completed.length },
        balance: {
          week_to_date_kcal: balance.total == null ? null : round(balance.total),
          average_per_day: balance.average == null ? null : round(balance.average),
          logged_days: balance.loggedDays,
        },
        average_intake: balance.avgIntake == null ? null : round(balance.avgIntake),
        average_burn: balance.avgBurn == null ? null : round(balance.avgBurn),
        average_calorie_goal: goalValues.length ? round(goalValues.reduce((a, b) => a + b, 0) / goalValues.length) : null,
        resistance: {
          sessions: resistance.length,
          target: prefs.resistanceSessionsPerWeek,
          counted_apps_exclude: prefs.excludedResistanceSources,
          list: resistance.map((w) => publicWorkout(w, false)),
        },
        cardio: {
          minutes: cardio.reduce((s, w) => s + w.minutes, 0),
          sessions: cardio.length,
          target_minutes: prefs.cardioMinutesPerWeek,
        },
        preferences_synced: prefs.synced,
      });
    },
  );

  server.registerTool(
    "get_custom_foods",
    {
      description:
        "The user's saved custom foods and custom meals from FitBro (macros are for the stated serving_size_g; a meal's items are what it logs). " +
        "Use these when the user mentions one by name, e.g. log a custom meal by calling log_food for each item.",
      inputSchema: {},
      annotations: { readOnlyHint: true },
    },
    async () => {
      const library = await getLibrary(db);
      if (!library) return result({ custom_foods: [], custom_meals: [], note: "Not synced from the app yet." });
      const food = (f: { name: string; brandName: string | null; calories: number; proteinG: number; carbG: number; fatG: number; servingSizeG: number }) => ({
        name: f.name,
        brand_name: f.brandName,
        calories: round(f.calories),
        protein_g: round(f.proteinG),
        carb_g: round(f.carbG),
        fat_g: round(f.fatG),
        serving_size_g: round(f.servingSizeG),
      });
      return result({
        custom_foods: library.value.customFoods.map(food),
        custom_meals: library.value.customMeals.map((m) => ({
          name: m.name,
          items: m.items.map((i) => ({ ...food(i), serving_unit: i.servingUnit })),
          total_calories: round(m.items.reduce((s, i) => s + i.calories, 0)),
        })),
        synced_at: new Date(library.updatedAt).toISOString(),
      });
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
