package com.example.androidApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.text.BasicText
import com.example.feature.home.impl.HomeFeatureImpl
import com.example.shared.Greeting

/**
 * Entry point for the Android application.  This activity sets up a simple
 * Compose UI that displays messages from the shared module and the Home
 * feature.  Compose Material 3 is used to style the UI.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val greeting = Greeting().greet()
            val homeGreeting = HomeFeatureImpl().getHomeGreeting()
            BasicText(text = "$greeting\n$homeGreeting")
        }
    }
}
