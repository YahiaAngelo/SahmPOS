package io.github.yahiaangelo.sahmpos.di

import io.github.yahiaangelo.sahmpos.data.local.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DatabaseDriverFactory() }
}