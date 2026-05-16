package io.github.yahiaangelo.sahmpos.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.github.yahiaangelo.sahmpos.data.local.db.SahmDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(SahmDatabase.Schema, context, "sahm.db")
}