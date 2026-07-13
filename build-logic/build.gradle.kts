import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    // Use the Kotlin JVM plugin version from our shared version catalog
    alias(libs.plugins.kotlin.jvm)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
    }
}

gradlePlugin {
    plugins {
        register("featureApiConvention") {
            id = "com.nikkiw.videofeedlab.feature-api-convention"
            implementationClass = "com.nikkiw.videofeedlab.buildlogic.FeatureApiConventionPlugin"
        }
        register("featureImplConvention") {
            id = "com.nikkiw.videofeedlab.feature-impl-convention"
            implementationClass = "com.nikkiw.videofeedlab.buildlogic.FeatureImplConventionPlugin"
        }
        register("detektConvention") {
            id = "com.nikkiw.videofeedlab.detekt-convention"
            implementationClass = "com.nikkiw.videofeedlab.buildlogic.DetektConventionPlugin"
        }
        register("spotlessConvention") {
            id = "com.nikkiw.videofeedlab.spotless-convention"
            implementationClass = "com.nikkiw.videofeedlab.buildlogic.SpotlessConventionPlugin"
        }
    }
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.compose.gradlePlugin)
    implementation(libs.android.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.spotless.gradlePlugin)
}