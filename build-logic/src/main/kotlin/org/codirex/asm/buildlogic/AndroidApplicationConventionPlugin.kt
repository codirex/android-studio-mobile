package org.codirex.asm.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** `asm.android.application`: the one application module. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                compileSdk = AsmBuild.COMPILE_SDK

                defaultConfig {
                    minSdk = AsmBuild.MIN_SDK
                    targetSdk = AsmBuild.TARGET_SDK
                }

                compileOptions {
                    sourceCompatibility = AsmBuild.JAVA_VERSION
                    targetCompatibility = AsmBuild.JAVA_VERSION
                }

                lint {
                    // targetSdk 28 is deliberate and the app is distributed outside Play (ADR-0002).
                    disable.addAll(listOf("ExpiredTargetSdkVersion", "OldTargetApi"))
                }
            }
        }
    }
}
