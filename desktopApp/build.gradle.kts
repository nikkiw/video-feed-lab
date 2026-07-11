import org.jetbrains.compose.compose

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    application

    // Convention plugins for static analysis and formatting
    id("com.nikkiw.videofeedlab.detekt-convention")
    id("com.nikkiw.videofeedlab.spotless-convention")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
    }
}

application {
    mainClass.set("com.nikkiw.videofeedlab.desktop.MainKt")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":feature:video-feed:api"))
    implementation(project(":feature:video-feed:impl"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
}
