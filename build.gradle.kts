/*
 * Top‑level build script for the multi‑module KMP template.  This file
 * configures common repositories and plugin versions and exposes version
 * properties used throughout the build.  The actual module specific
 * configuration lives in the respective build.gradle.kts files in each
 * subproject.
 */

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    id("com.example.spotless-convention")
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
