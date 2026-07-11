import org.jetbrains.compose.compose

plugins {
    // Multiplatform library plugin for the shared code
    kotlin("multiplatform")
    // Compose plugin adds tasks and dependencies for Compose UI on all targets
    id("org.jetbrains.compose")
    // New KMP Android Library plugin
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.plugin.compose")

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
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        // Platform-specific source sets are created automatically.  If you need
        // additional dependencies for a specific platform, you can declare them
        // here.
    }
}
