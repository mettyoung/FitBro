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

/** FR-4: applies changes with last-write-wins on updatedAt. Statements run sequentially so each gets its own seq. */
export async function applyChanges(db: D1Database, changes: Change[]): Promise<void> {
  for (const c of changes) {
    const stmt = c.deleted ? db.prepare(TOMBSTONE_SQL).bind(c.uuid, c.updatedAt) : upsertStatement(db, c);
    await stmt.run();
  }
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
