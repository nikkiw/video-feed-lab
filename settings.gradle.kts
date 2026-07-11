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

rootProject.name = "video-feed-lab"

// Include the custom build‑logic as a composite build.  This allows us to
// reuse our convention plugins in other modules without publishing them to
// an external repository.
includeBuild("build-logic")

// Core shared logic
include(":shared")
include(":feature:video-feed:api")
include(":feature:video-feed:impl")
include(":androidApp")
include(":desktopApp")
