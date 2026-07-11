import org.jetbrains.compose.compose

plugins {
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")

    // Convention plugins for static analysis and formatting
    id("com.nikkiw.videofeedlab.detekt-convention")
    id("com.nikkiw.videofeedlab.spotless-convention")
}

android {
    namespace = "com.nikkiw.videofeedlab.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.nikkiw.videofeedlab"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    // Set up packaging exclusions for Compose and Kotlin/JS dependencies
    packaging {
        resources.excludes +=
            setOf(
                "/META-INF/*.kotlin_module",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
            )
    }
}

dependencies {
    implementation(project(":feature:video-feed:api"))
    implementation(project(":feature:video-feed:impl"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.material)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
