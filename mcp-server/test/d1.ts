import { readFileSync } from "node:fs";
import { DatabaseSync } from "node:sqlite";

type Value = string | number | null;

/** Minimal D1Database over node:sqlite — enough for prepare/bind/first/all/run. */
export function createTestDb(): D1Database {
  const sqlite = new DatabaseSync(":memory:");
  sqlite.exec(readFileSync(new URL("../migrations/0001_init.sql", import.meta.url), "utf8"));

  const prepare = (sql: string, params: Value[] = []) => ({
    bind: (...values: Value[]) => prepare(sql, values),
    first: async <T>() => (sqlite.prepare(sql).get(...params) as T | undefined) ?? null,
    all: async <T>() => ({ results: sqlite.prepare(sql).all(...params) as T[], success: true }),
    run: async () => {
      sqlite.prepare(sql).run(...params);
      return { success: true };
    },
  });

  return { prepare } as unknown as D1Database;
}
