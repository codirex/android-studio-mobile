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

            // CI signs debug builds with our own key when the ASM_KEYSTORE_* variables are set, so
            // that every build installs over the previous one. Without them (a local build, a pull
            // request from a fork) the default debug key is used.
            val keystoreFile = providers.environmentVariable("ASM_KEYSTORE_FILE").orNull
            val keystorePassword = providers.environmentVariable("ASM_KEYSTORE_PASSWORD").orNull
            val keyAlias = providers.environmentVariable("ASM_KEY_ALIAS").orNull
            val keyPassword = providers.environmentVariable("ASM_KEY_PASSWORD").orNull ?: keystorePassword
            val ownKey = keystoreFile != null && keystorePassword != null && keyAlias != null

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

                // Views are bound through generated classes, never findViewById (ADR-0010).
                buildFeatures {
                    viewBinding = true
                }

                lint {
                    // targetSdk 28 is deliberate and the app is distributed outside Play (ADR-0002).
                    disable.addAll(listOf("ExpiredTargetSdkVersion", "OldTargetApi"))
                }

                if (ownKey) {
                    signingConfigs {
                        create("asm") {
                            storeFile = file(keystoreFile!!)
                            storePassword = keystorePassword
                            this.keyAlias = keyAlias
                            this.keyPassword = keyPassword
                        }
                    }
                    buildTypes {
                        getByName("debug") {
                            signingConfig = signingConfigs.getByName("asm")
                        }
                    }
                }
            }
        }
    }
}
