package com.nikkiw.videofeedlab.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import com.diffplug.gradle.spotless.SpotlessExtension

/**
 * Spotless convention plugin that configures automated code style formatting.
 */
class SpotlessConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.diffplug.spotless")

        configure<SpotlessExtension> {
            this.kotlin {
                target("src/**/*.kt")
                ktlint("1.0.1")
                trimTrailingWhitespace()
                endWithNewline()
            }

            this.kotlinGradle {
                target("*.gradle.kts")
                ktlint("1.0.1")
                trimTrailingWhitespace()
                endWithNewline()
            }
        }
    }
}
