-- Daily energy expenditure computed by the app (Health Connect / HealthKit + FitBro's calorie math).
-- One row per local date; the app is the only writer. Last-write-wins on updated_at.
CREATE TABLE expenditure (
    date TEXT PRIMARY KEY,
    intake REAL NOT NULL,
    bmr REAL NOT NULL,
    tef REAL NOT NULL,
    neat REAL NOT NULL,
    eat REAL NOT NULL,
    burn REAL NOT NULL,
    balance REAL NOT NULL,
    updated_at INTEGER NOT NULL
);
