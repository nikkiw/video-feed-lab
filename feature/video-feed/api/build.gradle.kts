plugins {
    id("com.nikkiw.videofeedlab.feature-api-convention")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
        }
    }
}
