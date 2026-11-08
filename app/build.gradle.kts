plugins {
    id("asm.android.application")
    alias(libs.plugins.hilt)
}

android {
    namespace = "org.codirex.asm"

    defaultConfig {
        applicationId = "org.codirex.asm"
        versionCode = 1
        versionName = "0.0.1"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:concurrency"))
    implementation(project(":core:preferences"))
    implementation(project(":core:ui"))

    // The app wires data modules to the domain ports through Hilt; features never see them.
    implementation(project(":data:project"))
    implementation(project(":feature:project"))

    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)
}
