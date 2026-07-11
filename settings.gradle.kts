pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "kmp-modular-template"

// Include the custom build‑logic as a composite build.  This allows us to
// reuse our convention plugins in other modules without publishing them to
// an external repository.
includeBuild("build-logic")

// Core shared logic
include(":shared")

// Feature modules – each feature is split into an API and an implementation
include(":feature:home:api")
include(":feature:home:impl")

// Application entry points for each client platform
include(":androidApp")
include(":desktopApp")
