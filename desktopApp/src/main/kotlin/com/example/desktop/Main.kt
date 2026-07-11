package com.example.desktop

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.feature.home.impl.HomeFeatureImpl
import com.example.shared.Greeting

/**
 * The entry point for the desktop application.  A simple Compose window
 * displays a greeting from the shared module and the Home feature.
 */
fun main() =
    application {
        Window(onCloseRequest = ::exitApplication, title = "KMP Desktop Template") {
            val greeting = Greeting().greet()
            val homeGreeting = HomeFeatureImpl().getHomeGreeting()
            BasicText(text = "$greeting\n$homeGreeting")
        }
    }
