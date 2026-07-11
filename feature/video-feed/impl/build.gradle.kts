plugins {
    id("com.nikkiw.videofeedlab.feature-impl-convention")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material)

            implementation(libs.decompose)
            implementation(libs.decompose.compose)
            implementation(libs.mvikotlin)
            implementation(libs.mvikotlin.main)
            implementation(libs.mvikotlin.coroutines)
        }

        androidMain.dependencies {
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.exoplayer.hls)
            implementation(libs.androidx.media3.exoplayer.dash)
            implementation(libs.androidx.media3.ui)
        }

        desktopMain.dependencies {
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material)
        }
    }
}
