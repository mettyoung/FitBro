import { describe, expect, it } from "vitest";
import { applyChanges, changesSince, type Change } from "../src/db";
import { createTestDb } from "./d1";

const entry = (overrides: Partial<Extract<Change, { deleted: false }>> = {}): Change => ({
  uuid: "a",
  date: "2026-11-10",
  mealType: "BREAKFAST",
  foodName: "Eggs",
  brandName: null,
  calories: 140,
  proteinG: 12,
  carbG: 1,
  fatG: 10,
  servingSizeG: 100,
  servingUnit: "g",
  foodId: null,
  servingId: null,
  servingQuantity: null,
  sortOrder: 0,
  updatedAt: 100,
  deleted: false,
  ...overrides,
});

describe("sync (FR-4, FR-5)", () => {
  it("pull returns pushed rows with an advancing cursor", async () => {
    const db = createTestDb();
    await applyChanges(db, [entry({ uuid: "a" }), entry({ uuid: "b" })]);
    const first = await changesSince(db, 0);
    expect(first.changes.map((c) => c.uuid)).toEqual(["a", "b"]);
    expect(first.cursor).toBe(2);
    expect((await changesSince(db, first.cursor)).changes).toEqual([]);
  });

  it("newer write wins, older and equal writes are ignored", async () => {
    const db = createTestDb();
    await applyChanges(db, [entry({ updatedAt: 100, foodName: "v1" })]);
    await applyChanges(db, [entry({ updatedAt: 50, foodName: "older" })]);
    await applyChanges(db, [entry({ updatedAt: 100, foodName: "echo" })]);
    let { changes, cursor } = await changesSince(db, 0);
    expect(changes).toHaveLength(1);
    expect(changes[0]).toMatchObject({ foodName: "v1" });
    expect(cursor).toBe(1);

    await applyChanges(db, [entry({ updatedAt: 200, foodName: "v2" })]);
    ({ changes } = await changesSince(db, cursor));
    expect(changes).toEqual([expect.objectContaining({ foodName: "v2", updatedAt: 200 })]);
  });

  it("tombstones delete existing rows and are pulled", async () => {
    const db = createTestDb();
    await applyChanges(db, [entry()]);
    await applyChanges(db, [{ uuid: "a", updatedAt: 150, deleted: true }]);
    const { changes } = await changesSince(db, 1);
    expect(changes).toEqual([{ uuid: "a", updatedAt: 150, deleted: true }]);
  });

  it("an older edit cannot resurrect a deleted row", async () => {
    const db = createTestDb();
    await applyChanges(db, [entry(), { uuid: "a", updatedAt: 150, deleted: true }]);
    await applyChanges(db, [entry({ updatedAt: 120 })]);
    const { changes } = await changesSince(db, 0);
    expect(changes).toEqual([{ uuid: "a", updatedAt: 150, deleted: true }]);
  });

  it("tombstone for an unknown uuid is stored so the phone learns of it", async () => {
    const db = createTestDb();
    await applyChanges(db, [{ uuid: "z", updatedAt: 10, deleted: true }]);
    expect((await changesSince(db, 0)).changes).toEqual([{ uuid: "z", updatedAt: 10, deleted: true }]);
  });
});
