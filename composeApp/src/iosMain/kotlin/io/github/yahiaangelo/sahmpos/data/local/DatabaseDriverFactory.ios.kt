package io.github.yahiaangelo.sahmpos.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import io.github.yahiaangelo.sahmpos.data.local.db.SahmDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(SahmDatabase.Schema, "sahm.db")
}