-- Health data the app reads from Health Connect / HealthKit, mirrored so MCP clients can query it.
-- The app is the only writer; each push replaces a date range (so readings deleted on the phone disappear).

-- One row per local date with at least one reading.
CREATE TABLE body_measurements (
    date TEXT PRIMARY KEY,
    weight_kg REAL,
    body_fat_pct REAL,
    lean_mass_kg REAL,
    updated_at INTEGER NOT NULL
);

-- Exercise sessions from any app (Mi Fitness, Hevy, …). detail = JSON (heart rate, active kcal, segments…),
-- kept across pushes that omit it.
CREATE TABLE workouts (
    id TEXT PRIMARY KEY,
    date TEXT NOT NULL,
    start_ms INTEGER NOT NULL,
    minutes INTEGER NOT NULL,
    activity TEXT NOT NULL,
    type TEXT NOT NULL,
    category TEXT NOT NULL,
    source TEXT NOT NULL,
    detail TEXT,
    updated_at INTEGER NOT NULL
);

CREATE INDEX workouts_date_idx ON workouts(date);
