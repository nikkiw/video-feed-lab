import org.jetbrains.compose.compose

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    application

    // Convention plugins for static analysis and formatting
    id("com.example.detekt-convention")
    id("com.example.spotless-convention")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
    }
}

application {
    mainClass.set("com.example.desktop.MainKt")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":feature:home:api"))
    implementation(project(":feature:home:impl"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
}
