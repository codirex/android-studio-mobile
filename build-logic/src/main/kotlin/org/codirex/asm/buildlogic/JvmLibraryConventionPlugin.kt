package org.codirex.asm.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/** `asm.jvm.library`: pure JVM modules. Every `domain:*` module uses this and nothing else. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("java-library")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = AsmBuild.JAVA_VERSION
                targetCompatibility = AsmBuild.JAVA_VERSION
            }

            tasks.withType<JavaCompile>().configureEach {
                options.encoding = "UTF-8"
            }
        }
    }
}
