package com.mettyoung.fitbro.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.mettyoung.fitbro.AndroidAppContext

actual fun createSqlDriver(): SqlDriver = AndroidSqliteDriver(
    schema = FitBroDatabase.Schema,
    context = AndroidAppContext.context,
    name = "fitbro.db"
)
