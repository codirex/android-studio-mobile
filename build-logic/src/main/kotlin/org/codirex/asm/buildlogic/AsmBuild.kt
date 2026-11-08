package org.codirex.asm.buildlogic

import org.gradle.api.JavaVersion

/** The platform numbers every module shares (ADR-0002). Change them here and nowhere else. */
internal object AsmBuild {
    /** 37 because clotho-android declares minCompileSdk 37 in its AAR metadata. */
    const val COMPILE_SDK = 37
    const val MIN_SDK = 24

    /** 28 so the app may execute files from its data directory (ADR-0002). */
    const val TARGET_SDK = 28

    val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_17
}
