package org.codirex.asm.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** `asm.android.library`: any Android library module (core:*, data:*, feature:*, terminal:*). */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                compileSdk = AsmBuild.COMPILE_SDK

                defaultConfig {
                    minSdk = AsmBuild.MIN_SDK
                }

                compileOptions {
                    sourceCompatibility = AsmBuild.JAVA_VERSION
                    targetCompatibility = AsmBuild.JAVA_VERSION
                }
            }
        }
    }
}
