CREATE TABLE entries (
    uuid TEXT PRIMARY KEY,
    date TEXT,
    meal_type TEXT,
    food_name TEXT,
    brand_name TEXT,
    calories REAL,
    protein_g REAL,
    carb_g REAL,
    fat_g REAL,
    serving_size_g REAL,
    serving_unit TEXT,
    food_id TEXT,
    serving_id TEXT,
    serving_quantity REAL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL,
    deleted INTEGER NOT NULL DEFAULT 0,
    server_seq INTEGER NOT NULL
);

CREATE INDEX entries_date_idx ON entries(date);
CREATE INDEX entries_seq_idx ON entries(server_seq);

CREATE TABLE settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at INTEGER NOT NULL
);
