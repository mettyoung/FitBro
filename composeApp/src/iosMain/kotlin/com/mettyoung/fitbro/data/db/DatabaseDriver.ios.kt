package com.mettyoung.fitbro.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual fun createSqlDriver(): SqlDriver = NativeSqliteDriver(
    schema = FitBroDatabase.Schema,
    name = "fitbro.db"
)
