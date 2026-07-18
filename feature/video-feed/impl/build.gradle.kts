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
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }

        androidMain.dependencies {
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.exoplayer.hls)
            implementation(libs.androidx.media3.exoplayer.dash)
            implementation(libs.androidx.media3.ui)
            implementation(libs.coil.compose)
        }

        desktopMain.dependencies {
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.okhttp)

            // JVM wrapper; decoding and rendering are delegated to the locally installed LibVLC.
            implementation("uk.co.caprica:vlcj:4.12.1")
        }

        desktopTest.dependencies {
            implementation(libs.koin.test)
            implementation(libs.ktor.client.core)
        }
    }
}
