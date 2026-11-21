export const MEAL_TYPES = ["BREAKFAST", "LUNCH", "DINNER", "SNACKS"] as const;
export type MealType = (typeof MEAL_TYPES)[number];

export interface Entry {
  uuid: string;
  date: string;
  mealType: string;
  foodName: string;
  brandName: string | null;
  calories: number;
  proteinG: number;
  carbG: number;
  fatG: number;
  servingSizeG: number;
  servingUnit: string;
  foodId: string | null;
  servingId: string | null;
  servingQuantity: number | null;
  sortOrder: number;
  updatedAt: number;
}

export type Change = (Entry & { deleted: false }) | { uuid: string; updatedAt: number; deleted: true };

export interface Goals {
  calories: number;
  proteinG: number;
  carbG: number;
  fatG: number;
}

/** Keyed by weekday, 0 = Monday … 6 = Sunday (matches the app's dayOfWeekMonBased). */
export type GoalsByWeekday = Record<string, Goals>;

interface EntryRow {
  uuid: string;
  date: string;
  meal_type: string;
  food_name: string;
  brand_name: string | null;
  calories: number;
  protein_g: number;
  carb_g: number;
  fat_g: number;
  serving_size_g: number;
  serving_unit: string;
  food_id: string | null;
  serving_id: string | null;
  serving_quantity: number | null;
  sort_order: number;
  updated_at: number;
  deleted: number;
  server_seq: number;
}

const NEXT_SEQ = "(SELECT COALESCE(MAX(server_seq), 0) + 1 FROM entries)";

// Strict `>` keeps re-pushed/echoed rows from bumping server_seq.
const UPSERT_SQL = `
INSERT INTO entries (uuid, date, meal_type, food_name, brand_name, calories, protein_g, carb_g, fat_g,
  serving_size_g, serving_unit, food_id, serving_id, serving_quantity, sort_order, updated_at, deleted, server_seq)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ${NEXT_SEQ})
ON CONFLICT(uuid) DO UPDATE SET
  date = excluded.date, meal_type = excluded.meal_type, food_name = excluded.food_name,
  brand_name = excluded.brand_name, calories = excluded.calories, protein_g = excluded.protein_g,
  carb_g = excluded.carb_g, fat_g = excluded.fat_g, serving_size_g = excluded.serving_size_g,
  serving_unit = excluded.serving_unit, food_id = excluded.food_id, serving_id = excluded.serving_id,
  serving_quantity = excluded.serving_quantity, sort_order = excluded.sort_order,
  updated_at = excluded.updated_at, deleted = 0, server_seq = excluded.server_seq
WHERE excluded.updated_at > entries.updated_at`;

const TOMBSTONE_SQL = `
INSERT INTO entries (uuid, updated_at, deleted, server_seq) VALUES (?, ?, 1, ${NEXT_SEQ})
ON CONFLICT(uuid) DO UPDATE SET deleted = 1, updated_at = excluded.updated_at, server_seq = excluded.server_seq
WHERE excluded.updated_at > entries.updated_at`;

function toEntry(r: EntryRow): Entry {
  return {
    uuid: r.uuid,
    date: r.date,
    mealType: r.meal_type,
    foodName: r.food_name,
    brandName: r.brand_name,
    calories: r.calories,
    proteinG: r.protein_g,
    carbG: r.carb_g,
    fatG: r.fat_g,
    servingSizeG: r.serving_size_g,
    servingUnit: r.serving_unit,
    foodId: r.food_id,
    servingId: r.serving_id,
    servingQuantity: r.serving_quantity,
    sortOrder: r.sort_order,
    updatedAt: r.updated_at,
  };
}

function toChange(r: EntryRow): Change {
  return r.deleted ? { uuid: r.uuid, updatedAt: r.updated_at, deleted: true } : { ...toEntry(r), deleted: false };
}

function upsertStatement(db: D1Database, e: Entry): D1PreparedStatement {
  return db
    .prepare(UPSERT_SQL)
    .bind(
      e.uuid, e.date, e.mealType, e.foodName, e.brandName, e.calories, e.proteinG, e.carbG, e.fatG,
      e.servingSizeG, e.servingUnit, e.foodId, e.servingId, e.servingQuantity, e.sortOrder, e.updatedAt,
    );
}

/**
 * FR-4: applies changes with last-write-wins on updatedAt. One `batch` = one D1 round trip; D1 runs the
 * statements sequentially in a transaction, so each still gets its own seq. (A per-row `await run()` loop
 * made a 200-row backfill push exceed the app's socket timeout.)
 */
export async function applyChanges(db: D1Database, changes: Change[]): Promise<void> {
  if (changes.length === 0) return;
  await db.batch(
    changes.map((c) => (c.deleted ? db.prepare(TOMBSTONE_SQL).bind(c.uuid, c.updatedAt) : upsertStatement(db, c))),
  );
}

/** FR-5 */
export async function changesSince(db: D1Database, since: number, limit = 500): Promise<{ changes: Change[]; cursor: number }> {
  const { results } = await db
    .prepare("SELECT * FROM entries WHERE server_seq > ? ORDER BY server_seq ASC LIMIT ?")
    .bind(since, limit)
    .all<EntryRow>();
  const cursor = results.length ? results[results.length - 1].server_seq : since;
  return { changes: results.map(toChange), cursor };
}

export async function entriesInRange(db: D1Database, start: string, end: string): Promise<Entry[]> {
  const { results } = await db
    .prepare(
      `SELECT * FROM entries WHERE deleted = 0 AND date >= ? AND date <= ?
       ORDER BY date ASC, CASE meal_type WHEN 'BREAKFAST' THEN 0 WHEN 'LUNCH' THEN 1 WHEN 'DINNER' THEN 2 ELSE 3 END,
       sort_order ASC`,
    )
    .bind(start, end)
    .all<EntryRow>();
  return results.map(toEntry);
}

export async function getEntry(db: D1Database, uuid: string): Promise<Entry | null> {
  const row = await db.prepare("SELECT * FROM entries WHERE uuid = ? AND deleted = 0").bind(uuid).first<EntryRow>();
  return row ? toEntry(row) : null;
}

export async function nextSortOrder(db: D1Database, date: string, mealType: string): Promise<number> {
  const row = await db
    .prepare("SELECT COALESCE(MAX(sort_order), -1) + 1 AS next FROM entries WHERE deleted = 0 AND date = ? AND meal_type = ?")
    .bind(date, mealType)
    .first<{ next: number }>();
  return row?.next ?? 0;
}

/** One day of energy expenditure as computed by the app. burn = bmr + tef + neat + eat; balance = intake - burn. */
export interface Expenditure {
  date: string;
  intake: number;
  bmr: number;
  tef: number;
  neat: number;
  eat: number;
  burn: number;
  balance: number;
  updatedAt: number;
}

interface ExpenditureRow {
  date: string;
  intake: number;
  bmr: number;
  tef: number;
  neat: number;
  eat: number;
  burn: number;
  balance: number;
  updated_at: number;
}

// `>=` (not `>`): no server_seq here to protect, so an identical re-send simply re-applies; only older data is ignored.
const EXPENDITURE_UPSERT_SQL = `
INSERT INTO expenditure (date, intake, bmr, tef, neat, eat, burn, balance, updated_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
ON CONFLICT(date) DO UPDATE SET
  intake = excluded.intake, bmr = excluded.bmr, tef = excluded.tef, neat = excluded.neat, eat = excluded.eat,
  burn = excluded.burn, balance = excluded.balance, updated_at = excluded.updated_at
WHERE excluded.updated_at >= expenditure.updated_at`;

export async function putExpenditure(db: D1Database, days: Expenditure[]): Promise<void> {
  if (days.length === 0) return;
  await db.batch(
    days.map((d) =>
      db
        .prepare(EXPENDITURE_UPSERT_SQL)
        .bind(d.date, d.intake, d.bmr, d.tef, d.neat, d.eat, d.burn, d.balance, d.updatedAt),
    ),
  );
}

export async function expenditureInRange(db: D1Database, start: string, end: string): Promise<Expenditure[]> {
  const { results } = await db
    .prepare("SELECT * FROM expenditure WHERE date >= ? AND date <= ? ORDER BY date ASC")
    .bind(start, end)
    .all<ExpenditureRow>();
  return results.map((r) => ({
    date: r.date,
    intake: r.intake,
    bmr: r.bmr,
    tef: r.tef,
    neat: r.neat,
    eat: r.eat,
    burn: r.burn,
    balance: r.balance,
    updatedAt: r.updated_at,
  }));
}

const GOALS_KEY = "goals_by_weekday";

/** FR-6 */
export async function putGoals(db: D1Database, goals: GoalsByWeekday, now: number): Promise<void> {
  await db
    .prepare(
      `INSERT INTO settings (key, value, updated_at) VALUES (?, ?, ?)
       ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at`,
    )
    .bind(GOALS_KEY, JSON.stringify(goals), now)
    .run();
}

export async function getGoals(db: D1Database): Promise<GoalsByWeekday | null> {
  const row = await db.prepare("SELECT value FROM settings WHERE key = ?").bind(GOALS_KEY).first<{ value: string }>();
  return row ? (JSON.parse(row.value) as GoalsByWeekday) : null;
}

/** 0 = Monday … 6 = Sunday for a YYYY-MM-DD date. */
export function weekdayMonBased(date: string): number {
  const day = new Date(`${date}T00:00:00Z`).getUTCDay();
  return (day + 6) % 7;
}

export interface DayTotals {
  date: string;
  calories: number;
  proteinG: number;
  carbG: number;
  fatG: number;
  entryCount: number;
}

export function totalsByDay(entries: Entry[], start: string, end: string): DayTotals[] {
  const byDate = new Map<string, DayTotals>();
  for (let d = start; d <= end; d = addDays(d, 1)) {
    byDate.set(d, { date: d, calories: 0, proteinG: 0, carbG: 0, fatG: 0, entryCount: 0 });
  }
  for (const e of entries) {
    const t = byDate.get(e.date);
    if (!t) continue;
    t.calories += e.calories;
    t.proteinG += e.proteinG;
    t.carbG += e.carbG;
    t.fatG += e.fatG;
    t.entryCount += 1;
  }
  return [...byDate.values()];
}

export function addDays(date: string, days: number): string {
  const d = new Date(`${date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

// ---- Settings blobs (app is the only writer; whole value replaced on each push) ----

async function putSetting(db: D1Database, key: string, value: unknown, now: number): Promise<void> {
  await db
    .prepare(
      `INSERT INTO settings (key, value, updated_at) VALUES (?, ?, ?)
       ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at`,
    )
    .bind(key, JSON.stringify(value), now)
    .run();
}

async function getSetting<T>(db: D1Database, key: string): Promise<{ value: T; updatedAt: number } | null> {
  const row = await db
    .prepare("SELECT value, updated_at FROM settings WHERE key = ?")
    .bind(key)
    .first<{ value: string; updated_at: number }>();
  return row ? { value: JSON.parse(row.value) as T, updatedAt: row.updated_at } : null;
}

/** App preferences that shape the Dashboard (Settings → Weekly goals / Week start / Cardio / Resistance). */
export interface Preferences {
  /** MONDAY … SUNDAY */
  weekStart: string;
  resistanceSessionsPerWeek: number;
  cardioMinutesPerWeek: number;
  /** WorkoutType names left out of the Cardio tab and cardio minutes. */
  hiddenWorkoutTypes: string[];
  /** Source apps whose strength sessions don't count toward resistance. */
  excludedResistanceSources: string[];
}

const PREFERENCES_KEY = "preferences";
export const putPreferences = (db: D1Database, p: Preferences, now: number) => putSetting(db, PREFERENCES_KEY, p, now);
export async function getPreferences(db: D1Database): Promise<Preferences | null> {
  return (await getSetting<Preferences>(db, PREFERENCES_KEY))?.value ?? null;
}

export interface CustomFood {
  name: string;
  brandName: string | null;
  calories: number;
  proteinG: number;
  carbG: number;
  fatG: number;
  servingSizeG: number;
}

export interface CustomMealItem extends CustomFood {
  servingUnit: string;
}

export interface CustomMeal {
  name: string;
  items: CustomMealItem[];
}

export interface Library {
  customFoods: CustomFood[];
  customMeals: CustomMeal[];
}

const LIBRARY_KEY = "library";
export const putLibrary = (db: D1Database, l: Library, now: number) => putSetting(db, LIBRARY_KEY, l, now);
export async function getLibrary(db: D1Database): Promise<{ value: Library; updatedAt: number } | null> {
  return getSetting<Library>(db, LIBRARY_KEY);
}

// ---- Body measurements ----

export interface BodyMeasurement {
  date: string;
  weightKg: number | null;
  bodyFatPct: number | null;
  leanMassKg: number | null;
}

interface BodyRow {
  date: string;
  weight_kg: number | null;
  body_fat_pct: number | null;
  lean_mass_kg: number | null;
}

/** Replaces [start, end] with [days]: readings deleted on the phone disappear here too. */
export async function replaceBody(
  db: D1Database,
  start: string,
  end: string,
  days: BodyMeasurement[],
  now: number,
): Promise<void> {
  await db.batch([
    db.prepare("DELETE FROM body_measurements WHERE date >= ? AND date <= ?").bind(start, end),
    ...days
      .filter((d) => d.date >= start && d.date <= end)
      .map((d) =>
        db
          .prepare(
            `INSERT INTO body_measurements (date, weight_kg, body_fat_pct, lean_mass_kg, updated_at) VALUES (?, ?, ?, ?, ?)
             ON CONFLICT(date) DO UPDATE SET weight_kg = excluded.weight_kg, body_fat_pct = excluded.body_fat_pct,
               lean_mass_kg = excluded.lean_mass_kg, updated_at = excluded.updated_at`,
          )
          .bind(d.date, d.weightKg, d.bodyFatPct, d.leanMassKg, now),
      ),
  ]);
}

export async function bodyInRange(db: D1Database, start: string, end: string): Promise<BodyMeasurement[]> {
  const { results } = await db
    .prepare("SELECT * FROM body_measurements WHERE date >= ? AND date <= ? ORDER BY date ASC")
    .bind(start, end)
    .all<BodyRow>();
  return results.map((r) => ({ date: r.date, weightKg: r.weight_kg, bodyFatPct: r.body_fat_pct, leanMassKg: r.lean_mass_kg }));
}

// ---- Workouts ----

export interface WorkoutDetail {
  title: string | null;
  notes: string | null;
  recordingMethod: string | null;
  device: string | null;
  activeKcal: number | null;
  activeKcalSources: string[];
  heartRate: { avgBpm: number; minBpm: number; maxBpm: number; samples: number; sources: string[] } | null;
  heartRateAllowed: boolean;
  segments: { name: string; startMs: number; endMs: number; repetitions: number }[];
  laps: number;
  hasRoute: boolean;
}

export interface Workout {
  id: string;
  date: string;
  startMs: number;
  minutes: number;
  activity: string;
  /** WorkoutType enum name, e.g. RUNNING, STRENGTH_TRAINING */
  type: string;
  /** CARDIO | STRENGTH | MIND_BODY | SPORT | OTHER */
  category: string;
  source: string;
  detail: WorkoutDetail | null;
}

interface WorkoutRow {
  id: string;
  date: string;
  start_ms: number;
  minutes: number;
  activity: string;
  type: string;
  category: string;
  source: string;
  detail: string | null;
}

/** Replaces the sessions in [start, end]; a session pushed without detail keeps the detail stored earlier. */
export async function replaceWorkouts(
  db: D1Database,
  start: string,
  end: string,
  workouts: Workout[],
  now: number,
): Promise<void> {
  const inRange = workouts.filter((w) => w.date >= start && w.date <= end);
  await db.batch([
    db
      .prepare("DELETE FROM workouts WHERE date >= ? AND date <= ? AND id NOT IN (SELECT value FROM json_each(?))")
      .bind(start, end, JSON.stringify(inRange.map((w) => w.id))),
    ...inRange.map((w) =>
      db
        .prepare(
          `INSERT INTO workouts (id, date, start_ms, minutes, activity, type, category, source, detail, updated_at)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
           ON CONFLICT(id) DO UPDATE SET date = excluded.date, start_ms = excluded.start_ms, minutes = excluded.minutes,
             activity = excluded.activity, type = excluded.type, category = excluded.category, source = excluded.source,
             detail = COALESCE(excluded.detail, workouts.detail), updated_at = excluded.updated_at`,
        )
        .bind(
          w.id, w.date, w.startMs, w.minutes, w.activity, w.type, w.category, w.source,
          w.detail ? JSON.stringify(w.detail) : null, now,
        ),
    ),
  ]);
}

export async function workoutsInRange(db: D1Database, start: string, end: string): Promise<Workout[]> {
  const { results } = await db
    .prepare("SELECT * FROM workouts WHERE date >= ? AND date <= ? ORDER BY start_ms ASC")
    .bind(start, end)
    .all<WorkoutRow>();
  return results.map((r) => ({
    id: r.id,
    date: r.date,
    startMs: r.start_ms,
    minutes: r.minutes,
    activity: r.activity,
    type: r.type,
    category: r.category,
    source: r.source,
    detail: r.detail ? (JSON.parse(r.detail) as WorkoutDetail) : null,
  }));
}

export function daysBetween(start: string, end: string): number {
  return Math.round((Date.parse(`${end}T00:00:00Z`) - Date.parse(`${start}T00:00:00Z`)) / 86_400_000);
}
