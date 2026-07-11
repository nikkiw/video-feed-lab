package com.example.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin for feature implementation modules.
 */
class FeatureImplConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("com.example.detekt-convention")
        pluginManager.apply("com.example.spotless-convention")

        configure<KotlinMultiplatformExtension> {
            targets.getByName("android").apply {
                val androidTarget = this as com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
                val pathNamespace = project.path
                    .replace(":", ".")
                    .replace("-", "_")
                    .removePrefix(".")
                androidTarget.namespace = "com.example.$pathNamespace"
                androidTarget.compileSdk = 37
                androidTarget.minSdk = 23
                androidTarget.withHostTest {}
            }
            jvm("desktop")

            sourceSets {
                getByName("commonMain") {
                    dependencies {
                        val apiProjectPath = project.path.substringBeforeLast(":impl") + ":api"
                        implementation(project(apiProjectPath))
                        implementation("org.jetbrains.compose.runtime:runtime:${target.property("compose.version")}")
                    }
                }
                getByName("commonTest") {
                    dependencies {
                        implementation(kotlin("test"))
                    }
                }
            }
        }
    }
}
