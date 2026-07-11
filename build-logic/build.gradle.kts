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
            id = "com.example.feature-api-convention"
            implementationClass = "com.example.buildlogic.FeatureApiConventionPlugin"
        }
        register("featureImplConvention") {
            id = "com.example.feature-impl-convention"
            implementationClass = "com.example.buildlogic.FeatureImplConventionPlugin"
        }
        register("detektConvention") {
            id = "com.example.detekt-convention"
            implementationClass = "com.example.buildlogic.DetektConventionPlugin"
        }
        register("spotlessConvention") {
            id = "com.example.spotless-convention"
            implementationClass = "com.example.buildlogic.SpotlessConventionPlugin"
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