package io.github.yahiaangelo.sahmpos

import android.app.Application
import io.github.yahiaangelo.sahmpos.di.initKoin
import org.koin.android.ext.koin.androidContext

class SahmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@SahmApp)
        }
    }
}