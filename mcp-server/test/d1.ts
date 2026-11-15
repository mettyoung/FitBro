import { readdirSync, readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { DatabaseSync } from "node:sqlite";

type Value = string | number | null;

type Stmt = {
  bind: (...values: Value[]) => Stmt;
  first: <T>() => Promise<T | null>;
  all: <T>() => Promise<{ results: T[]; success: true }>;
  run: () => Promise<{ success: true }>;
  runSync: () => void;
};

/** Minimal D1Database over node:sqlite — enough for prepare/bind/first/all/run/batch. */
export function createTestDb(): D1Database {
  const sqlite = new DatabaseSync(":memory:");
  // Apply every migration in order, like `wrangler d1 migrations apply`.
  const migrations = fileURLToPath(new URL("../migrations/", import.meta.url).href);
  for (const file of readdirSync(migrations).filter((f) => f.endsWith(".sql")).sort()) {
    sqlite.exec(readFileSync(`${migrations}/${file}`, "utf8"));
  }

  const prepare = (sql: string, params: Value[] = []): Stmt => ({
    bind: (...values: Value[]) => prepare(sql, values),
    first: async <T>() => (sqlite.prepare(sql).get(...params) as T | undefined) ?? null,
    all: async <T>() => ({ results: sqlite.prepare(sql).all(...params) as T[], success: true }),
    run: async () => {
      sqlite.prepare(sql).run(...params);
      return { success: true };
    },
    runSync: () => {
      sqlite.prepare(sql).run(...params);
    },
  });

  // Like D1: statements run in order inside one transaction; any failure rolls the whole batch back.
  const batch = async (stmts: Stmt[]) => {
    sqlite.exec("BEGIN");
    try {
      stmts.forEach((s) => s.runSync());
      sqlite.exec("COMMIT");
    } catch (e) {
      sqlite.exec("ROLLBACK");
      throw e;
    }
    return stmts.map(() => ({ success: true }));
  };

  return { prepare, batch } as unknown as D1Database;
}
