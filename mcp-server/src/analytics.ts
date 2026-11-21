// Port of the app's Dashboard / Insights maths (composeApp data/model/BodyTrend.kt + Week.kt) so MCP answers
// match what the user sees in FitBro. Keep the constants and rules in step with the Kotlin.
import { addDays, daysBetween, weekdayMonBased, type BodyMeasurement, type Expenditure, type Workout } from "./db";

export const TREND_ALPHA = 0.1;
export const TREND_WINDOW_DAYS = 28;
export const KCAL_PER_KG = 7700;
export const EE_MIN_LOGGED_DAYS = 14;
export const EE_MIN_SPAN_DAYS = 14;
/** Extra days read before a window so the EMA is warmed up (app reads 2 × 28 days before the week). */
export const TREND_WARMUP_DAYS = 2 * TREND_WINDOW_DAYS;

export const WEEK_STARTS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"] as const;

export type BodyMetric = "weight" | "lean_mass" | "body_fat";

export function dateRange(start: string, end: string): string[] {
  const days: string[] = [];
  for (let d = start; d <= end; d = addDays(d, 1)) days.push(d);
  return days;
}

/** Body fat in kg = weight × body fat %; null unless both were measured that day. */
export function bodyFatKg(m: BodyMeasurement): number | null {
  return m.weightKg != null && m.bodyFatPct != null ? (m.weightKg * m.bodyFatPct) / 100 : null;
}

export function metricValue(metric: BodyMetric, m: BodyMeasurement): number | null {
  if (metric === "weight") return m.weightKg;
  if (metric === "lean_mass") return m.leanMassKg; // scale reading only — never derived
  return bodyFatKg(m);
}

export function metricPoints(metric: BodyMetric, measurements: BodyMeasurement[]): Map<string, number> {
  const points = new Map<string, number>();
  for (const m of measurements) {
    const v = metricValue(metric, m);
    if (v != null) points.set(m.date, v);
  }
  return points;
}

/** EMA seeded with the first reading, α toward each new reading, carried forward between readings. */
export function trendSeries(points: Map<string, number>, days: string[], alpha = TREND_ALPHA): (number | null)[] {
  let trend: number | null = null;
  return days.map((d) => {
    const v = points.get(d);
    if (v !== undefined) trend = trend == null ? v : trend + alpha * (v - trend);
    return trend;
  });
}

export interface TrendSummary {
  /** Trend at window end. */
  value: number | null;
  /** Trend at window end − trend at window start. */
  change: number | null;
  /** Mean of the trend over the window. */
  average: number | null;
  readings: { date: string; value: number; trend: number | null }[];
}

export function summarizeTrend(
  metric: BodyMetric,
  measurements: BodyMeasurement[],
  readDays: string[],
  window: string[],
): TrendSummary {
  const points = metricPoints(metric, measurements);
  const series = trendSeries(points, readDays);
  const byDate = new Map(readDays.map((d, i) => [d, series[i]]));
  const trend = window.map((d) => byDate.get(d) ?? null);
  const first = trend[0];
  const last = trend[trend.length - 1];
  const defined = trend.filter((v): v is number => v != null);
  return {
    value: last,
    change: first != null && last != null ? last - first : null,
    average: defined.length ? defined.reduce((a, b) => a + b, 0) / defined.length : null,
    readings: window.filter((d) => points.has(d)).map((d) => ({ date: d, value: points.get(d)!, trend: byDate.get(d) ?? null })),
  };
}

export interface ExpenditureEstimate {
  kcalPerDay: number;
  avgIntake: number;
  loggedDays: number;
  trendChangeKg: number;
  spanDays: number;
}

/** avgIntake − Δtrend × 7700 / span, Δtrend between the window's first and last weigh-in. */
export function estimateExpenditure(
  intakeByDate: Map<string, number>,
  weightPoints: Map<string, number>,
  trendByDate: Map<string, number | null>,
  window: string[],
): ExpenditureEstimate | null {
  const logged = window.map((d) => intakeByDate.get(d) ?? 0).filter((v) => v > 0);
  if (logged.length < EE_MIN_LOGGED_DAYS) return null;
  const weighDays = window.filter((d) => weightPoints.has(d));
  if (weighDays.length < 2) return null;
  const span = daysBetween(weighDays[0], weighDays[weighDays.length - 1]);
  if (span < EE_MIN_SPAN_DAYS) return null;
  const startTrend = trendByDate.get(weighDays[0]);
  const endTrend = trendByDate.get(weighDays[weighDays.length - 1]);
  if (startTrend == null || endTrend == null) return null;
  const change = endTrend - startTrend;
  const avgIntake = logged.reduce((a, b) => a + b, 0) / logged.length;
  return { kcalPerDay: avgIntake - (change * KCAL_PER_KG) / span, avgIntake, loggedDays: logged.length, trendChangeKg: change, spanDays: span };
}

/** First day of the week containing [date], given the user's week-start setting. */
export function weekStartFor(date: string, weekStart: string): string {
  const startIdx = Math.max(0, WEEK_STARTS.indexOf(weekStart as (typeof WEEK_STARTS)[number]));
  const offset = (((weekdayMonBased(date) - startIdx) % 7) + 7) % 7;
  return addDays(date, -offset);
}

/** Completed days of the week: before [today] (today is partial, so excluded — like the app). */
export function averagingDays(weekDays: string[], today: string): string[] {
  return weekDays.filter((d) => d < today);
}

export interface WeekBalance {
  loggedDays: number;
  total: number | null;
  average: number | null;
  avgIntake: number | null;
  avgBurn: number | null;
}

/** Week-to-date balance + averages over completed, logged days (intake > 0). */
export function weekBalance(expenditure: Expenditure[], days: string[]): WeekBalance {
  const set = new Set(days);
  const logged = expenditure.filter((e) => set.has(e.date) && e.intake > 0);
  if (!logged.length) return { loggedDays: 0, total: null, average: null, avgIntake: null, avgBurn: null };
  const sum = (pick: (e: Expenditure) => number) => logged.reduce((s, e) => s + pick(e), 0);
  return {
    loggedDays: logged.length,
    total: sum((e) => e.balance),
    average: sum((e) => e.balance) / logged.length,
    avgIntake: sum((e) => e.intake) / logged.length,
    avgBurn: sum((e) => e.burn) / logged.length,
  };
}

/** Strength sessions that count toward the resistance target (any hidden-types setting, minus excluded apps). */
export function resistanceSessions(workouts: Workout[], excludedSources: string[]): Workout[] {
  return workouts.filter((w) => w.category === "STRENGTH" && !excludedSources.includes(w.source));
}

/** Sessions the Cardio tab shows: everything except the user's hidden workout types. */
export function cardioSessions(workouts: Workout[], hiddenTypes: string[]): Workout[] {
  return workouts.filter((w) => !hiddenTypes.includes(w.type));
}
