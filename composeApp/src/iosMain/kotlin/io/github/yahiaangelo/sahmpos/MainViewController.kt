package io.github.yahiaangelo.sahmpos

import androidx.compose.ui.window.ComposeUIViewController
import io.github.yahiaangelo.sahmpos.di.initKoin

private var koinInitialized = false

fun MainViewController() = ComposeUIViewController {
    if (!koinInitialized) {
        initKoin()
        koinInitialized = true
    }
    App()
}