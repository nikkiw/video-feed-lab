import org.jetbrains.compose.compose

plugins {
    // Multiplatform library plugin for the shared code
    kotlin("multiplatform")
    // Compose plugin adds tasks and dependencies for Compose UI on all targets
    id("org.jetbrains.compose")
    // New KMP Android Library plugin
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.kotlinx.serialization)

    // Convention plugins for static analysis and formatting
    id("com.nikkiw.videofeedlab.detekt-convention")
    id("com.nikkiw.videofeedlab.spotless-convention")
}

kotlin {
    android {
        namespace = "com.nikkiw.videofeedlab.shared"
        compileSdk = 37
        minSdk = 23
        withHostTest {}
    }
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.decompose)
                implementation(libs.mvikotlin)
                implementation(libs.mvikotlin.main)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.koin.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
    }
}
