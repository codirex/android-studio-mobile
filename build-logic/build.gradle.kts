plugins {
    `kotlin-dsl`
}

group = "org.codirex.asm.buildlogic"

dependencies {
    compileOnly(libs.android.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "asm.android.application"
            implementationClass = "org.codirex.asm.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "asm.android.library"
            implementationClass = "org.codirex.asm.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("jvmLibrary") {
            id = "asm.jvm.library"
            implementationClass = "org.codirex.asm.buildlogic.JvmLibraryConventionPlugin"
        }
    }
}
